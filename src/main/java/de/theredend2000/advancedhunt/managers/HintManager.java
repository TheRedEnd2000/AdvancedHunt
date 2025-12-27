package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.model.PlayerData;
import de.theredend2000.advancedhunt.model.TreasureCore;
import de.theredend2000.advancedhunt.util.ParticleUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages hint delivery for players seeking treasures.
 * Provides coordinate hints and optional visual effects to guide players.
 */
public class HintManager {

    private final Main plugin;
    private final TreasureManager treasureManager;
    private final CollectionManager collectionManager;
    private final PlayerManager playerManager;
    private final MessageManager messageManager;
    private final ParticleManager particleManager;

    // Cooldown tracking: playerId -> last hint timestamp
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    
    // Active visual hints: playerId -> task
    private final Map<UUID, BukkitTask> activeVisualHints = new ConcurrentHashMap<>();

    // Configuration cache
    private boolean visualHintEnabled;
    private VisualHintType visualHintType;
    private int visualHintDuration;
    private String visualHintParticle;
    private boolean applyFailCooldown;
    private int cooldownSeconds;
    private int proximityRange;
    private CoordinateRevealType revealType;

    public enum VisualHintType {
        NONE,
        TRAIL,      // Particle line from player to treasure
        COMPASS,    // Set player compass to treasure
        BEACON      // Vertical particle beam at treasure
    }

    public enum CoordinateRevealType {
        X, Y, Z, RANDOM
    }

    public HintManager(Main plugin, TreasureManager treasureManager, CollectionManager collectionManager,
                      PlayerManager playerManager, MessageManager messageManager, ParticleManager particleManager) {
        this.plugin = plugin;
        this.treasureManager = treasureManager;
        this.collectionManager = collectionManager;
        this.playerManager = playerManager;
        this.messageManager = messageManager;
        this.particleManager = particleManager;
        reloadConfig();
    }

    /**
     * Reload configuration from config.yml
     */
    public void reloadConfig() {
        this.visualHintEnabled = plugin.getConfig().getBoolean("minigames.hint.visual-hint.enabled", false);
        String typeStr = plugin.getConfig().getString("minigames.hint.visual-hint.type", "NONE");
        try {
            this.visualHintType = VisualHintType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.visualHintType = VisualHintType.NONE;
            plugin.getLogger().warning("Invalid visual hint type: " + typeStr + ", defaulting to NONE");
        }
        this.visualHintDuration = plugin.getConfig().getInt("minigames.hint.visual-hint.duration-seconds", 15);
        this.visualHintParticle = plugin.getConfig().getString("minigames.hint.visual-hint.particle", "END_ROD");
        this.applyFailCooldown = plugin.getConfig().getBoolean("minigames.hint.apply-cooldown-on-fail", true);
        this.cooldownSeconds = plugin.getConfig().getInt("minigames.hint.cooldown-seconds", 300);
        this.proximityRange = plugin.getConfig().getInt("minigames.hint.proximity-range", 50);
        
        String revealStr = plugin.getConfig().getString("minigames.hint.reveal-coordinate", "RANDOM");
        try {
            this.revealType = CoordinateRevealType.valueOf(revealStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.revealType = CoordinateRevealType.RANDOM;
            plugin.getLogger().warning("Invalid reveal coordinate type: " + revealStr + ", defaulting to RANDOM");
        }
    }

    /**
     * Check if player is on cooldown
     */
    public boolean isOnCooldown(UUID playerId) {
        Long lastHint = cooldowns.get(playerId);
        if (lastHint == null) return false;
        
        long elapsed = (System.currentTimeMillis() - lastHint) / 1000;
        return elapsed < cooldownSeconds;
    }

    /**
     * Get remaining cooldown time in seconds
     */
    public long getRemainingCooldown(UUID playerId) {
        Long lastHint = cooldowns.get(playerId);
        if (lastHint == null) return 0;
        
        long elapsed = (System.currentTimeMillis() - lastHint) / 1000;
        return Math.max(0, cooldownSeconds - elapsed);
    }

    /**
     * Apply cooldown to player
     */
    public void applyCooldown(UUID playerId) {
        cooldowns.put(playerId, System.currentTimeMillis());
    }

    /**
     * Apply cooldown on minigame failure (if configured)
     */
    public void applyFailureCooldown(UUID playerId) {
        if (applyFailCooldown) {
            applyCooldown(playerId);
        }
    }

    /**
     * Find a random unfound treasure within proximity range
     */
    public Optional<TreasureCore> findRandomUnfoundTreasure(Player player) {
        PlayerData playerData = playerManager.getPlayerData(player.getUniqueId());
        Location playerLoc = player.getLocation();
        
        // Get available collections (ACT rules)
        Set<UUID> availableCollections = collectionManager.getAllCollections()
                .stream()
                .filter(collectionManager::isCollectionAvailable)
                .map(Collection::getId)
                .collect(java.util.stream.Collectors.toSet());

        if (availableCollections.isEmpty()) {
            return Optional.empty();
        }

        // Find unfound treasures in range using chunk-based search
        List<TreasureCore> candidateTreasures = new ArrayList<>();
        int chunkX = playerLoc.getBlockX() >> 4;
        int chunkZ = playerLoc.getBlockZ() >> 4;
        int chunkRadius = (int) Math.ceil((double) proximityRange / 16.0);
        int rangeSq = proximityRange * proximityRange;

        for (int x = chunkX - chunkRadius; x <= chunkX + chunkRadius; x++) {
            for (int z = chunkZ - chunkRadius; z <= chunkZ + chunkRadius; z++) {
                List<TreasureCore> treasures = treasureManager.getTreasureCoresInChunk(x, z);
                if (treasures == null || treasures.isEmpty()) continue;

                for (TreasureCore treasure : treasures) {
                    // Check world match
                    if (!treasure.getLocation().getWorld().equals(playerLoc.getWorld())) continue;
                    
                    // Check distance
                    if (treasure.getLocation().distanceSquared(playerLoc) > rangeSq) continue;
                    
                    // Check if player has found it
                    if (playerData.hasFound(treasure.getId())) continue;
                    
                    // Check if collection is available
                    if (!availableCollections.contains(treasure.getCollectionId())) continue;
                    
                    candidateTreasures.add(treasure);
                }
            }
        }

        if (candidateTreasures.isEmpty()) {
            return Optional.empty();
        }

        // Return random treasure
        return Optional.of(candidateTreasures.get(new Random().nextInt(candidateTreasures.size())));
    }

    /**
     * Deliver a hint to the player with text and optional visual effects
     */
    public void deliverHint(Player player, TreasureCore treasure) {
        Location treasureLoc = treasure.getLocation();
        Location playerLoc = player.getLocation();
        
        // Calculate distance
        double distance = playerLoc.distance(treasureLoc);
        
        // Calculate direction
        String direction = getCardinalDirection(playerLoc, treasureLoc);
        
        // Determine which coordinate to reveal
        CoordinateRevealType actualReveal = revealType;
        if (revealType == CoordinateRevealType.RANDOM) {
            CoordinateRevealType[] types = {CoordinateRevealType.X, CoordinateRevealType.Y, CoordinateRevealType.Z};
            actualReveal = types[new Random().nextInt(types.length)];
        }
        
        // Send coordinate hint
        String messageKey;
        String coordinateValue;
        switch (actualReveal) {
            case X:
                messageKey = "hint.coordinate_x";
                coordinateValue = String.valueOf(treasureLoc.getBlockX());
                break;
            case Y:
                messageKey = "hint.coordinate_y";
                coordinateValue = String.valueOf(treasureLoc.getBlockY());
                break;
            case Z:
                messageKey = "hint.coordinate_z";
                coordinateValue = String.valueOf(treasureLoc.getBlockZ());
                break;
            default:
                return;
        }
        
        player.sendMessage(messageManager.getMessage(messageKey,
                "%value%", coordinateValue,
                "%distance%", String.format("%.1f", distance),
                "%direction%", direction));
        
        // Apply visual hint if enabled
        if (visualHintEnabled && visualHintType != VisualHintType.NONE) {
            applyVisualHint(player, treasure);
            player.sendMessage(messageManager.getMessage("hint.visual_active"));
        }
        
        // Apply cooldown
        applyCooldown(player.getUniqueId());
    }

    /**
     * Calculate cardinal direction from one location to another
     */
    private String getCardinalDirection(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        
        // Calculate angle in degrees
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        
        // Normalize to 0-360
        if (angle < 0) {
            angle += 360;
        }
        
        // Convert to cardinal directions (8 directions)
        if (angle >= 337.5 || angle < 22.5) return "East";
        else if (angle >= 22.5 && angle < 67.5) return "Southeast";
        else if (angle >= 67.5 && angle < 112.5) return "South";
        else if (angle >= 112.5 && angle < 157.5) return "Southwest";
        else if (angle >= 157.5 && angle < 202.5) return "West";
        else if (angle >= 202.5 && angle < 247.5) return "Northwest";
        else if (angle >= 247.5 && angle < 292.5) return "North";
        else return "Northeast";
    }

    /**
     * Apply visual hint effect based on configuration
     */
    private void applyVisualHint(Player player, TreasureCore treasure) {
        // Cancel any existing visual hint for this player
        cancelVisualHint(player.getUniqueId());
        
        BukkitTask task;
        
        switch (visualHintType) {
            case TRAIL:
                task = startParticleTrail(player, treasure);
                break;
            case COMPASS:
                task = startCompassPointing(player, treasure);
                break;
            case BEACON:
                task = startBeaconEffect(player, treasure);
                break;
            default:
                return;
        }
        
        activeVisualHints.put(player.getUniqueId(), task);
    }

    /**
     * Start a particle trail from player to treasure
     */
    private BukkitTask startParticleTrail(Player player, TreasureCore treasure) {
        return new BukkitRunnable() {
            int ticksRemaining = visualHintDuration * 20;
            
            @Override
            public void run() {
                if (!player.isOnline() || ticksRemaining <= 0) {
                    cancel();
                    activeVisualHints.remove(player.getUniqueId());
                    return;
                }
                
                Location playerLoc = player.getLocation().add(0, 1, 0); // Eye level
                Location treasureLoc = treasure.getLocation().clone().add(0.5, 0.5, 0.5); // Center of block
                
                // Draw particle line
                Vector direction = treasureLoc.toVector().subtract(playerLoc.toVector());
                double distance = direction.length();
                direction.normalize();
                
                // Spawn particles along the line (every 0.5 blocks)
                for (double i = 0; i < distance; i += 0.5) {
                    Location particleLoc = playerLoc.clone().add(direction.clone().multiply(i));
                    ParticleUtils.spawnParticleForPlayer(player, particleLoc, visualHintParticle, 1, 0, 0, 0, 0);
                }
                
                ticksRemaining--;
            }
        }.runTaskTimer(plugin, 0L, 10L); // Update every 0.5 seconds
    }

    /**
     * Point player's compass to treasure
     */
    private BukkitTask startCompassPointing(Player player, TreasureCore treasure) {
        Location treasureLoc = treasure.getLocation();
        player.setCompassTarget(treasureLoc);
        
        // Reset compass after duration
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.setCompassTarget(player.getWorld().getSpawnLocation());
                }
                activeVisualHints.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, visualHintDuration * 20L);
    }

    /**
     * Create a vertical beacon beam at treasure location
     */
    private BukkitTask startBeaconEffect(Player player, TreasureCore treasure) {
        return new BukkitRunnable() {
            int ticksRemaining = visualHintDuration * 20;
            
            @Override
            public void run() {
                if (!player.isOnline() || ticksRemaining <= 0) {
                    cancel();
                    activeVisualHints.remove(player.getUniqueId());
                    return;
                }
                
                Location baseLoc = treasure.getLocation().clone().add(0.5, 0, 0.5);
                
                // Spawn vertical beam of particles (up to 20 blocks high or to world height)
                for (int y = 0; y < 20 && baseLoc.getBlockY() + y < baseLoc.getWorld().getMaxHeight(); y++) {
                    Location particleLoc = baseLoc.clone().add(0, y, 0);
                    ParticleUtils.spawnParticleForPlayer(player, particleLoc, visualHintParticle, 2, 0.1, 0.1, 0.1, 0);
                }
                
                ticksRemaining -= 10;
            }
        }.runTaskTimer(plugin, 0L, 10L); // Update every 0.5 seconds
    }

    /**
     * Cancel active visual hint for a player
     */
    public void cancelVisualHint(UUID playerId) {
        BukkitTask task = activeVisualHints.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Cancel all active visual hints (called on plugin disable)
     */
    public void cancelAllVisualHints() {
        activeVisualHints.values().forEach(BukkitTask::cancel);
        activeVisualHints.clear();
    }
}
