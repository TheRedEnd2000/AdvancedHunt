package de.theredend2000.advancedhunt.platform.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import de.theredend2000.advancedhunt.platform.PlatformAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Adapter for Spigot 1.9+ API.
 * - Particles via Bukkit Particle API (XParticle name mapping)
 */
public class Spigot19PlatformAdapter implements PlatformAdapter {
    private final ModernParticleSpawner particleSpawner = new ModernParticleSpawner();

    @Override
    public boolean isAir(Material material) {
        if (material == null) return true;
        return "AIR".equals(material.name());
    }

    @Override
    public void spawnParticle(Location location, String particleName, int count,
                              double offsetX, double offsetY, double offsetZ, double speed) {
        particleSpawner.spawn(location, particleName, count, offsetX, offsetY, offsetZ, speed);
    }

    @Override
    public void spawnParticleForPlayer(Player player, Location location, String particleName, int count,
                                       double offsetX, double offsetY, double offsetZ, double speed) {
        particleSpawner.spawnForPlayer(player, location, particleName, count, offsetX, offsetY, offsetZ, speed);
    }

    @Override
    public void applySkullOwner(ItemMeta meta, String ownerName) {
        if (!(meta instanceof SkullMeta)) return;
        if (ownerName == null || ownerName.trim().isEmpty()) return;
        try {
            ((SkullMeta) meta).setOwner(ownerName);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void applyHideTooltip(ItemMeta meta, boolean hide) {
        // Not supported in 1.9-1.20.4 base adapter.
    }

    @Override
    public void applyUnbreakable(ItemMeta meta, boolean unbreakable) {
        // Supported in later adapters.
    }

    @Override
    public void applyCustomModelData(ItemMeta meta, Integer customModelData) {
        // Supported in later adapters.
    }

    @Override
    public void applySkullOwner(ItemMeta meta, java.util.UUID ownerUuid) {
        // Supported in 1.13+ adapter.
    }

    @Override
    public ItemStack ensurePlayerHeadItem(ItemStack item) {
        if (item == null) return null;
        try {
            Material type = item.getType();
            if (type == null) return item;

            if ("SKULL".equals(type.name())) {
                Material skullItem = Material.getMaterial("SKULL_ITEM");
                if (skullItem != null) {
                    ItemMeta meta = item.getItemMeta();
                    ItemStack converted = new ItemStack(skullItem, item.getAmount());
                    if (meta != null) {
                        converted.setItemMeta(meta);
                    }
                    item = converted;
                    type = item.getType();
                }
            }

            if (type != null && "SKULL_ITEM".equals(type.name())) {
                if (item.getDurability() != (short) 3) {
                    item.setDurability((short) 3);
                }
            }
        } catch (Throwable ignored) {
        }
        return item;
    }

    @Override
    public boolean isMainHandInteract(PlayerInteractEvent event) {
        if (event == null) return true;
        try {
            return event.getHand() == EquipmentSlot.HAND;
        } catch (Throwable ignored) {
            return true;
        }
    }

    @Override
    public String getBlockStateString(Block block) {
        if (block == null) return "0";
        try {
            return String.valueOf(block.getData());
        } catch (Throwable ignored) {
            return "0";
        }
    }

    @Override
    public void setFireworkSilent(Firework firework, boolean silent) {
        if (firework == null) return;
        try {
            firework.setSilent(silent);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean spawnHologramArmorStandForPlayer(Player player, int entityId, UUID entityUuid, Location location, String customName) {
        if (player == null || location == null) return false;
        if (location.getWorld() == null) return false;

        // Only reference PacketEvents classes after confirming the plugin is enabled.
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

            // Entity flags: 0x20 = invisible, 0x40 = glowing (1.9+)
            final byte invisibleFlag = (byte) (0x20 | 0x40);
            // Note: marker armor stands (0x10) don't render a model, so there's nothing to outline.
            // If we want the client-side glow outline to be visible, don't use the marker flag.
            final byte armorStandFlags = (byte) (0x01 | 0x08);

            // 1.9-1.12: custom name is a plain string.
            // Armor stand flags index is 11 in this tier.
            List<EntityData<?>> meta = new ArrayList<>();
            meta.add(new EntityData<>(0,
                    EntityDataTypes.BYTE, invisibleFlag));
            meta.add(new EntityData<>(2,
                    EntityDataTypes.STRING, customName == null ? "" : customName));
            meta.add(new EntityData<>(3,
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
                            new Vector3d(0, 0.0, 0.0),
                            meta
                    );

            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean destroyEntitiesForPlayer(Player player, int... entityIds) {
        if (player == null) return false;
        if (entityIds == null || entityIds.length == 0) return false;

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
            WrapperPlayServerDestroyEntities packet =
                    new WrapperPlayServerDestroyEntities(entityIds);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

}
