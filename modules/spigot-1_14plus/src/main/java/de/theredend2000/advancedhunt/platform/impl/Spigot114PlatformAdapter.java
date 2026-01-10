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
 * Adapter for Spigot 1.14+ API.
 * - ItemMeta#setCustomModelData
 * - ItemMeta#setUnbreakable
 */
public class Spigot114PlatformAdapter extends Spigot113PlatformAdapter {

    @Override
    public void applyUnbreakable(ItemMeta meta, boolean unbreakable) {
        if (meta == null) return;
        meta.setUnbreakable(unbreakable);
    }

    @Override
    public void applyCustomModelData(ItemMeta meta, Integer customModelData) {
        if (meta == null) return;
        meta.setCustomModelData(customModelData);
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

            // 1.14+: armor stand-specific metadata indices shifted (base entity gained additional tracked data).
            // Armor stand flags are expected at index 14 in this tier.
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
            meta.add(new EntityData<>(14,
                    EntityDataTypes.BYTE, armorStandFlags));

            WrapperPlayServerSpawnLivingEntity packet =
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

            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
