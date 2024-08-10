package de.theredend2000.advancedegghunt.util;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableItemNBT;
import org.bukkit.inventory.ItemStack;

import java.util.function.Function;

import static de.theredend2000.advancedegghunt.util.Constants.CustomIdKey;

public class ItemHelper {
    public static ItemStack setCustomId(ItemStack item, String id) {
        NBT.modify(item, nbt -> {
            nbt.setString(CustomIdKey, id);
        });
        return item;
    }

    public static boolean hasItemId(ItemStack item) {
        return NBT.get(item, (Function<ReadableItemNBT, Boolean>) nbt -> nbt.hasTag(CustomIdKey));
    }

    public static String getItemId(ItemStack item) {
        return NBT.get(item, (Function<ReadableItemNBT, String>) nbt -> nbt.getString(CustomIdKey));
    }
}
