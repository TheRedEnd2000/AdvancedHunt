package de.theredend2000.advancedhunt.platform.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTIntArray;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Isolates all PacketEvents references so that the adapter classes
 * (Spigot1205PlusPlatformAdapter, Spigot121PlusPlatformAdapter) contain zero PacketEvents
 * imports and can be loaded by the JVM even when PacketEvents is not installed.
 *
 * The JVM loads this class during bytecode verification of the adapter classes, but does NOT
 * immediately link or verify it. It is only linked — and PacketEvents is first accessed — when
 * one of the static methods here is first called. By that time, the caller has already confirmed
 * PacketEvents is enabled via the Bukkit plugin manager.
 */
final class PacketEvents1205Bridge {

    private PacketEvents1205Bridge() {}

    static boolean isInitialized() {
        try {
            return PacketEvents.getAPI().isInitialized();
        } catch (Throwable t) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Shared metadata builder
    // -------------------------------------------------------------------------

    static List<EntityData<?>> buildHologramArmorStandMeta(String jsonCustomName, int armorStandFlagsIndex) {
        final byte invisibleFlag = (byte) (0x20 | 0x40);
        final byte armorStandFlags = (byte) (0x01 | 0x08);

        List<EntityData<?>> meta = new ArrayList<>();
        meta.add(new EntityData<>(0, EntityDataTypes.BYTE, invisibleFlag));
        meta.add(new EntityData<>(2, EntityDataTypes.OPTIONAL_COMPONENT, Optional.of(jsonCustomName)));
        meta.add(new EntityData<>(3, EntityDataTypes.BOOLEAN, true));
        meta.add(new EntityData<>(5, EntityDataTypes.BOOLEAN, true));
        meta.add(new EntityData<>(armorStandFlagsIndex, EntityDataTypes.BYTE, armorStandFlags));
        return meta;
    }

    // -------------------------------------------------------------------------
    // Hologram armor stand — 1.20.5/1.20.6: SpawnLivingEntity with inline meta
    // -------------------------------------------------------------------------

    static boolean spawnHologramArmorStand1205(Player player, int entityId, UUID entityUuid,
            Location location, String jsonCustomName, int armorStandFlagsIndex) {
        try {
            List<EntityData<?>> meta = buildHologramArmorStandMeta(jsonCustomName, armorStandFlagsIndex);

            WrapperPlayServerSpawnLivingEntity spawnPacket =
                    new WrapperPlayServerSpawnLivingEntity(
                            entityId,
                            entityUuid,
                            EntityTypes.ARMOR_STAND,
                            new Vector3d(location.getX(), location.getY(), location.getZ()),
                            0.0f,
                            0.0f,
                            0.0f,
                            new Vector3d(0.0, 0.0, 0.0),
                            meta
                    );

            PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPacket);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Glowing block marker (BlockDisplay entity)
    // -------------------------------------------------------------------------

    static boolean spawnGlowingBlockMarker(Player player, int entityId, UUID entityUuid,
            Location blockLocation) {
        try {
            Block block = blockLocation.getBlock();
            if (block == null) return false;

            int globalBlockStateId = 0;
            try {
                if (block.getBlockData() != null) {
                    WrappedBlockState wrapped = SpigotConversionUtil.fromBukkitBlockData(block.getBlockData());
                    globalBlockStateId = wrapped != null ? wrapped.getGlobalId() : 0;
                }
            } catch (Throwable ignored) {
            }

            // Base entity flags: 0x40 = glowing.
            final byte entityFlags = (byte) 0x40;

            List<EntityData<?>> meta = new ArrayList<>();
            meta.add(new EntityData<>(0, EntityDataTypes.BYTE, entityFlags));

            // Full brightness so the marker is consistently visible.
            // 1.20.5–1.20.6 uses OPTIONAL_INT; INT is not applicable here as this adapter
            // tier only runs on 1.20.5–1.20.6 servers.
            final int packedFullBright = (15 << 20) | (15 << 4);
            try {
                meta.add(new EntityData<>(16, EntityDataTypes.OPTIONAL_INT, Optional.of(packedFullBright)));
            } catch (Throwable ignored) {
            }

            final String typeName = block.getType().name();
            final boolean isHeadOrSkull = typeName.endsWith("_HEAD") || typeName.endsWith("_SKULL");

            final float scaleX = isHeadOrSkull ? 1.0030f : 1.0025f;
            final float scaleY = isHeadOrSkull ? 1.0060f : 1.0025f;
            final float scaleZ = isHeadOrSkull ? 1.0030f : 1.0025f;

            final float halfExpandX = (scaleX - 1.0f) / 2.0f;
            final float halfExpandY = (scaleY - 1.0f) / 2.0f;
            final float halfExpandZ = (scaleZ - 1.0f) / 2.0f;
            meta.add(new EntityData<>(11, EntityDataTypes.VECTOR3F, new Vector3f(-halfExpandX, -halfExpandY, -halfExpandZ)));
            meta.add(new EntityData<>(12, EntityDataTypes.VECTOR3F, new Vector3f(scaleX, scaleY, scaleZ)));
            meta.add(new EntityData<>(23, EntityDataTypes.BLOCK_STATE, globalBlockStateId));

            Location spawnLoc = blockLocation.clone();

            WrapperPlayServerSpawnLivingEntity spawnPacket =
                    new WrapperPlayServerSpawnLivingEntity(
                            entityId,
                            entityUuid,
                            EntityTypes.BLOCK_DISPLAY,
                            new Vector3d(spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ()),
                            0.0f,
                            0.0f,
                            0.0f,
                            new Vector3d(0.0, 0.0, 0.0),
                            meta
                    );

            PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPacket);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Skull block entity update packet (1.20.5+ profile format)
    // -------------------------------------------------------------------------

    static void sendSkullUpdatePacket(Player player, Location loc, String texture) {
        try {
            Vector3i pos = new Vector3i(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            NBTCompound root = new NBTCompound();
            root.setTag("id", new NBTString("minecraft:skull"));

            NBTCompound profile = new NBTCompound();

            UUID randomUUID = UUID.randomUUID();
            int[] uuidInts = new int[] {
                (int) (randomUUID.getMostSignificantBits() >> 32),
                (int) randomUUID.getMostSignificantBits(),
                (int) (randomUUID.getLeastSignificantBits() >> 32),
                (int) randomUUID.getLeastSignificantBits()
            };
            profile.setTag("id", new NBTIntArray(uuidInts));

            NBTList<NBTCompound> properties = NBTList.createCompoundList();
            NBTCompound textureTag = new NBTCompound();
            textureTag.setTag("name", new NBTString("textures"));
            textureTag.setTag("value", new NBTString(texture));
            properties.addTag(textureTag);

            profile.setTag("properties", properties);
            root.setTag("profile", profile);

            WrapperPlayServerBlockEntityData packet = new WrapperPlayServerBlockEntityData(pos, BlockEntityTypes.SKULL, root);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        } catch (Throwable ignored) {
        }
    }
}
