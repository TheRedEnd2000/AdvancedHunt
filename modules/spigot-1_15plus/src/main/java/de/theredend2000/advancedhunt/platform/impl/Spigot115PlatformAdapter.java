package de.theredend2000.advancedhunt.platform.impl;

import org.bukkit.Material;

/**
 * Adapter for Spigot 1.15+ API.
 * - Material#isAir()
 */
public class Spigot115PlatformAdapter extends Spigot114PlatformAdapter {

    @Override
    public boolean isAir(Material material) {
        if (material == null) return true;
        return material.isAir();
    }
}
