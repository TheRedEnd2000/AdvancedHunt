package de.theredend2000.advancedhunt.util;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public class ItemsAdderAdapter {

    private static boolean enabled = false;
    private static Class<?> customBlockClass;
    private static Class<?> customFurnitureClass;
    private static Class<?> customStackClass;

    static {
        try {
            customBlockClass = Class.forName("dev.lone.itemsadder.api.CustomBlock");
            customFurnitureClass = Class.forName("dev.lone.itemsadder.api.CustomFurniture");
            customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            enabled = true;
        } catch (ClassNotFoundException e) {
            enabled = false;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isItemsAdderBlock(Block block) {
        if (!enabled) return false;
        try {
            Method method = customBlockClass.getMethod("byAlreadyPlaced", Block.class);
            Object result = method.invoke(null, block);
            return result != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getCustomBlockId(Block block) {
        if (!enabled) return null;
        try {
            Method byAlreadyPlaced = customBlockClass.getMethod("byAlreadyPlaced", Block.class);
            Object customBlock = byAlreadyPlaced.invoke(null, block);
            if (customBlock != null) {
                Method getNamespacedID = customBlockClass.getMethod("getNamespacedID");
                return (String) getNamespacedID.invoke(customBlock);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isCustomBlockItem(ItemStack item) {
        if (!enabled || item == null) return false;
        try {
            Method byItemStack = customBlockClass.getMethod("byItemStack", ItemStack.class);
            Object result = byItemStack.invoke(null, item);
            return result != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getCustomBlockId(ItemStack item) {
        if (!enabled || item == null) return null;
        try {
            Method byItemStack = customBlockClass.getMethod("byItemStack", ItemStack.class);
            Object customBlock = byItemStack.invoke(null, item);
            if (customBlock != null) {
                Method getNamespacedID = customBlockClass.getMethod("getNamespacedID");
                return (String) getNamespacedID.invoke(customBlock);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public static boolean isCustomFurniture(ItemStack item) {
        if (!enabled || item == null) return false;
        try {
            Method byItemStack = customFurnitureClass.getMethod("byItemStack", ItemStack.class);
            Object result = byItemStack.invoke(null, item);
            return result != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isItemsAdderFurniture(Entity entity) {
        return getCustomFurnitureId(entity) != null;
    }

    public static String getCustomFurnitureId(Entity entity) {
        if (!enabled || entity == null) return null;
        try {
            Method byAlreadySpawned = customFurnitureClass.getMethod("byAlreadySpawned", Entity.class);
            Object customFurniture = byAlreadySpawned.invoke(null, entity);
            if (customFurniture != null) {
                Method getNamespacedID = customFurnitureClass.getMethod("getNamespacedID");
                return (String) getNamespacedID.invoke(customFurniture);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public static String getCustomFurnitureId(ItemStack item) {
        if (!enabled || item == null) return null;
        try {
            Method byItemStack = customFurnitureClass.getMethod("byItemStack", ItemStack.class);
            Object customFurniture = byItemStack.invoke(null, item);
            if (customFurniture != null) {
                Method getNamespacedID = customFurnitureClass.getMethod("getNamespacedID");
                return (String) getNamespacedID.invoke(customFurniture);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ItemStack getCustomItem(String id) {
        if (!enabled || id == null) return null;
        try {
            Method getInstance = customStackClass.getMethod("getInstance", String.class);
            Object customStack = getInstance.invoke(null, id);
            if (customStack != null) {
                Method getItemStack = customStackClass.getMethod("getItemStack");
                return (ItemStack) getItemStack.invoke(customStack);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
