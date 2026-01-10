package de.theredend2000.advancedhunt.platform.impl;

import com.cryptomorin.xseries.particles.XParticle;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

final class ModernParticleSpawner {

    private static final Map<String, Particle> PARTICLE_CACHE = new ConcurrentHashMap<>();

    void spawn(Location location, String particleName, int count,
               double offsetX, double offsetY, double offsetZ, double speed) {
        if (location == null) return;
        World world = location.getWorld();
        if (world == null) return;

        Particle particle = resolveParticle(particleName);
        if (particle == null) return;

        try {
            world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
        } catch (Throwable ignored) {
        }
    }

    void spawnForPlayer(Player player, Location location, String particleName, int count,
                       double offsetX, double offsetY, double offsetZ, double speed) {
        if (player == null || location == null) return;
        if (location.getWorld() == null) return;

        Particle particle = resolveParticle(particleName);
        if (particle == null) return;

        try {
            player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
        } catch (Throwable ignored) {
        }
    }

    private static Particle resolveParticle(String particleName) {
        if (particleName == null) return null;
        String upper = particleName.trim().toUpperCase();
        if (upper.isEmpty()) return null;

        Particle cached = PARTICLE_CACHE.get(upper);
        if (cached != null) return cached;

        Optional<XParticle> xParticle = XParticle.of(upper);
        Particle particle = xParticle.map(XParticle::get).orElse(null);
        if (particle == null) return null;

        PARTICLE_CACHE.put(upper, particle);
        return particle;
    }
}
