package de.theredend2000.advancedHunt.util;

import com.cryptomorin.xseries.particles.XParticle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Utility class for spawning particles across different Minecraft versions.
 * Supports 1.8.8 through 1.21.x by detecting the server version at runtime.
 * <p>
 * For modern versions (1.9+), uses XSeries' {@link XParticle} for automatic version mapping.
 * For legacy versions (1.8.x), uses reflection to invoke spigot's playEffect method.
 */
public class ParticleUtils {

    private static final boolean IS_LEGACY;
    private static final Map<String, Particle> PARTICLE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> LEGACY_PARTICLE_MAPPINGS = new HashMap<>();
    private static boolean warnedInvalidParticle = false;

    // Cached reflection objects for legacy particle spawning
    private static Class<?> effectClass;
    private static Method effectValueOf;
    private static Method spigotMethod;
    private static Method playEffectMethod;
    private static boolean legacyReflectionInitialized = false;
    private static boolean legacyReflectionFailed = false;

    static {
        // Detect if we're on a legacy server (pre-1.9)
        boolean legacy;
        try {
            Class.forName("org.bukkit.Particle");
            legacy = false;
        } catch (ClassNotFoundException e) {
            legacy = true;
        }
        IS_LEGACY = legacy;

        // Legacy particle mappings only needed for 1.8.x reflection fallback
        // Modern versions use XParticle which handles all mappings automatically
        LEGACY_PARTICLE_MAPPINGS.put("VILLAGER_HAPPY", "HAPPY_VILLAGER");
        LEGACY_PARTICLE_MAPPINGS.put("SMOKE", "SMOKE_NORMAL");
        LEGACY_PARTICLE_MAPPINGS.put("SPLASH", "WATER_SPLASH");
        LEGACY_PARTICLE_MAPPINGS.put("BUBBLE", "WATER_BUBBLE");
        LEGACY_PARTICLE_MAPPINGS.put("RAIN", "WATER_DROP");
        LEGACY_PARTICLE_MAPPINGS.put("POOF", "EXPLOSION_NORMAL");
        LEGACY_PARTICLE_MAPPINGS.put("EXPLOSION", "EXPLOSION_LARGE");
        LEGACY_PARTICLE_MAPPINGS.put("UNDERWATER", "SUSPENDED_DEPTH");
        LEGACY_PARTICLE_MAPPINGS.put("WITCH", "SPELL_WITCH");
        LEGACY_PARTICLE_MAPPINGS.put("INSTANT_EFFECT", "SPELL_INSTANT");
        LEGACY_PARTICLE_MAPPINGS.put("ENTITY_EFFECT", "SPELL_MOB");
        LEGACY_PARTICLE_MAPPINGS.put("DUST", "REDSTONE");
        LEGACY_PARTICLE_MAPPINGS.put("ITEM_SNOWBALL", "SNOWBALL");
        LEGACY_PARTICLE_MAPPINGS.put("ITEM_SLIME", "SLIME");
        LEGACY_PARTICLE_MAPPINGS.put("BLOCK", "BLOCK_CRACK");
        LEGACY_PARTICLE_MAPPINGS.put("TOTEM_OF_UNDYING", "TOTEM");
    }

    /**
     * Spawns a particle at the given location with the specified parameters.
     * Automatically handles version differences between 1.8.8 and modern versions.
     * <p>
     * For modern versions (1.9+), uses XSeries' XParticle for automatic cross-version support.
     * For legacy versions (1.8.x), uses reflection with manual name mappings.
     *
     * @param location     The location to spawn the particle at
     * @param particleName The name of the particle (any version's name - XParticle handles mapping)
     * @param count        The number of particles to spawn
     * @param offsetX      The maximum random offset on the X axis
     * @param offsetY      The maximum random offset on the Y axis
     * @param offsetZ      The maximum random offset on the Z axis
     * @param speed        The speed of the particles
     */
    public static void spawnParticle(Location location, String particleName, int count, double offsetX, double offsetY, double offsetZ, double speed) {
        if (location == null || location.getWorld() == null) return;

        if (IS_LEGACY) {
            spawnLegacyParticle(location, particleName, count, offsetX, offsetY, offsetZ, speed);
        } else {
            spawnModernParticle(location, particleName, count, offsetX, offsetY, offsetZ, speed);
        }
    }

    /**
     * Spawns a particle at the given location visible only to the specified player.
     * This allows different players to see different particles at the same location.
     * Note: This method is only supported on modern versions (1.9+).
     * For legacy versions (1.8), use the simplified global particle system instead.
     *
     * @param player       The player who will see the particle
     * @param location     The location to spawn the particle at
     * @param particleName The name of the particle
     * @param count        The number of particles to spawn
     * @param offsetX      The maximum random offset on the X axis
     * @param offsetY      The maximum random offset on the Y axis
     * @param offsetZ      The maximum random offset on the Z axis
     * @param speed        The speed of the particles
     */
    public static void spawnParticleForPlayer(Player player, Location location, String particleName,
                                              int count, double offsetX, double offsetY, double offsetZ, double speed) {
        if (player == null || location == null || location.getWorld() == null) return;

        // Only supported on modern versions
        if (!IS_LEGACY) {
            spawnModernParticleForPlayer(player, location, particleName, count, offsetX, offsetY, offsetZ, speed);
        }
    }

    /**
     * Checks if the server is running a legacy version (1.8.x).
     *
     * @return true if running on 1.8.x, false otherwise
     */
    public static boolean isLegacy() {
        return IS_LEGACY;
    }

    /**
     * Resets the warning flag for invalid particles.
     * Should be called on plugin reload.
     */
    public static void resetWarnings() {
        warnedInvalidParticle = false;
        PARTICLE_CACHE.clear();
    }

    /**
     * Spawns particles using XParticle for modern versions (1.9+).
     * XParticle automatically handles all particle name mappings across versions.
     */
    private static void spawnModernParticle(Location location, String particleName, int count, double offsetX, double offsetY, double offsetZ, double speed) {
        Particle particle = resolveModernParticle(particleName);
        if (particle == null) return;

        try {
            location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
        } catch (Exception e) {
            // Silently fail - particle may not be supported on this specific version
        }
    }

    /**
     * Spawns particles visible only to a specific player for modern versions (1.9+).
     */
    private static void spawnModernParticleForPlayer(Player player, Location location, String particleName,
                                                     int count, double offsetX, double offsetY, double offsetZ, double speed) {
        Particle particle = resolveModernParticle(particleName);
        if (particle == null) return;

        try {
            player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
        } catch (Exception e) {
            // Silently fail - particle may not be supported on this specific version
        }
    }

    /**
     * Resolves a particle name to a Particle enum using XParticle.
     * XParticle handles all version-specific name mappings automatically.
     * Results are cached for performance.
     */
    private static Particle resolveModernParticle(String particleName) {
        String upperName = particleName.toUpperCase();

        // Check cache first
        Particle cached = PARTICLE_CACHE.get(upperName);
        if (cached != null) {
            return cached;
        }

        // Use XParticle for automatic cross-version mapping
        Optional<XParticle> xParticle = XParticle.of(upperName);
        Particle particle = xParticle.map(XParticle::get).orElse(null);

        if (particle != null) {
            PARTICLE_CACHE.put(upperName, particle);
        } else if (!warnedInvalidParticle) {
            Bukkit.getLogger().warning("[AdvancedHunt] Invalid particle name: " + particleName + ". Check config.yml for valid particle names.");
            warnedInvalidParticle = true;
        }

        return particle;
    }

    /**
     * Initializes the reflection objects needed for legacy particle spawning.
     * Only called once, results are cached.
     */
    private static synchronized void initLegacyReflection() {
        if (legacyReflectionInitialized || legacyReflectionFailed) return;

        try {
            effectClass = Class.forName("org.bukkit.Effect");
            effectValueOf = effectClass.getMethod("valueOf", String.class);
            spigotMethod = World.class.getMethod("spigot");
            
            // We need to get the Spigot class dynamically since it's an inner class
            Class<?> worldSpigotClass = Class.forName("org.bukkit.World$Spigot");
            playEffectMethod = worldSpigotClass.getMethod("playEffect",
                    Location.class,
                    effectClass,
                    int.class,
                    int.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class,
                    int.class,
                    int.class
            );
            
            legacyReflectionInitialized = true;
        } catch (Exception e) {
            legacyReflectionFailed = true;
            Bukkit.getLogger().log(Level.WARNING, "[AdvancedHunt] Failed to initialize legacy particle reflection. Particles will not work on 1.8.x", e);
        }
    }

    /**
     * Spawns particles using reflection for legacy versions (1.8.x).
     * Uses manual name mappings since XParticle doesn't support 1.8.
     */
    private static void spawnLegacyParticle(Location location, String particleName, int count, double offsetX, double offsetY, double offsetZ, double speed) {
        // Initialize reflection if not done yet
        if (!legacyReflectionInitialized && !legacyReflectionFailed) {
            initLegacyReflection();
        }
        
        if (legacyReflectionFailed) return;

        try {
            // Map modern particle names to legacy Effect enum names
            String legacyName = particleName.toUpperCase();
            if (LEGACY_PARTICLE_MAPPINGS.containsKey(legacyName)) {
                legacyName = LEGACY_PARTICLE_MAPPINGS.get(legacyName);
            }

            // Try to get the Effect enum value
            Object effect = null;
            try {
                effect = effectValueOf.invoke(null, legacyName);
            } catch (Exception e) {
                // Try the original name if mapping failed
                try {
                    effect = effectValueOf.invoke(null, particleName.toUpperCase());
                } catch (Exception ignored) {
                    // Effect not found
                }
            }

            if (effect != null) {
                World world = location.getWorld();
                Object spigot = spigotMethod.invoke(world);
                
                playEffectMethod.invoke(spigot,
                        location,
                        effect,
                        0,
                        0,
                        (float) offsetX,
                        (float) offsetY,
                        (float) offsetZ,
                        (float) speed,
                        count,
                        64 // View distance radius
                );
            } else if (!warnedInvalidParticle) {
                Bukkit.getLogger().warning("[AdvancedHunt] Invalid particle/effect name for 1.8: " + particleName);
                warnedInvalidParticle = true;
            }
        } catch (Exception e) {
            // Silently fail - may occur if the method signature differs on specific server implementations
        }
    }
}
