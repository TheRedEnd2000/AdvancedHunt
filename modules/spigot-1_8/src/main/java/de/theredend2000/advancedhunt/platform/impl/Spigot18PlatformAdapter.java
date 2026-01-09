package de.theredend2000.advancedhunt.platform.impl;

import de.theredend2000.advancedhunt.platform.PlatformAdapter;
import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Spigot18PlatformAdapter implements PlatformAdapter {

    private static final Map<String, Effect> EFFECTS = new HashMap<>();

    static {
        // Common mappings from modern particle names to 1.8 Effect names.
        // This is intentionally small; unknown names are ignored.
        register("VILLAGER_HAPPY", "HAPPY_VILLAGER");
        register("SMOKE", "SMOKE");
        register("SMOKE_NORMAL", "SMOKE");
        register("SPLASH", "SPLASH");
        register("WATER_SPLASH", "SPLASH");
        register("WATER_BUBBLE", "BUBBLE");
        register("WATER_DROP", "RAIN");
        register("EXPLOSION_NORMAL", "EXPLOSION");
        register("EXPLOSION_LARGE", "EXPLOSION");
        register("SPELL_WITCH", "WITCH_MAGIC");
        register("SPELL_INSTANT", "INSTANT_SPELL");
        register("SPELL_MOB", "MOBSPAWNER_FLAMES");
        register("REDSTONE", "COLOURED_DUST");
        register("SNOWBALL", "SNOWBALL_BREAK");
        register("SLIME", "SLIME");
        register("BLOCK_CRACK", "TILE_BREAK");
        register("TOTEM", "MOBSPAWNER_FLAMES");
    }

    private static void register(String key, String effectName) {
        try {
            EFFECTS.put(key, Effect.valueOf(effectName));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean isAir(Material material) {
        if (material == null) return true;
        return "AIR".equals(material.name());
    }

    @Override
    public void sendActionBar(Player player, String message) {
        if (player == null) return;
        try {
            PacketPlayOutChat packet = new PacketPlayOutChat(new ChatComponentText(message == null ? "" : message), (byte) 2);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void spawnParticle(Location location, String particleName, int count,
                              double offsetX, double offsetY, double offsetZ, double speed) {
        if (location == null) return;
        World world = location.getWorld();
        if (world == null) return;
        if (particleName == null || particleName.trim().isEmpty()) return;

        Effect effect = EFFECTS.get(particleName.trim().toUpperCase());
        if (effect == null) {
            // Best-effort: try direct Effect name.
            try {
                effect = Effect.valueOf(particleName.trim().toUpperCase());
            } catch (Throwable ignored) {
                return;
            }
        }

        try {
            // Use Spigot API directly (no reflection).
            world.spigot().playEffect(
                    location,
                    effect,
                    0,
                    0,
                    (float) offsetX,
                    (float) offsetY,
                    (float) offsetZ,
                    (float) speed,
                    count,
                    64
            );
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void spawnParticleForPlayer(org.bukkit.entity.Player player, Location location, String particleName, int count,
                                       double offsetX, double offsetY, double offsetZ, double speed) {
        // Per-player particles are not supported on 1.8 without packets.
        // Keep behavior consistent with previous implementation (no-op).
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
            if (type != null && "SKULL_ITEM".equals(type.name())) {
                if (item.getDurability() != (short) 3) {
                    item.setDurability((short) 3);
                }
            }
        } catch (Throwable ignored) {
        }
        return item;
    }
}
