package de.theredend2000.advancedhunt.platform.impl;

import org.bukkit.inventory.meta.ItemMeta;

/**
 * Adapter for Spigot 1.20.5+ API.
 * Only overrides methods introduced/changed in 1.20.5+ (e.g. hide tooltip).
 */
public final class SpigotModernPlatformAdapter extends Spigot115PlatformAdapter {

    @Override
    public void applyHideTooltip(ItemMeta meta, boolean hide) {
        if (meta == null) return;
        meta.setHideTooltip(hide);
    }
}
