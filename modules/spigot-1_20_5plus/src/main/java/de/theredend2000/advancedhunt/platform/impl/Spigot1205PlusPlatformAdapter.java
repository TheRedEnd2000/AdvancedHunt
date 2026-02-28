package de.theredend2000.advancedhunt.platform.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

/**
 * Adapter for Spigot 1.20.5+ API.
 * Only overrides methods introduced/changed in 1.20.5+ (e.g. hide tooltip).
 */
public class Spigot1205PlusPlatformAdapter extends Spigot115PlatformAdapter {

    @Override
    public void applyHideTooltip(ItemMeta meta, boolean hide) {
        if (meta == null) return;
        meta.setHideTooltip(hide);
    }

    /**
     * Armor stand flags index shifted in the 1.20.5 protocol tier.
     *
     * This adapter is only selected on 1.20.5–1.20.6 servers, so we use the modern index.
     */
    protected int getArmorStandFlagsIndex(Player player) {
        return 15;
    }

    protected boolean isPacketEventsReady() {
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("packetevents")
                && !Bukkit.getPluginManager().isPluginEnabled("PacketEvents")) {
                return false;
            }
        } catch (Throwable ignored) {
            return false;
        }

        try {
            return PacketEvents1205Bridge.isInitialized();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean spawnHologramArmorStandForPlayer(Player player, int entityId, UUID entityUuid, Location location, String customName) {
        if (player == null || location == null) return false;
        if (location.getWorld() == null) return false;
        if (!isPacketEventsReady()) return false;
        try {
            return PacketEvents1205Bridge.spawnHologramArmorStand1205(
                    player, entityId, entityUuid, location,
                    toJsonTextComponent(customName), getArmorStandFlagsIndex(player));
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean spawnGlowingBlockMarkerForPlayer(Player player, int entityId, UUID entityUuid, Location blockLocation) {
        if (player == null || blockLocation == null) return false;
        if (blockLocation.getWorld() == null) return false;
        if (!isPacketEventsReady()) {
            return super.spawnGlowingBlockMarkerForPlayer(player, entityId, entityUuid, blockLocation);
        }
        try {
            boolean result = PacketEvents1205Bridge.spawnGlowingBlockMarker(player, entityId, entityUuid, blockLocation);
            if (!result) {
                return super.spawnGlowingBlockMarkerForPlayer(player, entityId, entityUuid, blockLocation);
            }
            return true;
        } catch (Throwable ignored) {
            return super.spawnGlowingBlockMarkerForPlayer(player, entityId, entityUuid, blockLocation);
        }
    }

    @Override
    public void sendSkullUpdatePacket(Player player, Location loc, String texture) {
        if (player == null || loc == null || texture == null || texture.isEmpty()) return;
        if (loc.getWorld() == null) return;
        if (!isPacketEventsReady()) {
            super.sendSkullUpdatePacket(player, loc, texture);
            return;
        }
        try {
            PacketEvents1205Bridge.sendSkullUpdatePacket(player, loc, texture);
        } catch (Throwable ignored) {
            super.sendSkullUpdatePacket(player, loc, texture);
        }
    }
}
