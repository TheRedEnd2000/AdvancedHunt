package de.theredend2000.advancedhunt.util;

import com.cryptomorin.xseries.XMaterial;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBTCompoundList;
import org.bukkit.inventory.ItemStack;

public class HeadHelper {

    /**
     * Fast heuristic for determining whether a stored material name represents a head/skull.
     * This is important for legacy versions (1.8-1.12) where skulls are represented by
     * {@code SKULL}/{@code SKULL_ITEM} + durability, and XMaterial matching can otherwise
     * resolve to a non-player skull type (e.g. creeper).
     */
    public static boolean isHeadMaterialName(String materialName) {
        if (materialName == null) return false;
        String name = materialName.trim();
        if (name.isEmpty()) return false;
        name = name.toUpperCase(java.util.Locale.ROOT);

        if ("SKULL".equals(name) || "SKULL_ITEM".equals(name) || "LEGACY_SKULL".equals(name) || "LEGACY_SKULL_ITEM".equals(name)) {
            return true;
        }
        return name.endsWith("_HEAD")
                || name.endsWith("_WALL_HEAD")
                || name.endsWith("_SKULL")
                || name.endsWith("_WALL_SKULL");
    }

    public static boolean isPlayerHead(ItemStack item) {
        XMaterial material = XMaterial.matchXMaterial(item);
        return material == XMaterial.PLAYER_HEAD || material == XMaterial.PLAYER_WALL_HEAD;
    }

    public static String getTextureFromNbt(String nbtData) {
        if (nbtData == null || nbtData.isEmpty()) return null;

        try {
            ReadWriteNBT nbt = NBT.parseNBT(nbtData);
            ReadWriteNBT profile = getProfileCompound(nbt);

            if (profile != null) {
                return extractTextureFromProfile(profile);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String getProfileNameFromNbt(String nbtData) {
        if (nbtData == null || nbtData.isEmpty()) return null;

        try {
            ReadWriteNBT nbt = NBT.parseNBT(nbtData);
            ReadWriteNBT profile = getProfileCompound(nbt);

            if (profile != null) {
                // Try "name" field (most common)
                if (profile.hasTag("name")) {
                    return profile.getString("name");
                }
                // Try "Name" field (alternative)
                if (profile.hasTag("Name")) {
                    return profile.getString("Name");
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static ReadWriteNBT getProfileCompound(ReadWriteNBT nbt) {
        if (nbt.hasTag("Owner")) return nbt.getCompound("Owner");
        if (nbt.hasTag("SkullOwner")) return nbt.getCompound("SkullOwner");
        if (nbt.hasTag("profile")) return nbt.getCompound("profile");
        return null;
    }

    private static String extractTextureFromProfile(ReadWriteNBT profile) {
        // Try "Properties" (Capitalized)
        if (profile.hasTag("Properties")) {
            ReadWriteNBT props = profile.getCompound("Properties");
            if (props.hasTag("textures")) {
                return getTextureFromList(props.getCompoundList("textures"));
            }
        }
        
        // Try "properties" (Lowercase)
        if (profile.hasTag("properties")) {
            // Try as a list directly (1.20.5+ component format sometimes)
             try {
                 ReadWriteNBTCompoundList list = profile.getCompoundList("properties");
                 for (ReadWriteNBT entry : list) {
                     if (entry.hasTag("name") && "textures".equals(entry.getString("name"))) {
                         return entry.getString("value");
                     }
                 }
             } catch (Exception ignored) {}
             
             // Try as a compound
             try {
                 ReadWriteNBT props = profile.getCompound("properties");
                 if (props.hasTag("textures")) {
                     return getTextureFromList(props.getCompoundList("textures"));
                 }
             } catch (Exception ignored) {}
        }
        return null;
    }

    private static String getTextureFromList(ReadWriteNBTCompoundList list) {
        if (!list.isEmpty()) {
            ReadWriteNBT entry = list.get(0);
            if (entry.hasTag("Value")) return entry.getString("Value");
            if (entry.hasTag("value")) return entry.getString("value");
        }
        return null;
    }
}
