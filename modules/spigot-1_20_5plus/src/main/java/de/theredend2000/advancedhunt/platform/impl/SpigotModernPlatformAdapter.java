package de.theredend2000.advancedhunt.platform.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
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
public final class SpigotModernPlatformAdapter extends Spigot115PlatformAdapter {

    @Override
    public void applyHideTooltip(ItemMeta meta, boolean hide) {
        if (meta == null) return;
        meta.setHideTooltip(hide);
    }

    @Override
    public boolean spawnHologramArmorStandForPlayer(Player player, int entityId, UUID entityUuid, Location location, String customName) {
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
            // Note: marker armor stands (0x10) don't render a model, so there's nothing to outline.
            // If we want the client-side glow outline to be visible, don't use the marker flag.
            final byte armorStandFlags = (byte) (0x01 | 0x08);

            // 1.20.5+ (component-era) may shift tracked-data indices again.
            // Armor stand flags are expected at index 15 in this tier.
            List<EntityData<?>> meta = new ArrayList<>();
            meta.add(new EntityData<>(0,
                    EntityDataTypes.BYTE, invisibleFlag));
            meta.add(new EntityData<>(2,
                    EntityDataTypes.OPTIONAL_COMPONENT,
                    Optional.of(toJsonTextComponent(customName))));
            meta.add(new EntityData<>(3,
                    EntityDataTypes.BOOLEAN, true));
            meta.add(new EntityData<>(5,
                    EntityDataTypes.BOOLEAN, true));
            meta.add(new EntityData<>(15,
                    EntityDataTypes.BYTE, armorStandFlags));

                // 1.21.x: ArmorStands are spawned using the generic spawn-entity packet (not spawn-living-entity).
                // Also, metadata is not guaranteed to be accepted inline with the spawn packet on newer protocol tiers.
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
