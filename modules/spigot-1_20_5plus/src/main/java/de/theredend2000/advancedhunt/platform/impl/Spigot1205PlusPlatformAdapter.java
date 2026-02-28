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
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
            return PacketEvents.getAPI().isInitialized();
        } catch (Throwable ignored) {
            return false;
        }
    }

    protected List<EntityData<?>> buildHologramArmorStandMetadata(Player player, String customName) {
        // Entity flags: 0x20 = invisible, 0x40 = glowing
        final byte invisibleFlag = (byte) (0x20 | 0x40);

        // Armor stand flags: 0x01 = small, 0x08 = no baseplate
        // Don't use marker (0x10), because marker armor stands don't render a model and thus can't show a glow outline.
        final byte armorStandFlags = (byte) (0x01 | 0x08);

        int armorStandFlagsIndex = getArmorStandFlagsIndex(player);

        List<EntityData<?>> meta = new ArrayList<>();
        meta.add(new EntityData<>(0, EntityDataTypes.BYTE, invisibleFlag));
        meta.add(new EntityData<>(2,
            EntityDataTypes.OPTIONAL_COMPONENT,
            Optional.of(toJsonTextComponent(customName))));
        meta.add(new EntityData<>(3, EntityDataTypes.BOOLEAN, true));
        meta.add(new EntityData<>(5, EntityDataTypes.BOOLEAN, true));
        meta.add(new EntityData<>(armorStandFlagsIndex, EntityDataTypes.BYTE, armorStandFlags));
        return meta;
    }

    @Override
    public boolean spawnHologramArmorStandForPlayer(Player player, int entityId, UUID entityUuid, Location location, String customName) {
        if (player == null || location == null) return false;
        if (location.getWorld() == null) return false;

        try {
            if (!isPacketEventsReady()) return false;

            List<EntityData<?>> meta = buildHologramArmorStandMetadata(player, customName);

            // 1.20.5-1.20.6: living entity spawn works and metadata can be included inline.
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

    @Override
    public boolean spawnGlowingBlockMarkerForPlayer(Player player, int entityId, UUID entityUuid, Location blockLocation) {
        if (player == null || blockLocation == null) return false;
        if (blockLocation.getWorld() == null) return false;
        if (!isPacketEventsReady()) {
            return super.spawnGlowingBlockMarkerForPlayer(player, entityId, entityUuid, blockLocation);
        }

        try {
            Block block = blockLocation.getBlock();
            if (block == null) {
                return super.spawnGlowingBlockMarkerForPlayer(player, entityId, entityUuid, blockLocation);
            }

            // Resolve the block state id for the BlockDisplay.
            int globalBlockStateId = 0;
            try {
                if (block.getBlockData() != null) {
                    WrappedBlockState wrapped = SpigotConversionUtil.fromBukkitBlockData(block.getBlockData());
                    globalBlockStateId = wrapped != null ? wrapped.getGlobalId() : 0;
                }
            } catch (Throwable ignored) {
            }

            // Base entity flags: 0x40 = glowing (do NOT set invisible; we want the block model rendered).
            final byte entityFlags = (byte) 0x40;

            // Display entity metadata indices:
            // - 11: translation (Vector3f)
            // - 12: scale (Vector3f)
            // - 16: brightness override (type differs by protocol tier)
            // BlockDisplay:
            // - 23: block state (int)
            List<EntityData<?>> meta = new ArrayList<>();
            meta.add(new EntityData<>(0, EntityDataTypes.BYTE, entityFlags));

            // Prevent the display from sampling block-internal lighting (which can look very dark on solid blocks).
            // Minecraft uses packed light coordinates: LightTexture.pack(blockLight, skyLight) == (sky << 20) | (block << 4)
            // Use full brightness (15,15) so the marker is consistently visible.
            final int packedFullBright = (15 << 20) | (15 << 4);
            try {
                ClientVersion clientVersion = PacketEvents.getAPI().getPlayerManager().getClientVersion(player);
                // clientVersion can be null during login - fall back to server version
                if (clientVersion == null) {
                    clientVersion = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
                }
                // 1.21+ uses an INT (default -1). Earlier component-era tiers used OptionalInt.
                if (clientVersion != null && clientVersion.isNewerThanOrEquals(ClientVersion.V_1_21)) {
                    meta.add(new EntityData<>(16, EntityDataTypes.INT, packedFullBright));
                } else {
                    meta.add(new EntityData<>(16, EntityDataTypes.OPTIONAL_INT, Optional.of(packedFullBright)));
                }
            } catch (Throwable ignored) {
                // Safe fallback: don't set brightness if we can't determine the client protocol.
            }

            // Slightly upscale to avoid z-fighting with the real world block.
            // Important: scaling is applied from the entity origin. Because we spawn at the block corner
            // (integer coords), we must apply a small negative translation so the upscale expands equally
            // towards all sides (+/-X, +/-Y, +/-Z) instead of only into the positive direction.
            //
            // Heads/skulls have thin top geometry that tends to z-fight more, so we add a bit more Y-scale
            // (still centered) to separate the top face.
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

            // Spawn at the block corner (integer coords).
            Location spawnLoc = blockLocation.clone();

            WrapperPlayServerSpawnEntity spawnPacket =
                    new WrapperPlayServerSpawnEntity(
                            entityId,
                            Optional.of(entityUuid),
                            EntityTypes.BLOCK_DISPLAY,
                            new Vector3d(spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ()),
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
            // 1.20.5+ uses "profile" instead of "SkullOwner" and IntArray for UUIDs.
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
            super.sendSkullUpdatePacket(player, loc, texture);
        }
    }
}
