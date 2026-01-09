package de.theredend2000.advancedhunt.platform.impl;

import com.cryptomorin.xseries.particles.XParticle;
import de.theredend2000.advancedhunt.platform.PlatformAdapter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter for Spigot 1.9+ API.
 * - ActionBar via Player#spigot().sendMessage(ChatMessageType, ...)
 * - Particles via Bukkit Particle API
 */
public class Spigot19PlatformAdapter implements PlatformAdapter {

    private static final Map<String, Particle> PARTICLE_CACHE = new ConcurrentHashMap<>();

    @Override
    public void sendActionBar(Player player, String message) {
        if (player == null) return;
        BaseComponent[] components = TextComponent.fromLegacyText(message == null ? "" : message);
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void spawnParticle(Location location, String particleName, int count,
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

    @Override
    public void spawnParticleForPlayer(Player player, Location location, String particleName, int count,
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

    @Override
    public void applySkullOwner(ItemMeta meta, String ownerName) {
        if (!(meta instanceof SkullMeta)) return;
        if (ownerName == null || ownerName.trim().isEmpty()) return;
        try {
            ((SkullMeta) meta).setOwner(ownerName);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public ItemStack ensurePlayerHeadItem(ItemStack item) {
        if (item == null) return null;
        try {
            Material type = item.getType();
            if (type != null && "SKULL_ITEM".equals(type.name())) {
                if (item.getDurability() != (short) 3) {
                    item.setDurability((short) 3);
                }
            }
        } catch (Throwable ignored) {
        }
        return item;
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
