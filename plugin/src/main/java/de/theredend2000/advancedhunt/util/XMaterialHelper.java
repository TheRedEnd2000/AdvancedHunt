package de.theredend2000.advancedhunt.util;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

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

    /**
     * Resolves a stored material name into a displayable item stack.
     * Head/skull aliases are normalized to a player head so texture application can run later.
     */
    public static ItemStack getItemStack(String materialName) {
        if (materialName == null || materialName.trim().isEmpty()) {
            return null;
        }

        if (HeadHelper.isHeadMaterialName(materialName)) {
            return XMaterial.PLAYER_HEAD.parseItem();
        }

        Material material = Material.matchMaterial(materialName);
        if (material != null) {
            return getItemStack(XMaterial.matchXMaterial(material));
        }

        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(materialName);
        if (xMaterial.isPresent()) {
            return getItemStack(xMaterial.get());
        }

        return null;
    }
}