package de.theredend2000.advancedhunt.util;

import de.theredend2000.advancedhunt.platform.PlatformAccess;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ParticleUtils {

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
        PlatformAccess.get().spawnParticle(location, particleName, count, offsetX, offsetY, offsetZ, speed);
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
        PlatformAccess.get().spawnParticleForPlayer(player, location, particleName, count, offsetX, offsetY, offsetZ, speed);
    }

    /**
     * Checks if the server is running a legacy version (1.8.x).
     *
     * @return true if running on 1.8.x, false otherwise
     */
    public static boolean isLegacy() {
        // Kept for backward compatibility with existing callers.
        // Legacy means 1.8.x.
        String base = org.bukkit.Bukkit.getBukkitVersion().split("-")[0];
        return base.startsWith("1.8");
    }

    /**
     * Resets the warning flag for invalid particles.
     * Should be called on plugin reload.
     */
    public static void resetWarnings() {
        // No-op: caching/warnings are handled inside the platform adapter.
    }
}
