package de.theredend2000.advancedhunt.platform.impl;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

/**
 * Legacy alias kept for IDE configurations / old references.
 *
 * Kept as a thin delegating wrapper so we don't rely on inheritance
 * (the real implementation class is {@link Spigot1205PlusPlatformAdapter}).
 */
public final class SpigotModernPlatformAdapter extends Spigot115PlatformAdapter {

	private final Spigot1205PlusPlatformAdapter delegate = new Spigot1205PlusPlatformAdapter();

	@Override
	public void applyHideTooltip(ItemMeta meta, boolean hide) {
		delegate.applyHideTooltip(meta, hide);
	}

	@Override
	public boolean spawnHologramArmorStandForPlayer(Player player, int entityId, UUID entityUuid, Location location, String customName) {
		return delegate.spawnHologramArmorStandForPlayer(player, entityId, entityUuid, location, customName);
	}
}
