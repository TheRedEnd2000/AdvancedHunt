package de.theredend2000.advancedhunt.util.itemsadder;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

/**
 * Lightweight abstraction over ItemsAdder APIs.
 *
 * This interface must not reference ItemsAdder classes so the plugin can run
 * without ItemsAdder installed.
 */
public interface ItemsAdderBridge {

    boolean isItemsAdderBlock(Block block);

    String getCustomBlockId(Block block);

    boolean isCustomBlockItem(ItemStack item);

    String getCustomBlockId(ItemStack item);

    boolean isCustomFurniture(ItemStack item);

    String getCustomFurnitureId(ItemStack item);

    String getCustomFurnitureId(Entity entity);

    ItemStack getCustomItem(String namespacedId);

    BlockData getCustomBlockData(String namespacedId);

    boolean isCustomFurnitureId(String namespacedId);

    boolean placeCustomBlock(String namespacedId, Location location);

    boolean removeCustomBlock(Location location);

    boolean spawnCustomFurniture(String namespacedId, Location location);

    boolean removeCustomFurniture(Location location);
}
