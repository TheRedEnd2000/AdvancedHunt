package de.theredend2000.advancedhunt.util;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.inventory.ItemStack;

public class XMaterialHelper {
    /**
     * Gets an ItemStack from XMaterial, handling wall variants
     * @param material The XMaterial to convert
     * @return ItemStack of the material, including wall variants
     */
    public static ItemStack getItemStack(XMaterial material) {
        if (material == null) {
            return null;
        }

        ItemStack item;

        // Handle wall variants (e.g., COBBLESTONE_WALL -> COBBLESTONE)
        if (material.name().endsWith("_WALL")) {
            String baseName = material.name().replaceFirst("_WALL$", "");
            try {
                XMaterial baseMaterial = XMaterial.valueOf(baseName);
                item = baseMaterial.parseItem();
                if (item != null) {
                    return item;
                }
            } catch (IllegalArgumentException e) {
                // Base material doesn't exist, fall through
            }
        }

        // Try to parse material directly
        item = material.parseItem();
        return item;
    }
}