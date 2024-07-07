package de.theredend2000.advancedegghunt.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static de.theredend2000.advancedegghunt.util.Constants.CustomIdKey;

public class ItemHelper {
    public static ItemStack setCustomId(ItemStack item, String id){
        var itemMeta = item.getItemMeta();

        if (itemMeta != null) {
            itemMeta.getPersistentDataContainer().set(CustomIdKey, PersistentDataType.STRING, id);
        }

        item.setItemMeta(itemMeta);
        return item;
    }

    public static boolean hasItemId(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            if (container.has(CustomIdKey, PersistentDataType.STRING)) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasItemId(ItemMeta meta) {
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (container.has(CustomIdKey, PersistentDataType.STRING)) {
            return true;
        }

        return false;
    }

    public static String getItemId(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            if (container.has(CustomIdKey, PersistentDataType.STRING)) {
                return container.get(CustomIdKey, PersistentDataType.STRING);
            }
        }
        return null; // or a default value
    }


    public static String getItemId(ItemMeta meta) {
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (container.has(CustomIdKey, PersistentDataType.STRING)) {
            return container.get(CustomIdKey, PersistentDataType.STRING);
        }
        return null; // or a default value
    }
}
