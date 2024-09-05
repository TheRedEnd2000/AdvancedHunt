package de.theredend2000.advancedegghunt.util;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableItemNBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
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

    public static String getSkullTexture(ItemStack item) {
        String fullTexture;
        String version = Bukkit.getBukkitVersion().split("-", 2)[0];

        if (VersionComparator.isGreaterThanOrEqual(version, "1.20.5")) {
            var components = NBT.itemStackToNBT(item).getOrCreateCompound("components");

            if (components == null) return null;
            final ReadableNBT skullOwnerCompound = components.getCompound("minecraft:profile");

            if (skullOwnerCompound == null) return null;
            ReadableNBTList<ReadWriteNBT> skullOwnerPropertiesCompound = skullOwnerCompound.getCompoundList("Properties");

            for (ReadWriteNBT property : skullOwnerPropertiesCompound) {
                if (Objects.equals(property.getString("name"), "textures") && property.getString("value") != null) {
                    return property.getString("value");
                }
            }
            return "";
        } else {
            fullTexture = NBT.get(item, nbt -> {
                final ReadableNBT skullOwnerCompound = nbt.getCompound("SkullOwner");

                if (skullOwnerCompound == null) return null;
                ReadableNBT skullOwnerPropertiesCompound = skullOwnerCompound.getCompound("Properties");

                if (skullOwnerPropertiesCompound == null) return null;
                ReadableNBTList<ReadWriteNBT> skullOwnerPropertiesTexturesCompound = skullOwnerPropertiesCompound.getCompoundList("textures");

                if (skullOwnerPropertiesTexturesCompound == null) return null;
                return skullOwnerPropertiesTexturesCompound.get(0).getString("Value");
            });
        }

        return fullTexture;
    }
}
