package de.theredend2000.advancedhunt.managers.inventorymanager.common;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public abstract class InventoryButton extends ItemStack {
    public abstract void onClick(ClickType clickType, Player player);
}
