package de.theredend2000.advancedhunt.managers.inventorymanager.common;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public interface IInventoryMenu extends InventoryHolder {
    public void handleMenu(InventoryClickEvent event);
}
