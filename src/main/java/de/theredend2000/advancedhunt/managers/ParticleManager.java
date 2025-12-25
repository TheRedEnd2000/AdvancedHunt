package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.model.PlayerData;
import de.theredend2000.advancedhunt.model.TreasureCore;
import de.theredend2000.advancedhunt.util.ParticleUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the spawning of particles around treasure locations.
 * Runs a scheduled task that displays configurable particles to nearby players.
 */
public class ParticleManager {

    private final Main plugin;
    private final TreasureManager treasureManager;
    private final PlayerManager playerManager;
    private final CollectionManager collectionManager;
    private BukkitTask task;
    
    // Cache of globally claimed treasures in single-player-find collections
    private final Map<UUID, Boolean> globallyClaimedCache = new ConcurrentHashMap<>();

    // Cached configuration values for three particle states
    private ParticleConfig notFoundConfig;
    private ParticleConfig foundByPlayerConfig;
    private ParticleConfig foundByOthersConfig;
    private int viewDistanceSq;
    
    // Inner class to hold particle configuration
    private static class ParticleConfig {
        boolean enabled;
        String particleName;
        int count;
        double speed;
        double offX;
        double offY;
        double offZ;
        
        ParticleConfig(boolean enabled, String particleName, int count, double speed,
                      double offX, double offY, double offZ) {
            this.enabled = enabled;
            this.particleName = particleName;
            this.count = count;
            this.speed = speed;
            this.offX = offX;
            this.offY = offY;
            this.offZ = offZ;
        }
    }

    // Reusable location object to avoid allocations
    private final Location reusableLocation = new Location(null, 0, 0, 0);

    public ParticleManager(Main plugin, TreasureManager treasureManager, 
                          PlayerManager playerManager, CollectionManager collectionManager) {
        this.plugin = plugin;
        this.treasureManager = treasureManager;
        this.playerManager = playerManager;
        this.collectionManager = collectionManager;
    }
    
    /**
     * Marks a treasure as globally claimed (for single-player-find collections).
     * Called when any player finds a treasure in a single-find collection.
     */
    public void markTreasureAsGloballyClaimed(UUID treasureId) {
        globallyClaimedCache.put(treasureId, Boolean.TRUE);
    }
    
    /**
     * Checks if a treasure is globally claimed.
     */
    public boolean isTreasureGloballyClaimed(UUID treasureId) {
        return globallyClaimedCache.getOrDefault(treasureId, Boolean.FALSE);
    }
    
    /**
     * Clears the global cache for a specific collection.
     * Called when a collection is reset via cron.
     */
    public void clearGlobalCache(UUID collectionId) {
        // Use lightweight TreasureCore - we only need IDs
        List<TreasureCore> treasures = treasureManager.getTreasureCoresInCollection(collectionId);
        for (TreasureCore treasure : treasures) {
            globallyClaimedCache.remove(treasure.getId());
        }
    }
    
    /**
     * Clears the entire global cache.
     * Called during reload.
     */
    public void clearAllGlobalCache() {
        globallyClaimedCache.clear();
    }
    
    /**
     * Preloads globally claimed treasures from the database.
     * Called during startup to populate the cache for single-player-find collections.
     */
    private void preloadGloballyClaimedTreasures() {
        // Get all collections
        List<Collection> collections = collectionManager.getAllCollections();
        
        for (Collection collection : collections) {
            // Only check single-player-find collections
            if (collection.isSinglePlayerFind()) {
                // Use lightweight TreasureCore - we only need IDs
                List<TreasureCore> treasures = treasureManager.getTreasureCoresInCollection(collection.getId());
                
                for (TreasureCore treasure : treasures) {
                    // Async check if anyone has found this treasure
                    plugin.getDataRepository().getPlayersWhoFound(treasure.getId()).thenAccept(claimers -> {
                        if (!claimers.isEmpty()) {
                            globallyClaimedCache.put(treasure.getId(), Boolean.TRUE);
                        }
                    });
                }
            }
        }
    }

    /**
     * Starts the particle display task.
     * Reads configuration values and begins the scheduled task.
     */
    public void start() {
        stop(); // Ensure any existing task is stopped

        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("particle-settings.enabled", false)) {
            return;
        }

        // Load view distance
        int viewDistance = config.getInt("particle-settings.view-distance", 32);
        viewDistanceSq = viewDistance * viewDistance;
        
        // Load not-found particle configuration
        notFoundConfig = loadParticleConfig(config, "particle-settings.not-found", 
            true, "VILLAGER_HAPPY", 2, 0.03, 0.5, 0.5, 0.5);
        
        // Load found-by-player particle configuration
        foundByPlayerConfig = loadParticleConfig(config, "particle-settings.found-by-player",
            true, "END_ROD", 1, 0.01, 0.2, 0.2, 0.2);
        
        // Load found-by-others particle configuration
        foundByOthersConfig = loadParticleConfig(config, "particle-settings.found-by-others",
            false, "SMOKE_NORMAL", 1, 0.02, 0.3, 0.3, 0.3);
        
        // Preload globally claimed treasures cache
        preloadGloballyClaimedTreasures();

        // Use sync task - spawnParticle should be called from main thread for safety
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickParticles, 20L, 20L);
    }
    
    /**
     * Helper method to load particle configuration from config file.
     */
    private ParticleConfig loadParticleConfig(FileConfiguration config, String path,
                                             boolean defaultEnabled, String defaultParticle,
                                             int defaultCount, double defaultSpeed,
                                             double defaultOffX, double defaultOffY, double defaultOffZ) {
        return new ParticleConfig(
            config.getBoolean(path + ".enabled", defaultEnabled),
            config.getString(path + ".particle", defaultParticle),
            config.getInt(path + ".count", defaultCount),
            config.getDouble(path + ".speed", defaultSpeed),
            config.getDouble(path + ".offset.x", defaultOffX),
            config.getDouble(path + ".offset.y", defaultOffY),
            config.getDouble(path + ".offset.z", defaultOffZ)
        );
    }

    /**
     * Main particle tick method. Iterates through players and checks nearby chunks
     * for treasures to spawn particles. This is spatially optimized for large treasure counts.
     */
    private void tickParticles() {
        // For legacy versions (1.8), use simplified global particles since per-player particles aren't supported
        if (ParticleUtils.isLegacy()) {
            tickParticlesLegacy();
            return;
        }
        
        // Modern version: per-player particle states
        // Calculate chunk radius based on view distance
        // viewDistance is in blocks, so divide by 16 to get chunks
        int chunkRadius = (int) Math.ceil(Math.sqrt(viewDistanceSq) / 16.0);

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Track processed treasures PER PLAYER to avoid duplicate particles for the same treasure
            Set<UUID> processedTreasures = new HashSet<>();
            
            Location playerLoc = player.getLocation();
            World world = player.getWorld();
            int playerChunkX = playerLoc.getBlockX() >> 4;
            int playerChunkZ = playerLoc.getBlockZ() >> 4;
            
            // Get player data once per player for efficiency
            PlayerData playerData = playerManager.getPlayerData(player.getUniqueId());

            // Check chunks around the player
            for (int x = -chunkRadius; x <= chunkRadius; x++) {
                for (int z = -chunkRadius; z <= chunkRadius; z++) {
                    int chunkX = playerChunkX + x;
                    int chunkZ = playerChunkZ + z;

                    // Skip unloaded chunks
                    if (!world.isChunkLoaded(chunkX, chunkZ)) continue;

                    // Use lightweight TreasureCore - no heavy data needed for particles
                    List<TreasureCore> treasures = treasureManager.getTreasureCoresInChunk(chunkX, chunkZ);
                    if (treasures.isEmpty()) continue;

                    for (TreasureCore treasure : treasures) {
                        // Skip if already processed for this player in this tick
                        if (processedTreasures.contains(treasure.getId())) continue;

                        Location treasureLoc = treasure.getLocation();
                        // Check world (treasureChunkMap mixes worlds)
                        if (treasureLoc == null || treasureLoc.getWorld() == null || !treasureLoc.getWorld().getName().equals(world.getName())) continue;

                        // Check exact distance
                        double dx = playerLoc.getX() - treasureLoc.getX();
                        double dy = playerLoc.getY() - treasureLoc.getY();
                        double dz = playerLoc.getZ() - treasureLoc.getZ();
                        double distSq = dx * dx + dy * dy + dz * dz;

                        if (distSq <= viewDistanceSq) {
                            // Determine particle state and spawn appropriate particles
                            ParticleConfig config = determineParticleConfig(treasure, playerData);
                            
                            if (config != null && config.enabled) {
                                // Spawn particle visible only to this specific player
                                Location particleLoc = new Location(
                                    world,
                                    treasureLoc.getBlockX() + 0.5,
                                    treasureLoc.getBlockY() + 0.5,
                                    treasureLoc.getBlockZ() + 0.5
                                );

                                ParticleUtils.spawnParticleForPlayer(player, particleLoc, config.particleName, 
                                    config.count, config.offX, config.offY, config.offZ, config.speed);
                            }
                            
                            // Mark as processed for this player
                            processedTreasures.add(treasure.getId());
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Simplified particle spawning for legacy versions (1.8.x).
     * Shows basic particles for all treasures globally since per-player particles aren't supported.
     */
    private void tickParticlesLegacy() {
        // Track processed treasures globally to avoid duplicates
        Set<UUID> processedTreasures = new HashSet<>();
        
        // Calculate chunk radius based on view distance
        int chunkRadius = (int) Math.ceil(Math.sqrt(viewDistanceSq) / 16.0);
        
        // Only show particles if not-found config is enabled
        if (notFoundConfig == null || !notFoundConfig.enabled) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            Location playerLoc = player.getLocation();
            World world = player.getWorld();
            int playerChunkX = playerLoc.getBlockX() >> 4;
            int playerChunkZ = playerLoc.getBlockZ() >> 4;

            // Check chunks around the player
            for (int x = -chunkRadius; x <= chunkRadius; x++) {
                for (int z = -chunkRadius; z <= chunkRadius; z++) {
                    int chunkX = playerChunkX + x;
                    int chunkZ = playerChunkZ + z;

                    // Skip unloaded chunks
                    if (!world.isChunkLoaded(chunkX, chunkZ)) continue;

                    // Use lightweight TreasureCore - no heavy data needed for particles
                    List<TreasureCore> treasures = treasureManager.getTreasureCoresInChunk(chunkX, chunkZ);
                    if (treasures.isEmpty()) continue;

                    for (TreasureCore treasure : treasures) {
                        // Skip if already processed
                        if (processedTreasures.contains(treasure.getId())) continue;
                        
                        // Check if collection is available - skip unavailable collections
                        boolean available = collectionManager.getCollectionById(treasure.getCollectionId())
                            .map(collectionManager::isCollectionAvailable)
                            .orElse(false);
                        if (!available) continue;

                        Location treasureLoc = treasure.getLocation();
                        if (treasureLoc == null || treasureLoc.getWorld() == null || 
                            !treasureLoc.getWorld().getName().equals(world.getName())) continue;

                        // Check exact distance
                        double dx = playerLoc.getX() - treasureLoc.getX();
                        double dy = playerLoc.getY() - treasureLoc.getY();
                        double dz = playerLoc.getZ() - treasureLoc.getZ();
                        double distSq = dx * dx + dy * dy + dz * dz;

                        if (distSq <= viewDistanceSq) {
                            // Spawn basic particle for all treasures (visible to all players)
                            Location particleLoc = new Location(
                                world,
                                treasureLoc.getBlockX() + 0.5,
                                treasureLoc.getBlockY() + 0.5,
                                treasureLoc.getBlockZ() + 0.5
                            );

                            ParticleUtils.spawnParticle(particleLoc, notFoundConfig.particleName, 
                                notFoundConfig.count, notFoundConfig.offX, notFoundConfig.offY, 
                                notFoundConfig.offZ, notFoundConfig.speed);
                            
                            processedTreasures.add(treasure.getId());
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Determines which particle configuration to use based on treasure state.
     * Returns null if particles should not be shown (collection unavailable).
     * @param treasure The treasure core to check (lightweight)
     * @param playerData The current player's data
     * @return The particle configuration to use, or null if no particles should be shown
     */
    private ParticleConfig determineParticleConfig(TreasureCore treasure, PlayerData playerData) {
        // Get collection once and check availability
        Optional<Collection> collectionOpt = collectionManager.getCollectionById(treasure.getCollectionId());
        if (collectionOpt.isEmpty() || !collectionManager.isCollectionAvailable(collectionOpt.get())) {
            return null; // No particles for unavailable/disabled collections
        }
        
        Collection collection = collectionOpt.get();
        
        // Check if player has found this treasure
        if (playerData.hasFound(treasure.getId())) {
            return foundByPlayerConfig;
        }
        
        // Only check global cache for single-player-find collections
        if (collection.isSinglePlayerFind()) {
            // Check if treasure is globally claimed by another player
            if (isTreasureGloballyClaimed(treasure.getId())) {
                return foundByOthersConfig;
            }
        }
        
        return notFoundConfig; // Regular collection, treasure not found
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }
    
    public void reload() {
        stop();
        ParticleUtils.resetWarnings();
        clearAllGlobalCache();
        start();
    }
}
