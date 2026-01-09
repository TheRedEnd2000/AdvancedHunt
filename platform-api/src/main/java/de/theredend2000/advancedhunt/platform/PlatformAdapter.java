package de.theredend2000.advancedhunt.platform;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public interface PlatformAdapter {

    boolean isAir(Material material);

    void sendActionBar(Player player, String message);

    void spawnParticle(Location location, String particleName, int count,
                       double offsetX, double offsetY, double offsetZ, double speed);

    void spawnParticleForPlayer(Player player, Location location, String particleName, int count,
                                double offsetX, double offsetY, double offsetZ, double speed);

    void applyHideTooltip(ItemMeta meta, boolean hide);

    void applyUnbreakable(ItemMeta meta, boolean unbreakable);

    void applyCustomModelData(ItemMeta meta, Integer customModelData);

    void applySkullOwner(ItemMeta meta, UUID ownerUuid);

    void applySkullOwner(ItemMeta meta, String ownerName);

    /**
     * Ensure the given item stack represents a player head in the current server version.
     * Implementations may replace the stack type/durability if needed.
     */
    ItemStack ensurePlayerHeadItem(ItemStack item);
}
