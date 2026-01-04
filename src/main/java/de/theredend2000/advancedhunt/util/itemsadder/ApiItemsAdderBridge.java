package de.theredend2000.advancedhunt.util.itemsadder;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * ItemsAdder-backed implementation.
 *
 * IMPORTANT: This class references ItemsAdder API types and must only be loaded
 * when ItemsAdder is installed/enabled.
 */
public class ApiItemsAdderBridge implements ItemsAdderBridge {

    @Override
    public boolean isItemsAdderBlock(Block block) {
        if (block == null) return false;
        return CustomBlock.byAlreadyPlaced(block) != null;
    }

    @Override
    public String getCustomBlockId(Block block) {
        if (block == null) return null;
        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
        return customBlock != null ? customBlock.getNamespacedID() : null;
    }

    @Override
    public boolean isCustomBlockItem(ItemStack item) {
        if (item == null) return false;
        return CustomBlock.byItemStack(item) != null;
    }

    @Override
    public String getCustomBlockId(ItemStack item) {
        if (item == null) return null;
        CustomBlock customBlock = CustomBlock.byItemStack(item);
        return customBlock != null ? customBlock.getNamespacedID() : null;
    }

    @Override
    public boolean isCustomFurniture(ItemStack item) {
        return getCustomFurnitureId(item) != null;
    }

    @Override
    public String getCustomFurnitureId(ItemStack item) {
        if (item == null) return null;
        CustomStack stack = CustomStack.byItemStack(item);
        if (stack == null) return null;

        String id = stack.getNamespacedID();
        if (id == null) return null;

        Set<String> furnitureIds = CustomFurniture.getNamespacedIdsInRegistry();
        return furnitureIds != null && furnitureIds.contains(id) ? id : null;
    }

    @Override
    public String getCustomFurnitureId(Entity entity) {
        if (entity == null) return null;
        CustomFurniture furniture = CustomFurniture.byAlreadySpawned(entity);
        return furniture != null ? furniture.getNamespacedID() : null;
    }

    @Override
    public ItemStack getCustomItem(String namespacedId) {
        if (namespacedId == null) return null;

        CustomStack stack = CustomStack.getInstance(namespacedId);
        if (stack != null) {
            return stack.getItemStack();
        }

        CustomBlock block = CustomBlock.getInstance(namespacedId);
        if (block != null) {
            return block.getItemStack();
        }

        return null;
    }
}
