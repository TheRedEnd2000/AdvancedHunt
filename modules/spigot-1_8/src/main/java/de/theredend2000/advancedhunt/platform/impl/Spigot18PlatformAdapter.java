package de.theredend2000.advancedhunt.platform.impl;

import de.theredend2000.advancedhunt.platform.PlatformAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public final class Spigot18PlatformAdapter implements PlatformAdapter {

    // Delegates keep 1.8-specific NMS/effect logic out of the adapter surface.

    private final LegacyActionBarSender actionBarSender = new LegacyActionBarSender();
    private final LegacyParticleSpawner particleSpawner = new LegacyParticleSpawner();

    @Override
    public boolean isAir(Material material) {
        if (material == null) return true;
        return "AIR".equals(material.name());
    }

    @Override
    public void sendActionBar(Player player, String message) {
        actionBarSender.send(player, message);
    }

    @Override
    public void spawnParticle(Location location, String particleName, int count,
                              double offsetX, double offsetY, double offsetZ, double speed) {
        particleSpawner.spawn(location, particleName, count, offsetX, offsetY, offsetZ, speed);
    }

    @Override
    public void spawnParticleForPlayer(org.bukkit.entity.Player player, Location location, String particleName, int count,
                                       double offsetX, double offsetY, double offsetZ, double speed) {
        // Optional: use PacketEvents (if installed) to send per-player particle packets.
        // Fallback remains a no-op to preserve prior behavior when PacketEvents is absent.
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("packetevents")) return;
        } catch (Throwable ignored) {
            return;
        }

        PacketEventsParticleSender.spawn(player, location, particleName, count, offsetX, offsetY, offsetZ, speed);
    }

    @Override
    public void applyHideTooltip(ItemMeta meta, boolean hide) {
        // Not supported.
    }

    @Override
    public void applyUnbreakable(ItemMeta meta, boolean unbreakable) {
        // Not supported.
    }

    @Override
    public void applyCustomModelData(ItemMeta meta, Integer customModelData) {
        // Not supported.
    }

    @Override
    public void applySkullOwner(ItemMeta meta, UUID ownerUuid) {
        // 1.8 only supports owner by name. Keep no-op.
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

    @Override
    public boolean isMainHandInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        return true;
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
    public void setFireworkSilent(org.bukkit.entity.Firework firework, boolean silent) {
        // 1.8 does not support silent entities.
    }
}
