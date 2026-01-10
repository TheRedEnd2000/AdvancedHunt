package de.theredend2000.advancedhunt.platform.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter for Spigot 1.13+ API.
 * - Player heads as Material.PLAYER_HEAD
 * - SkullMeta#setOwningPlayer
 */
public class Spigot113PlatformAdapter extends Spigot19PlatformAdapter {

    @Override
    public boolean isAir(Material material) {
        if (material == null) return true;
        String name = material.name();
        return "AIR".equals(name) || "CAVE_AIR".equals(name) || "VOID_AIR".equals(name);
    }

    @Override
    public void applySkullOwner(ItemMeta meta, UUID ownerUuid) {
        if (!(meta instanceof SkullMeta)) return;
        if (ownerUuid == null) return;
        ((SkullMeta) meta).setOwningPlayer(Bukkit.getOfflinePlayer(ownerUuid));
    }

    @Override
    public ItemStack ensurePlayerHeadItem(ItemStack item) {
        if (item == null) return null;
        try {
            Material type = item.getType();
            if (type != null && "PLAYER_WALL_HEAD".equals(type.name())) {
                ItemMeta meta = item.getItemMeta();
                item.setType(Material.PLAYER_HEAD);
                if (meta != null) {
                    item.setItemMeta(meta);
                }
                return item;
            }

            if (type != null && ("SKULL_ITEM".equals(type.name()) || "LEGACY_SKULL_ITEM".equals(type.name()))) {
                ItemMeta meta = item.getItemMeta();
                item.setType(Material.PLAYER_HEAD);
                if (meta != null) {
                    item.setItemMeta(meta);
                }
            }
        } catch (Throwable ignored) {
        }
        return item;
    }

    @Override
    public String getBlockStateString(Block block) {
        if (block == null) return "";
        try {
            BlockData data = block.getBlockData();
            return data != null ? data.getAsString() : "";
        } catch (Throwable ignored) {
            return "";
        }
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
            final byte armorStandFlags = (byte) (0x01 | 0x08 | 0x10);

            // 1.13+: custom name is an optional chat component.
            // Armor stand flags are still at index 11 in 13.x.
            List<EntityData<?>> meta = new ArrayList<>();
            meta.add(new EntityData<>(0,
                    EntityDataTypes.BYTE, invisibleFlag));
            meta.add(new EntityData<>(2,
                    EntityDataTypes.OPTIONAL_ADV_COMPONENT,
                    Optional.of(Component.text(customName == null ? "" : customName))));
            meta.add(new EntityData<>(3,
                    EntityDataTypes.BOOLEAN, true));
            meta.add(new EntityData<>(5,
                    EntityDataTypes.BOOLEAN, true));
            meta.add(new EntityData<>(11,
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
