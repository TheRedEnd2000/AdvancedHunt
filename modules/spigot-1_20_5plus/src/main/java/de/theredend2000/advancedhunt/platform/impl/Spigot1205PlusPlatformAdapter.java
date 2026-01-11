package de.theredend2000.advancedhunt.platform.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

    protected enum ArmorStandSpawnMode {
        SPAWN_LIVING_INLINE_META,
        SPAWN_ENTITY_THEN_META
    }

    @Override
    public void applyHideTooltip(ItemMeta meta, boolean hide) {
        if (meta == null) return;
        meta.setHideTooltip(hide);
    }

    protected ClientVersion getClientVersion(Player player) {
        try {
            if (player == null) return null;
            if (!PacketEvents.getAPI().isInitialized()) return null;
            return PacketEvents.getAPI().getPlayerManager().getClientVersion(player);
        } catch (Throwable ignored) {
            return null;
        }
    }

    protected ServerVersion getServerVersion() {
        try {
            if (!PacketEvents.getAPI().isInitialized()) return null;
            return PacketEvents.getAPI().getServerManager().getVersion();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * 1.21+ uses the generic spawn-entity packet for armor stands.
     * Older protocol tiers expect the living-entity spawn packet.
     */
    protected boolean shouldUseSpawnEntity(Player player) {
        ClientVersion clientVersion = getClientVersion(player);
        if (clientVersion != null) {
            return clientVersion.isNewerThanOrEquals(ClientVersion.V_1_21);
        }
        ServerVersion serverVersion = getServerVersion();
        return serverVersion != null && serverVersion.isNewerThanOrEquals(ServerVersion.V_1_21);
    }

    /**
     * Armor stand flags index shifted in the 1.20.5 protocol tier.
     * We base this on the player's protocol version when available to be compatible with ViaVersion setups.
     */
    protected int getArmorStandFlagsIndex(Player player) {
        ClientVersion clientVersion = getClientVersion(player);
        if (clientVersion != null) {
            return clientVersion.isOlderThan(ClientVersion.V_1_20_5) ? 14 : 15;
        }
        // This adapter is only selected on 1.20.5+ servers, so default to the modern index.
        return 15;
    }

    protected ArmorStandSpawnMode resolveArmorStandSpawnMode(Player player) {
        return shouldUseSpawnEntity(player)
                ? ArmorStandSpawnMode.SPAWN_ENTITY_THEN_META
                : ArmorStandSpawnMode.SPAWN_LIVING_INLINE_META;
    }

    @Override
    public boolean spawnHologramArmorStandForPlayer(Player player, int entityId, UUID entityUuid, Location location, String customName) {
        return spawnHologramArmorStandForPlayerWithMode(
            player,
            entityId,
            entityUuid,
            location,
            customName,
            resolveArmorStandSpawnMode(player)
        );
    }

    protected boolean spawnHologramArmorStandForPlayerWithMode(Player player, int entityId, UUID entityUuid, Location location, String customName, ArmorStandSpawnMode mode) {
        if (player == null || location == null) return false;
        if (location.getWorld() == null) return false;

        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("packetevents")
                    && !Bukkit.getPluginManager().isPluginEnabled("PacketEvents")) {
                return false;
            }
        } catch (Throwable ignored) {
            return false;
        }

        try {
            if (!PacketEvents.getAPI().isInitialized()) return false;

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

            if (mode == ArmorStandSpawnMode.SPAWN_ENTITY_THEN_META) {
                // 1.21+: spawn entity + separate metadata
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
            }

            // 1.20.5-1.20.6: living entity spawn still works, metadata can be included inline.
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
}
