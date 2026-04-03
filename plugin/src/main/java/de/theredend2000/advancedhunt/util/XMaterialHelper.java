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
     * Head/skull aliases are normalized to the correct display item, while player heads
     * can still be upgraded later when texture or owner data is available.
     */
    public static ItemStack getItemStack(String materialName) {
        return getItemStack(materialName, null);
    }

    public static ItemStack getItemStack(String materialName, String blockState) {
        if (materialName == null || materialName.trim().isEmpty()) {
            return null;
        }

        String headMaterialName = HeadHelper.resolveHeadDisplayMaterialName(materialName, blockState);
        if (headMaterialName != null) {
            Optional<XMaterial> resolvedHeadMaterial = XMaterial.matchXMaterial(headMaterialName);
            if (resolvedHeadMaterial.isPresent()) {
                ItemStack item = getItemStack(resolvedHeadMaterial.get());
                if (item != null) {
                    return item;
                }
            }
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