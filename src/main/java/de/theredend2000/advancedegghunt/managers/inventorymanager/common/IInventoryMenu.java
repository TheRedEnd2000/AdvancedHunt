package de.theredend2000.advancedegghunt.managers.inventorymanager.common;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public interface IInventoryMenu extends InventoryHolder {
    public void handleMenu(InventoryClickEvent event);
}
