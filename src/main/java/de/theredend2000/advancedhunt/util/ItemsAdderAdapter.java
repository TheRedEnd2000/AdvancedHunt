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

    private static Object tryGetInstance(Class<?> clazz, String id) {
        if (clazz == null || id == null) return null;
        try {
            Method getInstance = clazz.getMethod("getInstance", String.class);
            return getInstance.invoke(null, id);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ItemStack tryGetItemStack(Object instance) {
        if (instance == null) return null;
        try {
            Method getItemStack = instance.getClass().getMethod("getItemStack");
            return (ItemStack) getItemStack.invoke(instance);
        } catch (Exception ignored) {
            return null;
        }
    }

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

        // Prefer CustomStack (the usual way to obtain an inventory item)
        ItemStack item = tryGetItemStack(tryGetInstance(customStackClass, id));
        if (item != null) return item;

        // Fallback: some IDs are resolved via CustomBlock/CustomFurniture instances
        item = tryGetItemStack(tryGetInstance(customBlockClass, id));
        if (item != null) return item;

        item = tryGetItemStack(tryGetInstance(customFurnitureClass, id));
        return item;
    }
}
