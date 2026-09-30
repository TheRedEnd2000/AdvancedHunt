package de.theredend2000.advancedhunt.util.itemsadder;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
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

    @Override
    public BlockData getCustomBlockData(String namespacedId) {
        if (namespacedId == null || namespacedId.isEmpty()) return null;

        try {
            CustomBlock block = CustomBlock.getInstance(namespacedId);
            if (block != null) {
                try {
                    return block.getBaseBlockData();
                } catch (Throwable ignored) {
                }
            }

            try {
                return CustomBlock.getBaseBlockData(namespacedId);
            } catch (Throwable ignored) {
                return null;
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public boolean isCustomFurnitureId(String namespacedId) {
        if (namespacedId == null || namespacedId.isEmpty()) return false;
        try {
            Set<String> ids = CustomFurniture.getNamespacedIdsInRegistry();
            return ids != null && ids.contains(namespacedId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean hasCustomFurnitureAt(String namespacedId, Location location) {
        return findCustomFurnitureAt(namespacedId, location) != null;
    }

    @Override
    public boolean placeCustomBlock(String namespacedId, Location location) {
        if (namespacedId == null || location == null) return false;
        try {
            return CustomBlock.place(namespacedId, location) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean removeCustomBlock(Location location) {
        if (location == null) return false;
        try {
            return CustomBlock.remove(location);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean spawnCustomFurniture(String namespacedId, Location location) {
        if (namespacedId == null || location == null) return false;
        if (hasCustomFurnitureAt(namespacedId, location)) return true;

        try {
            CustomFurniture furniture = CustomFurniture.spawnPreciseNonSolid(namespacedId, location);
            if (furniture != null) {
                return true;
            }
        } catch (Throwable ignored) {
        }

        if (hasCustomFurnitureAt(namespacedId, location)) return true;

        try {
            if (location.getBlock() == null) return false;
            CustomFurniture furniture = CustomFurniture.spawn(namespacedId, location.getBlock());
            return furniture != null || hasCustomFurnitureAt(namespacedId, location);
        } catch (Throwable ignored) {
            return hasCustomFurnitureAt(namespacedId, location);
        }
    }

    @Override
    public boolean removeCustomFurniture(String namespacedId, Location location) {
        if (namespacedId == null || location == null) return false;
        CustomFurniture furniture = findCustomFurnitureAt(namespacedId, location);
        if (furniture == null) return false;

        try {
            furniture.remove(false);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private CustomFurniture findCustomFurnitureAt(String namespacedId, Location location) {
        if (namespacedId == null || namespacedId.isEmpty() || location == null || location.getWorld() == null) {
            return null;
        }

        CustomFurniture furniture = CustomFurniture.byAlreadySpawned(location.getBlock());
        if (matchesFurniture(furniture, namespacedId)) return furniture;

        for (Entity entity : location.getWorld().getNearbyEntities(location, 1.0, 1.0, 1.0)) {
            Location entityLocation = entity.getLocation();
            if (!isSameBlock(location, entityLocation)) continue;

            furniture = CustomFurniture.byAlreadySpawned(entity);
            if (matchesFurniture(furniture, namespacedId)) return furniture;
        }

        return null;
    }

    private boolean matchesFurniture(CustomFurniture furniture, String namespacedId) {
        return furniture != null && namespacedId.equals(furniture.getNamespacedID());
    }

    private boolean isSameBlock(Location first, Location second) {
        return first != null && second != null
                && first.getWorld() != null && first.getWorld().equals(second.getWorld())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }
}
