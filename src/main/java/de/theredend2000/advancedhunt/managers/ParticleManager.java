package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.model.PlayerData;
import de.theredend2000.advancedhunt.model.TreasureCore;
import de.theredend2000.advancedhunt.util.ParticleUtils;
import de.theredend2000.advancedhunt.util.PlayerSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
    public void processTick(List<PlayerSnapshot> snapshots, Set<UUID> availableCollections, Set<UUID> singlePlayerFindCollections) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("particle-settings.enabled", false)) {
            return;
        }

        // For legacy versions (1.8), use simplified global particles since per-player particles aren't supported
        if (ParticleUtils.isLegacy()) {
            tickParticlesLegacy(snapshots, availableCollections);
            return;
        }

        List<ParticleAction> actions = new ArrayList<>();
        int chunkRadius = (int) Math.ceil(Math.sqrt(viewDistanceSq) / 16.0);

        for (PlayerSnapshot snapshot : snapshots) {
            Set<UUID> processedTreasures = new HashSet<>();
            Location playerLoc = snapshot.location();
            int playerChunkX = playerLoc.getBlockX() >> 4;
            int playerChunkZ = playerLoc.getBlockZ() >> 4;
            
            PlayerData playerData = playerManager.getPlayerData(snapshot.player().getUniqueId());

            for (int x = -chunkRadius; x <= chunkRadius; x++) {
                for (int z = -chunkRadius; z <= chunkRadius; z++) {
                    int chunkX = playerChunkX + x;
                    int chunkZ = playerChunkZ + z;

                    List<TreasureCore> treasures = treasureManager.getTreasureCoresInChunk(chunkX, chunkZ);
                    if (treasures == null || treasures.isEmpty()) continue;

                    for (TreasureCore treasure : treasures) {
                        if (processedTreasures.contains(treasure.getId())) continue;

                        Location treasureLoc = treasure.getLocation();
                        if (treasureLoc == null || treasureLoc.getWorld() == null || 
                            !treasureLoc.getWorld().getName().equals(playerLoc.getWorld().getName())) continue;

                        double dx = playerLoc.getX() - treasureLoc.getX();
                        double dy = playerLoc.getY() - treasureLoc.getY();
                        double dz = playerLoc.getZ() - treasureLoc.getZ();
                        double distSq = dx * dx + dy * dy + dz * dz;

                        if (distSq <= viewDistanceSq) {
                            ParticleConfig particleConfig = determineParticleConfig(treasure, playerData, 
                                availableCollections, singlePlayerFindCollections, globallyClaimedCache);
                            
                            if (particleConfig != null && particleConfig.enabled) {
                                actions.add(new ParticleAction(snapshot.player(), treasureLoc, particleConfig));
                            }
                            processedTreasures.add(treasure.getId());
                        }
                    }
                }
            }
        }

        // Schedule sync task to spawn particles
        if (!actions.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (ParticleAction action : actions) {
                    Location loc = action.location;
                    // Re-create location with offset for spawning
                    Location particleLoc = new Location(loc.getWorld(), 
                        loc.getBlockX() + 0.5, loc.getBlockY() + 0.5, loc.getBlockZ() + 0.5);
                    
                    ParticleUtils.spawnParticleForPlayer(action.player, particleLoc, 
                        action.config.particleName, action.config.count, 
                        action.config.offX, action.config.offY, action.config.offZ, action.config.speed);
                }
            });
        }
    }
    
    /**
     * Simplified particle spawning for legacy versions (1.8.x).
     * Shows basic particles for all treasures globally since per-player particles aren't supported.
     */
    private void tickParticlesLegacy(List<PlayerSnapshot> snapshots, Set<UUID> availableCollections) {
        Set<UUID> processedTreasures = new HashSet<>();
        List<GlobalParticleAction> actions = new ArrayList<>();
        int chunkRadius = (int) Math.ceil(Math.sqrt(viewDistanceSq) / 16.0);
        
        if (notFoundConfig == null || !notFoundConfig.enabled) return;

        for (PlayerSnapshot snapshot : snapshots) {
            Location playerLoc = snapshot.location();
            int playerChunkX = playerLoc.getBlockX() >> 4;
            int playerChunkZ = playerLoc.getBlockZ() >> 4;

            for (int x = -chunkRadius; x <= chunkRadius; x++) {
                for (int z = -chunkRadius; z <= chunkRadius; z++) {
                    int chunkX = playerChunkX + x;
                    int chunkZ = playerChunkZ + z;

                    List<TreasureCore> treasures = treasureManager.getTreasureCoresInChunk(chunkX, chunkZ);
                    if (treasures == null || treasures.isEmpty()) continue;

                    for (TreasureCore treasure : treasures) {
                        if (processedTreasures.contains(treasure.getId())) continue;
                        
                        if (!availableCollections.contains(treasure.getCollectionId())) continue;

                        Location treasureLoc = treasure.getLocation();
                        if (treasureLoc == null || treasureLoc.getWorld() == null || 
                            !treasureLoc.getWorld().getName().equals(playerLoc.getWorld().getName())) continue;

                        double dx = playerLoc.getX() - treasureLoc.getX();
                        double dy = playerLoc.getY() - treasureLoc.getY();
                        double dz = playerLoc.getZ() - treasureLoc.getZ();
                        double distSq = dx * dx + dy * dy + dz * dz;

                        if (distSq <= viewDistanceSq) {
                            actions.add(new GlobalParticleAction(treasureLoc, notFoundConfig));
                            processedTreasures.add(treasure.getId());
                        }
                    }
                }
            }
        }

        if (!actions.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (GlobalParticleAction action : actions) {
                    Location loc = action.location;
                    Location particleLoc = new Location(loc.getWorld(), 
                        loc.getBlockX() + 0.5, loc.getBlockY() + 0.5, loc.getBlockZ() + 0.5);

                    ParticleUtils.spawnParticle(particleLoc, action.config.particleName, 
                        action.config.count, action.config.offX, action.config.offY, 
                        action.config.offZ, action.config.speed);
                }
            });
        }
    }
    
    private void tickParticles() {
        // Deprecated - logic moved to processTick
    }
    
    /**
     * Determines which particle configuration to use based on treasure state.
     * Returns null if particles should not be shown (collection unavailable).
     */
    private ParticleConfig determineParticleConfig(TreasureCore treasure, PlayerData playerData,
                                                  Set<UUID> availableCollections,
                                                  Set<UUID> singlePlayerFindCollections,
                                                  Map<UUID, Boolean> globallyClaimedCache) {
        // Check availability (O(1))
        if (!availableCollections.contains(treasure.getCollectionId())) {
            return null;
        }
        
        // Check found status (O(1))
        if (playerData.hasFound(treasure.getId())) {
            return foundByPlayerConfig;
        }
        
        // Check single player find (O(1))
        if (singlePlayerFindCollections.contains(treasure.getCollectionId())) {
            if (globallyClaimedCache.getOrDefault(treasure.getId(), Boolean.FALSE)) {
                return foundByOthersConfig;
            }
        }
        
        return notFoundConfig;
    }

    private static class ParticleAction {
        final Player player;
        final Location location;
        final ParticleConfig config;

        ParticleAction(Player player, Location location, ParticleConfig config) {
            this.player = player;
            this.location = location;
            this.config = config;
        }
    }

    private static class GlobalParticleAction {
        final Location location;
        final ParticleConfig config;

        GlobalParticleAction(Location location, ParticleConfig config) {
            this.location = location;
            this.config = config;
        }
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
