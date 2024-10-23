package de.theredend2000.advancedhunt.util;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

public class XMaterialHelper {
    /**
     * Gets an ItemStack from XMaterial, handling wall variants
     * @param material The XMaterial to convert
     * @return ItemStack of the material, including wall variants
     */
    public static ItemStack getItemStack(XMaterial material) {
        ItemStack item;

        // First try to get the item directly
        try {
            item = material.parseItem();
        } catch (IllegalArgumentException e) {
            item = null;
        }

        // If the item is null and might be a wall variant, try to get the base block
        if (item == null && material.name().contains("WALL")) {
            String baseName = material.name().replace("WALL_", "");
            try {
                XMaterial baseMaterial = XMaterial.valueOf(baseName);
                item = baseMaterial.parseItem();
            } catch (IllegalArgumentException e) {
                // Handle case where base material name is invalid
                return null;
            }
        }

        return item;
    }
}
