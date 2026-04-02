package de.theredend2000.advancedhunt.util;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.Treasure;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.logging.Logger;

public class ItemUtils {

    private static Logger getLogger() {
        try {
            return JavaPlugin.getPlugin(Main.class).getLogger();
        } catch (IllegalStateException e) {
            return Logger.getLogger("AdvancedHunt");
        }
    }

    public static ItemStack createBaseItem(Treasure treasure) {
        if ("ITEMS_ADDER".equals(treasure.getMaterial())) {
            ItemStack item = ItemsAdderAdapter.getCustomItem(treasure.getBlockState());
            if (item != null) {
                return item;
            }
            getLogger().warning("ItemsAdder item '" + treasure.getBlockState() + "' not found, falling back to STONE");
            return XMaterial.STONE.parseItem();
        } else {
            ItemStack item = XMaterialHelper.getItemStack(treasure.getMaterial());
            if (item != null) {
                return item;
            }

            Optional<XMaterial> xMat = XMaterial.matchXMaterial(treasure.getMaterial());
            if (xMat.isPresent()) {
                getLogger().warning("Material '" + treasure.getMaterial() + "' parsed to null item, falling back to STONE");
            } else {
                getLogger().warning("Unknown material '" + treasure.getMaterial() + "', falling back to STONE");
            }
            return XMaterial.STONE.parseItem();
        }
    }

    public static void applyHeadNbt(ItemStack item, Treasure treasure) {
        if (treasure.getNbtData() != null && !treasure.getNbtData().isEmpty()) {
            try {
                NBT.modify(item, nbt -> {
                    ReadWriteNBT blockEntityTag = nbt.getOrCreateCompound("BlockEntityTag");
                    try {
                        blockEntityTag.mergeCompound(NBT.parseNBT(treasure.getNbtData()));
                    } catch (Exception ignored) {
                    }
                });
            } catch (Exception ignored) {
            }
        }
    }
}
