package de.theredend2000.advancedhunt.util;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.model.Treasure;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class ItemUtils {

    public static ItemStack createBaseItem(Treasure treasure) {
        if ("ITEMS_ADDER".equals(treasure.getMaterial())) {
            ItemStack item = ItemsAdderAdapter.getCustomItem(treasure.getBlockState());
            return item != null ? item : XMaterial.STONE.parseItem();
        } else {
            Optional<XMaterial> xMat = XMaterial.matchXMaterial(treasure.getMaterial());
            if (xMat.isPresent()) {
                ItemStack item = xMat.get().parseItem();
                if (item != null) return item;
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
