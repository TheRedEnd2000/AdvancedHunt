package de.theredend2000.advancedHunt.listeners;

import de.theredend2000.advancedHunt.Main;
import de.theredend2000.advancedHunt.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class MenuListener implements Listener {

    private final Main plugin;

    public MenuListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory topInventory = event.getView().getTopInventory();

        if (!(topInventory.getHolder() instanceof Menu)) {
            return;
        }

        Menu menu = (Menu) topInventory.getHolder();

        event.setCancelled(true);

        if (event.getClickedInventory() != null) {
            menu.performClick(event);
        }

        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory topInventory = event.getView().getTopInventory();

        if (!(topInventory.getHolder() instanceof Menu)) return;

        // 1. CHECK AFFECTED SLOTS
        Set<Integer> rawSlots = event.getRawSlots();
        int topSize = topInventory.getSize();

        for (int slot : rawSlots) {
            if (slot < topSize) { // Slots 0 to size-1 are the top inventory
                event.setCancelled(true);
                ((Player) event.getWhoClicked()).updateInventory();
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();

        if (inventory.getHolder() instanceof Menu) {
            Menu menu = (Menu) inventory.getHolder();
            menu.onClose(event);
            
            // 1. CURSOR SMUGGLING PREVENTION
            ItemStack cursorItem = player.getItemOnCursor();
            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                player.setItemOnCursor(null);
                for (ItemStack leftover : player.getInventory().addItem(cursorItem).values()) {
                    player.getWorld().dropItem(player.getLocation(), leftover);
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.isCancelled()) return; 
        
        if (event.getInventory().getHolder() instanceof Menu) {
            ((Menu) event.getInventory().getHolder()).onOpen(event);
        }
    }
}
