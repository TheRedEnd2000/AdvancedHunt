package de.theredend2000.advancedhunt.platform.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
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
		if (player == null || location == null) return false;
		if (location.getWorld() == null) return false;
		if (!isPacketEventsReady()) return false;

		try {
			List<EntityData<?>> meta = buildHologramArmorStandMetadata(player, customName);

			WrapperPlayServerSpawnEntity spawnPacket =
				new WrapperPlayServerSpawnEntity(
					entityId,
					Optional.of(entityUuid),
					EntityTypes.ARMOR_STAND,
					new Vector3d(location.getX(), location.getY(), location.getZ()),
					0.0f,
					0.0f,
					0.0f,
					0,
					Optional.of(new Vector3d(0.0, 0.0, 0.0))
				);

			WrapperPlayServerEntityMetadata metaPacket =
				new WrapperPlayServerEntityMetadata(entityId, meta);

			PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPacket);
			PacketEvents.getAPI().getPlayerManager().sendPacket(player, metaPacket);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}
}