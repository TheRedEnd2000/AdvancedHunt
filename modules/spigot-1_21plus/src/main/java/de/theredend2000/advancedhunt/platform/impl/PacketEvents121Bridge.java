package de.theredend2000.advancedhunt.platform.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Isolates 1.21+-specific PacketEvents references from Spigot121PlusPlatformAdapter.
 *
 * In 1.21 the living-entity spawn packet was removed; armor stands must now be spawned
 * with the generic WrapperPlayServerSpawnEntity followed by a separate
 * WrapperPlayServerEntityMetadata packet.
 *
 * This class is only linked by the JVM when one of its methods is first called, so
 * PacketEvents does not need to be present at class-load time.
 */
final class PacketEvents121Bridge {

    private PacketEvents121Bridge() {}

    // -------------------------------------------------------------------------
    // Hologram armor stand — 1.21+: separate SpawnEntity + EntityMetadata packets
    // -------------------------------------------------------------------------

    static boolean spawnHologramArmorStand(Player player, int entityId, UUID entityUuid,
            Location location, String jsonCustomName, int armorStandFlagsIndex) {
        try {
            final byte invisibleFlag = (byte) (0x20 | 0x40);
            final byte armorStandFlags = (byte) (0x01 | 0x08);

            List<EntityData<?>> meta = new ArrayList<>();
            meta.add(new EntityData<>(0, EntityDataTypes.BYTE, invisibleFlag));
            meta.add(new EntityData<>(2, EntityDataTypes.OPTIONAL_COMPONENT, Optional.of(jsonCustomName)));
            meta.add(new EntityData<>(3, EntityDataTypes.BOOLEAN, true));
            meta.add(new EntityData<>(5, EntityDataTypes.BOOLEAN, true));
            meta.add(new EntityData<>(armorStandFlagsIndex, EntityDataTypes.BYTE, armorStandFlags));

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
