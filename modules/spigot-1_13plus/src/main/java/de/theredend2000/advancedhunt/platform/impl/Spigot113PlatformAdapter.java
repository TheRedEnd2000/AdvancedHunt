package de.theredend2000.advancedhunt.platform.impl;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

/**
 * Adapter for Spigot 1.13+ API.
 * - Player heads as Material.PLAYER_HEAD
 * - SkullMeta#setOwningPlayer
 */
public class Spigot113PlatformAdapter extends Spigot19PlatformAdapter {

    @Override
    public boolean isAir(Material material) {
        if (material == null) return true;
        String name = material.name();
        return "AIR".equals(name) || "CAVE_AIR".equals(name) || "VOID_AIR".equals(name);
    }

    @Override
    public void applySkullOwner(ItemMeta meta, UUID ownerUuid) {
        if (!(meta instanceof SkullMeta)) return;
        if (ownerUuid == null) return;
        ((SkullMeta) meta).setOwningPlayer(Bukkit.getOfflinePlayer(ownerUuid));
    }

    @Override
    public ItemStack ensurePlayerHeadItem(ItemStack item) {
        if (item == null) return null;
        try {
            Material type = item.getType();
            if (type != null && ("SKULL_ITEM".equals(type.name()) || "LEGACY_SKULL_ITEM".equals(type.name()))) {
                ItemMeta meta = item.getItemMeta();
                item.setType(Material.PLAYER_HEAD);
                if (meta != null) {
                    item.setItemMeta(meta);
                }
            }
        } catch (Throwable ignored) {
        }
        return item;
    }
}
