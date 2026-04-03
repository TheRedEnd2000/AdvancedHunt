package de.theredend2000.advancedhunt.platform.impl;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import de.theredend2000.advancedhunt.platform.PlatformAdapter;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Spigot18PlatformAdapter implements PlatformAdapter {

    private final LegacyParticleSpawner particleSpawner = new LegacyParticleSpawner();

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
        // Optional: use PacketEvents (if installed) to send per-player particle packets.
        // Fallback remains a no-op to preserve prior behavior when PacketEvents is absent.
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("packetevents")) return;
        } catch (Throwable ignored) {
            return;
        }

        PacketEventsParticleSender.spawn(player, location, particleName, count, offsetX, offsetY, offsetZ, speed);
    }

    @Override
    public void applyHideTooltip(ItemMeta meta, boolean hide) {
        // Not supported.
    }

    @Override
    public void applyUnbreakable(ItemMeta meta, boolean unbreakable) {
        // Not supported.
    }

    @Override
    public void applyCustomModelData(ItemMeta meta, Integer customModelData) {
        // Not supported.
    }

    @Override
    public void applySkullOwner(ItemMeta meta, UUID ownerUuid) {
        // 1.8 only supports owner by name. Keep no-op.
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
    public ItemStack ensurePlayerHeadItem(ItemStack item) {
        if (item == null) return null;
        try {
            Material type = item.getType();
            if (type == null) return item;

            // Legacy: heads can be stored as the block material (SKULL) in some code paths.
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
                // 3 = player head (0 skeleton, 1 wither, 2 zombie, 3 player, 4 creeper, 5 dragon)
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
        return true;
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
        // 1.8 does not support silent entities.
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

            // Legacy 1.8 metadata indices/types.
            // 0: entity flags (byte) -> invis
            // 2: custom name (string)
            // 3: custom name visible (byte)
            // 10: armor stand flags (byte) -> small + no baseplate + marker
            final byte invisibleFlag = (byte) 0x20;
            final byte armorStandFlags = (byte) (0x01 | 0x08 | 0x10);

            List<EntityData<?>> meta = new ArrayList<>();
            meta.add(new EntityData<>(0,
                    EntityDataTypes.BYTE, invisibleFlag));
            meta.add(new EntityData<>(2,
                    EntityDataTypes.STRING, customName == null ? "" : customName));
            meta.add(new EntityData<>(3,
                    EntityDataTypes.BYTE, (byte) 1));
            meta.add(new EntityData<>(10,
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

    @Override
    public boolean spawnGlowingBlockMarkerForPlayer(Player player, int entityId, UUID entityUuid, Location blockLocation) {
        // 1.8 does not support the glowing outline feature.
        // Callers should provide a fallback (e.g., per-player particles).
        return false;
    }

    @Override
    public void sendClickableCopyText(Player player, String displayText, String copyText, String hoverText) {
        // 1.8-1.14 doesn't support COPY_TO_CLIPBOARD, use SUGGEST_COMMAND instead
        // When clicked, it will populate the player's chat box with the text
        TextComponent component = new TextComponent(displayText);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, copyText));
        
        if (hoverText != null && !hoverText.isEmpty()) {
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverText).create()));
        }
        
        player.spigot().sendMessage(component);
    }

    @Override
    public void sendSkullUpdatePacket(Player player, Location loc, String texture, String ownerName) {
        if (player == null || loc == null) return;

        String normalizedTexture = normalizeProfileValue(texture);
        String normalizedOwnerName = normalizeProfileValue(ownerName);
        if (normalizedTexture == null && normalizedOwnerName == null) return;

        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("packetevents")
                && !Bukkit.getPluginManager().isPluginEnabled("PacketEvents")) {
                return;
            }
            if (!PacketEvents.getAPI().isInitialized()) return;

            Vector3i pos = new Vector3i(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            NBTCompound root = new NBTCompound();
            root.setTag("id", new NBTString("minecraft:skull"));
            root.setTag("SkullType", new NBTByte((byte) 3));

            NBTCompound skullOwner = new NBTCompound();
            if (normalizedOwnerName != null) {
                skullOwner.setTag("Name", new NBTString(normalizedOwnerName));
            }
            if (normalizedTexture != null) {
                skullOwner.setTag("Id", new NBTString(UUID.randomUUID().toString()));

                NBTCompound properties = new NBTCompound();
                NBTList<NBTCompound> textures = NBTList.createCompoundList();
                NBTCompound textureTag = new NBTCompound();
                textureTag.setTag("Value", new NBTString(normalizedTexture));
                textures.addTag(textureTag);

                properties.setTag("textures", textures);
                skullOwner.setTag("Properties", properties);
            }
            root.setTag("SkullOwner", skullOwner);

            WrapperPlayServerBlockEntityData packet = new WrapperPlayServerBlockEntityData(pos, BlockEntityTypes.SKULL, root);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        } catch (Throwable ignored) {
        }
    }

    private String normalizeProfileValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
