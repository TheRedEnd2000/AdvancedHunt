package de.theredend2000.advancedhunt.platform.impl;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Adapter tier for 1.21+.
 *
 * 1.21 introduced protocol-level changes (notably for packet-only spawned entities like armor stands)
 * which are best handled in a dedicated tier, even though the Bukkit API surface is largely unchanged.
 */
public final class Spigot121PlusPlatformAdapter extends Spigot1205PlusPlatformAdapter {

	@Override
	public boolean spawnHologramArmorStandForPlayer(Player player, int entityId, UUID entityUuid, Location location, String customName) {
		// 1.21+ prefers spawn-entity + separate metadata.
		return spawnHologramArmorStandForPlayerWithMode(
			player,
			entityId,
			entityUuid,
			location,
			customName,
			ArmorStandSpawnMode.SPAWN_ENTITY_THEN_META
		);
	}
}