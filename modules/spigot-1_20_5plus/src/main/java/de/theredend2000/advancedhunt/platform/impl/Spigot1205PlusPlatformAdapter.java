package de.theredend2000.advancedhunt.platform.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
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
}
