package de.theredend2000.advancedegghunt.versions.managers.inventorymanager;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public interface InventoryManager {

    public void createEggsListInventory(Player player);
    public void setChests(Inventory inventory);

}
