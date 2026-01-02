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
import java.util.concurrent.ThreadLocalRandom;

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

    private BukkitTask cleanupTask;

    // Configuration cache
    private boolean visualHintEnabled;
    private VisualHintType visualHintType;
    private int visualHintDuration;
    private String visualHintParticle;
    private boolean applyFailCooldown;
    private boolean applyFailCooldownOnOP;
    private int cooldownSeconds;
    private int proximityRange;
    private CoordinateRevealType revealType;
    private int visualHintMaxDistance;
    private int visualHintOffsetRange;
    
    // Constants for visual effects
    private static final long VISUAL_UPDATE_INTERVAL = 20L; // 1 second (less spammy / less in-your-face)
    private static final double PARTICLE_SPACING = 1.25;
    private static final int BEACON_HEIGHT = 20;
    private static final double EYE_LEVEL_OFFSET = 1.0;

    private static final double TRAIL_START_DISTANCE = 1.5; // start a bit in front of the player
    private static final double TRAIL_Y_OFFSET = -0.35;     // slightly below eye-level
    private static final double TRAIL_OFFSET = 0.03;        // subtle spread so it's not a laser line
    private static final double BEACON_OFFSET = 0.06;       // subtle spread, avoids a harsh column
    private static final int BEACON_Y_STEP = 2;             // reduces particle density
    
    // Cardinal direction message keys (index matches getCardinalDirection mapping)
    private static final String[] DIRECTION_KEYS = {
        "command.direction.east", "command.direction.southeast", "command.direction.south", "command.direction.southwest",
        "command.direction.west", "command.direction.northwest", "command.direction.north", "command.direction.northeast"
    };

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
        
        // Periodic cleanup of expired cooldowns (every 5 minutes)
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredCooldowns();
            }
        }.runTaskTimerAsynchronously(plugin, 6000L, 6000L);
    }

    public void stop() {
        cancelAllVisualHints();
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
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
        this.visualHintDuration = Math.max(1, plugin.getConfig().getInt("minigames.hint.visual-hint.duration-seconds", 15));
        this.visualHintParticle = plugin.getConfig().getString("minigames.hint.visual-hint.particle", "END_ROD");
        this.visualHintMaxDistance = Math.max(5, plugin.getConfig().getInt("minigames.hint.visual-hint.max-distance", 15));
        this.visualHintOffsetRange = Math.max(3, plugin.getConfig().getInt("minigames.hint.visual-hint.offset-range", 10));
        this.applyFailCooldown = plugin.getConfig().getBoolean("minigames.hint.apply-cooldown-on-fail", true);
        this.applyFailCooldownOnOP = plugin.getConfig().getBoolean("minigames.hint.apply-cooldown-to-operators", false);
        this.cooldownSeconds = Math.max(0, plugin.getConfig().getInt("minigames.hint.cooldown-seconds", 300));
        this.proximityRange = Math.max(1, plugin.getConfig().getInt("minigames.hint.proximity-range", 50));
        
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
    public void applyFailureCooldown(Player player) {
        if (!applyFailCooldown) return;

        // OP-Check
        if (player.isOp() && !applyFailCooldownOnOP) return;

        applyCooldown(player.getUniqueId());
    }
    
    /**
     * Clean up expired cooldowns to prevent memory leak
     */
    private void cleanupExpiredCooldowns() {
        long now = System.currentTimeMillis();
        long expirationThreshold = cooldownSeconds * 1000L;
        cooldowns.entrySet().removeIf(entry -> (now - entry.getValue()) > expirationThreshold);
    }

    /**
     * Find a random unfound treasure within proximity range
     */
    public Optional<TreasureCore> findRandomUnfoundTreasure(Player player) {
        PlayerData playerData = playerManager.getPlayerData(player.getUniqueId());
        Location playerLoc = player.getLocation();
        
        // Get available collections (ACT rules)
        List<Collection> allCollections = collectionManager.getAllCollections();
        Set<UUID> availableCollections = new HashSet<>();
        for (Collection collection : allCollections) {
            if (collectionManager.isCollectionAvailable(collection)) {
                availableCollections.add(collection.getId());
            }
        }

        if (availableCollections.isEmpty()) {
            return Optional.empty();
        }

        // Find unfound treasures in range using chunk-based search
        int chunkX = playerLoc.getBlockX() >> 4;
        int chunkZ = playerLoc.getBlockZ() >> 4;
        int chunkRadius = (int) Math.ceil((double) proximityRange / 16.0);
        int rangeSq = proximityRange * proximityRange;
        
        List<TreasureCore> candidateTreasures = null;

        for (int x = chunkX - chunkRadius; x <= chunkX + chunkRadius; x++) {
            for (int z = chunkZ - chunkRadius; z <= chunkZ + chunkRadius; z++) {
                List<TreasureCore> treasures = treasureManager.getTreasureCoresInChunk(x, z);
                if (treasures == null || treasures.isEmpty()) continue;

                for (TreasureCore treasure : treasures) {
                    // Check world match
                    if (!treasure.getLocation().getWorld().equals(playerLoc.getWorld())) continue;
                    
                    // Check distance (using squared distance for performance)
                    if (treasure.getLocation().distanceSquared(playerLoc) > rangeSq) continue;
                    
                    // Check if player has found it
                    if (playerData.hasFound(treasure.getId())) continue;
                    
                    // Check if collection is available
                    if (!availableCollections.contains(treasure.getCollectionId())) continue;
                    
                    // Lazy initialize list only when needed
                    if (candidateTreasures == null) {
                        candidateTreasures = new ArrayList<>();
                    }
                    candidateTreasures.add(treasure);
                }
            }
        }

        if (candidateTreasures == null || candidateTreasures.isEmpty()) {
            return Optional.empty();
        }

        // Return random treasure using ThreadLocalRandom (faster, no allocation)
        int randomIndex = ThreadLocalRandom.current().nextInt(candidateTreasures.size());
        return Optional.of(candidateTreasures.get(randomIndex));
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
        CoordinateRevealType actualReveal = selectCoordinateToReveal();
        
        // Send coordinate hint
        String messageKey;
        String coordinateValue;
        switch (actualReveal) {
            case X:
                messageKey = "hint.coordinate_x";
                coordinateValue = String.valueOf(treasureLoc.getBlockX());
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
     * Select which coordinate type to reveal based on configuration
     */
    private CoordinateRevealType selectCoordinateToReveal() {
        if (revealType != CoordinateRevealType.RANDOM) {
            return revealType;
        }
        
        // Only reveal X or Z
        return ThreadLocalRandom.current().nextBoolean() ? CoordinateRevealType.X : CoordinateRevealType.Z;
    }

    /**
     * Calculate cardinal direction from one location to another.
     * Uses lookup table for performance.
     */
    private String getCardinalDirection(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        
        // Calculate angle in degrees and normalize to 0-360
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        if (angle < 0) {
            angle += 360;
        }
        
        // Map to 8 cardinal directions (45 degrees each)
        int directionIndex = (int) Math.round(angle / 45.0) % 8;
        return messageManager.getMessage(DIRECTION_KEYS[directionIndex], false);
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
     * Start a particle trail from player toward treasure (limited distance for hint, not full path).
     * Only shows particles for the first few blocks in the correct direction.
     * Updates every 0.5 seconds to reduce performance impact.
     */
    private BukkitTask startParticleTrail(Player player, TreasureCore treasure) {
        // Aim at an approximate target (offset-range), not the exact treasure location.
        // This keeps the hint helpful without giving away the exact spot.
        Location treasureLoc = treasure.getLocation();
        int offsetX = ThreadLocalRandom.current().nextInt(-visualHintOffsetRange, visualHintOffsetRange + 1);
        int offsetZ = ThreadLocalRandom.current().nextInt(-visualHintOffsetRange, visualHintOffsetRange + 1);
        final Location targetCenter = treasureLoc.clone().add(offsetX + 0.5, 0.5, offsetZ + 0.5);
        
        return new BukkitRunnable() {
            int ticksRemaining = visualHintDuration * 20;
            
            @Override
            public void run() {
                if (!player.isOnline() || ticksRemaining <= 0) {
                    cancel();
                    activeVisualHints.remove(player.getUniqueId());
                    return;
                }
                
                // Get player eye location (single allocation per update)
                Location playerEye = player.getEyeLocation();
                
                // Calculate direction vector to treasure
                Vector direction = targetCenter.toVector().subtract(playerEye.toVector());
                double actualDistance = direction.length();
                direction.normalize();
                
                // Only show particles for max-distance blocks (partial hint, not full path)
                double hintDistance = Math.min(actualDistance, visualHintMaxDistance);

                // Don't spawn right in the player's face
                double startDistance = Math.min(TRAIL_START_DISTANCE, hintDistance);
                
                // Spawn particles along the limited direction
                double steps = Math.ceil((hintDistance - startDistance) / PARTICLE_SPACING);
                for (int i = 0; i < steps; i++) {
                    double offset = startDistance + (i * PARTICLE_SPACING);
                    Location particleLoc = playerEye.clone().add(direction.clone().multiply(offset)).add(0, TRAIL_Y_OFFSET, 0);
                    ParticleUtils.spawnParticleForPlayer(player, particleLoc, visualHintParticle, 1, TRAIL_OFFSET, TRAIL_OFFSET, TRAIL_OFFSET, 0);
                }
                
                ticksRemaining -= VISUAL_UPDATE_INTERVAL;
            }
        }.runTaskTimer(plugin, 0L, VISUAL_UPDATE_INTERVAL);
    }

    /**
     * Point player's compass toward treasure's general area (with random offset for hint, not exact location).
     * Compass points to approximate area within offset-range blocks of the treasure.
     */
    private BukkitTask startCompassPointing(Player player, TreasureCore treasure) {
        // Add random offset to treasure location for approximate pointing
        Location treasureLoc = treasure.getLocation();
        int offsetX = ThreadLocalRandom.current().nextInt(-visualHintOffsetRange, visualHintOffsetRange + 1);
        int offsetZ = ThreadLocalRandom.current().nextInt(-visualHintOffsetRange, visualHintOffsetRange + 1);
        Location approximateTarget = treasureLoc.clone().add(offsetX, 0, offsetZ);
        
        player.setCompassTarget(approximateTarget);
        
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
     * Create a vertical beacon beam near treasure's general area (with offset for hint, not exact location).
     * Beacon appears within offset-range blocks of the actual treasure location.
     */
    private BukkitTask startBeaconEffect(Player player, TreasureCore treasure) {
        // Add random offset to treasure location for approximate beacon placement
        Location treasureLoc = treasure.getLocation();
        int offsetX = ThreadLocalRandom.current().nextInt(-visualHintOffsetRange, visualHintOffsetRange + 1);
        int offsetZ = ThreadLocalRandom.current().nextInt(-visualHintOffsetRange, visualHintOffsetRange + 1);
        
        // Pre-calculate base location with offset (immutable)
        final Location baseLoc = treasureLoc.clone().add(offsetX + 0.5, 0, offsetZ + 0.5);
        final int maxHeight = Math.min(BEACON_HEIGHT, baseLoc.getWorld().getMaxHeight() - baseLoc.getBlockY());
        
        return new BukkitRunnable() {
            int ticksRemaining = visualHintDuration * 20;
            
            @Override
            public void run() {
                if (!player.isOnline() || ticksRemaining <= 0) {
                    cancel();
                    activeVisualHints.remove(player.getUniqueId());
                    return;
                }
                
                // Spawn vertical beam of particles in approximate area
                for (int y = 0; y < maxHeight; y += BEACON_Y_STEP) {
                    Location particleLoc = baseLoc.clone().add(0, y, 0);
                    ParticleUtils.spawnParticleForPlayer(player, particleLoc, visualHintParticle, 1, BEACON_OFFSET, BEACON_OFFSET, BEACON_OFFSET, 0);
                }
                
                ticksRemaining -= VISUAL_UPDATE_INTERVAL;
            }
        }.runTaskTimer(plugin, 0L, VISUAL_UPDATE_INTERVAL);
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
