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
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
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
    public boolean isAir(Material material) {
        if (material == null) return true;
        return "AIR".equals(material.name());
    }

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
    public void applyHideTooltip(ItemMeta meta, boolean hide) {
        // Not supported in 1.9-1.20.4 base adapter.
    }

    @Override
    public void applyUnbreakable(ItemMeta meta, boolean unbreakable) {
        // Supported in later adapters.
    }

    @Override
    public void applyCustomModelData(ItemMeta meta, Integer customModelData) {
        // Supported in later adapters.
    }

    @Override
    public void applySkullOwner(ItemMeta meta, java.util.UUID ownerUuid) {
        // Supported in 1.13+ adapter.
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

    @Override
    public boolean isMainHandInteract(PlayerInteractEvent event) {
        if (event == null) return true;
        try {
            return event.getHand() == EquipmentSlot.HAND;
        } catch (Throwable ignored) {
            return true;
        }
    }

    @Override
    public String getBlockStateString(Block block) {
        if (block == null) return "0";
        try {
            return String.valueOf(block.getData());
        } catch (Throwable ignored) {
            return "0";
        }
    }

    @Override
    public void setFireworkSilent(Firework firework, boolean silent) {
        if (firework == null) return;
        try {
            firework.setSilent(silent);
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
