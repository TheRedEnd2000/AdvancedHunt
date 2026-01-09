package de.theredend2000.advancedhunt.platform.impl;

import org.bukkit.inventory.meta.ItemMeta;

/**
 * Adapter for Spigot 1.14+ API.
 * - ItemMeta#setCustomModelData
 * - ItemMeta#setUnbreakable
 */
public class Spigot114PlatformAdapter extends Spigot113PlatformAdapter {

    @Override
    public void applyUnbreakable(ItemMeta meta, boolean unbreakable) {
        if (meta == null) return;
        meta.setUnbreakable(unbreakable);
    }

    @Override
    public void applyCustomModelData(ItemMeta meta, Integer customModelData) {
        if (meta == null) return;
        meta.setCustomModelData(customModelData);
    }
}
