package de.theredend2000.advancedhunt.platform.impl;

import de.theredend2000.advancedhunt.platform.PlatformAdapter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Adapter for Spigot 1.9+ API.
 * - Particles via Bukkit Particle API (XParticle name mapping)
 */
public class Spigot19PlatformAdapter implements PlatformAdapter {
    private final ModernParticleSpawner particleSpawner = new ModernParticleSpawner();

    @Override
    public boolean isAir(Material material) {
        if (material == null) return true;
        return "AIR".equals(material.name());
    }

    @Override
    public void spawnParticle(Location location, String particleName, int count,
                              double offsetX, double offsetY, double offsetZ, double speed) {
        particleSpawner.spawn(location, particleName, count, offsetX, offsetY, offsetZ, speed);
    }

    @Override
    public void spawnParticleForPlayer(Player player, Location location, String particleName, int count,
                                       double offsetX, double offsetY, double offsetZ, double speed) {
        particleSpawner.spawnForPlayer(player, location, particleName, count, offsetX, offsetY, offsetZ, speed);
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
            if (type == null) return item;

            if ("SKULL".equals(type.name())) {
                Material skullItem = Material.getMaterial("SKULL_ITEM");
                if (skullItem != null) {
                    ItemMeta meta = item.getItemMeta();
                    ItemStack converted = new ItemStack(skullItem, item.getAmount());
                    if (meta != null) {
                        converted.setItemMeta(meta);
                    }
                    item = converted;
                    type = item.getType();
                }
            }

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

}
