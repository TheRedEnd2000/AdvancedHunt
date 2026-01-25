package de.theredend2000.advancedhunt.util;

import de.theredend2000.advancedhunt.util.itemsadder.ItemsAdderBridge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class ItemsAdderAdapter {

    private static volatile ItemsAdderBridge bridge;

    private static ItemsAdderBridge getBridge() {
        ItemsAdderBridge local = bridge;
        if (local != null) {
            return local;
        }

        synchronized (ItemsAdderAdapter.class) {
            if (bridge != null) {
                return bridge;
            }
            try {
                // Avoid loading any ItemsAdder API types unless the plugin exists AND is enabled.
                Plugin itemsAdderPlugin = Bukkit.getPluginManager().getPlugin("ItemsAdder");
                if (itemsAdderPlugin == null || !itemsAdderPlugin.isEnabled()) {
                    return null;
                }

                Class<?> impl = Class.forName(
                        "de.theredend2000.advancedhunt.util.itemsadder.ApiItemsAdderBridge",
                        true,
                        ItemsAdderAdapter.class.getClassLoader()
                );
                bridge = (ItemsAdderBridge) impl.getDeclaredConstructor().newInstance();
                return bridge;
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    public static boolean isEnabled() {
        return getBridge() != null;
    }

    public static boolean isItemsAdderBlock(Block block) {
        ItemsAdderBridge bridge = getBridge();
        if (bridge == null) return false;
        return bridge.isItemsAdderBlock(block);
    }

    public static String getCustomBlockId(Block block) {
        ItemsAdderBridge bridge = getBridge();
        if (bridge == null) return null;
        return bridge.getCustomBlockId(block);
    }

    public static boolean isCustomBlockItem(ItemStack item) {
        ItemsAdderBridge bridge = getBridge();
        if (bridge == null) return false;
        return bridge.isCustomBlockItem(item);
    }

    public static String getCustomBlockId(ItemStack item) {
        ItemsAdderBridge bridge = getBridge();
        if (bridge == null) return null;
        return bridge.getCustomBlockId(item);
    }

    public static boolean isCustomFurniture(ItemStack item) {
        ItemsAdderBridge bridge = getBridge();
        if (bridge == null) return false;
        return bridge.isCustomFurniture(item);
    }

    public static boolean isItemsAdderFurniture(Entity entity) {
        return getCustomFurnitureId(entity) != null;
    }

    public static String getCustomFurnitureId(Entity entity) {
        ItemsAdderBridge bridge = getBridge();
        if (bridge == null) return null;
        return bridge.getCustomFurnitureId(entity);
    }

    public static String getCustomFurnitureId(ItemStack item) {
        ItemsAdderBridge bridge = getBridge();
        if (bridge == null) return null;
        return bridge.getCustomFurnitureId(item);
    }

    public static ItemStack getCustomItem(String id) {
        ItemsAdderBridge bridge = getBridge();
        if (bridge == null) return null;
        return bridge.getCustomItem(id);
    }

    public static BlockData getCustomBlockData(String namespacedId) {
        ItemsAdderBridge bridge = getBridge();
        if (bridge == null) return null;
        return bridge.getCustomBlockData(namespacedId);
    }

    public static boolean isCustomFurnitureId(String namespacedId) {
        ItemsAdderBridge bridge = getBridge();
        if (bridge == null) return false;
        return bridge.isCustomFurnitureId(namespacedId);
    }

    public static boolean placeCustomBlock(String namespacedId, Location location) {
        ItemsAdderBridge bridge = getBridge();
        if (bridge == null) return false;
        return bridge.placeCustomBlock(namespacedId, location);
    }

    public static boolean removeCustomBlock(Location location) {
        ItemsAdderBridge bridge = getBridge();
        if (bridge == null) return false;
        return bridge.removeCustomBlock(location);
    }

    public static boolean spawnCustomFurniture(String namespacedId, Location location) {
        ItemsAdderBridge bridge = getBridge();
        if (bridge == null) return false;
        return bridge.spawnCustomFurniture(namespacedId, location);
    }

    public static boolean removeCustomFurniture(Location location) {
        ItemsAdderBridge bridge = getBridge();
        if (bridge == null) return false;
        return bridge.removeCustomFurniture(location);
    }
}
