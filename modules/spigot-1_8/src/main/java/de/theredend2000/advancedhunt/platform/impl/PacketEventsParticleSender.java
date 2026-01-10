package de.theredend2000.advancedhunt.platform.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleType;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

/** Optional PacketEvents integration for 1.8 per-player particles. */
final class PacketEventsParticleSender {

    private PacketEventsParticleSender() {
    }

    static boolean spawn(Player player, Location location, String particleName, int count,
                         double offsetX, double offsetY, double offsetZ, double speed) {
        if (player == null || location == null) return false;
        if (location.getWorld() == null) return false;
        if (particleName == null || particleName.trim().isEmpty()) return false;

        if (!isPacketEventsPluginEnabled()) return false;
        if (!PacketEvents.getAPI().isInitialized()) return false;

        try {
            ParticleType<?> type = resolveParticleType(particleName);
            if (type == null) return false;

            Particle<?> particle = new Particle<>(type);
            Vector3d pos = new Vector3d(location.getX(), location.getY(), location.getZ());
            Vector3f offsets = new Vector3f((float) offsetX, (float) offsetY, (float) offsetZ);


            WrapperPlayServerParticle wrapper = new WrapperPlayServerParticle(
                    particle,
                    false,
                    pos,
                    offsets,
                    (float) speed,
                    count
            );

            PacketEvents.getAPI().getPlayerManager().sendPacket(player, wrapper);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isPacketEventsPluginEnabled() {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("packetevents");
            if (plugin == null) plugin = Bukkit.getPluginManager().getPlugin("PacketEvents");
            return plugin != null && plugin.isEnabled();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static ParticleType<?> resolveParticleType(String particleName) {
        // PacketEvents uses registry-like names; callers may supply Bukkit/XParticle-ish names.
        String raw = particleName.trim();
        String lower = raw.toLowerCase(Locale.ROOT);

        ParticleType<?> type = ParticleTypes.getByName(raw);
        if (type != null) return type;

        type = ParticleTypes.getByName(lower);
        if (type != null) return type;

        type = ParticleTypes.getByName(lower.replace(' ', '_').replace('-', '_'));
        if (type != null) return type;

        if (lower.startsWith("minecraft:")) {
            type = ParticleTypes.getByName(lower.substring("minecraft:".length()));
            if (type != null) return type;
        } else {
            type = ParticleTypes.getByName("minecraft:" + lower);
            if (type != null) return type;
        }

        // Common Bukkit/XParticle → registry swaps.
        if ("villager_happy".equals(lower) || "VILLAGER_HAPPY".equals(raw)) {
            type = ParticleTypes.getByName("happy_villager");
            if (type != null) return type;
        }
        if ("villager_angry".equals(lower) || "VILLAGER_ANGRY".equals(raw)) {
            type = ParticleTypes.getByName("angry_villager");
            if (type != null) return type;
        }
        if ("smoke_normal".equals(lower) || "SMOKE_NORMAL".equals(raw)) {
            type = ParticleTypes.getByName("smoke");
            if (type != null) return type;
        }

        // Upper snake case → lower snake case.
        String maybeUpperSnake = raw.toUpperCase(Locale.ROOT);
        if (maybeUpperSnake.equals(raw) && raw.indexOf('_') >= 0) {
            type = ParticleTypes.getByName(raw.toLowerCase(Locale.ROOT));
            if (type != null) return type;
        }

        return null;
    }
}
