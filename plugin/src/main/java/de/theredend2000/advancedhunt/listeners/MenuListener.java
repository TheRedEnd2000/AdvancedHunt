package de.theredend2000.advancedhunt.listeners;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Button;
import de.theredend2000.advancedhunt.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MenuListener implements Listener {

    private final Main plugin;

    public MenuListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        // 1.8 compatibility: avoid calling methods on InventoryView (API differs across versions).
        Inventory topInventory = event.getInventory();

        if (!(topInventory.getHolder() instanceof Menu)) {
            return;
        }

        Menu menu = (Menu) topInventory.getHolder();

        event.setCancelled(true);

        // ESCAPE + SHIFT-CLICK EXPLOIT PREVENTION
        // Block all clicks if the menu is in the process of closing
        if (menu.isClosing()) {
            player.updateInventory();
            return;
        }

        if (event.getClickedInventory() != null) {
            menu.performClick(event);
        }

        if (event.isCancelled()) {
            player.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        // 1.8 compatibility: avoid calling methods on InventoryView (API differs across versions).
        Inventory topInventory = event.getInventory();

        if (!(topInventory.getHolder() instanceof Menu)) return;

        Menu menu = (Menu) topInventory.getHolder();

        // Block all drags if the menu is in the process of closing
        if (menu.isClosing()) {
            event.setCancelled(true);
            player.updateInventory();
            return;
        }

        menu.handleDrag(event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();

        if (inventory.getHolder() instanceof Menu) {
            Menu menu = (Menu) inventory.getHolder();
            // ESCAPE + SHIFT-CLICK EXPLOIT PREVENTION
            // Mark menu as closing immediately to block any pending click/drag events
            menu.markClosing();
            menu.onClose(event);
            
            // 1. CURSOR SMUGGLING PREVENTION
            ItemStack cursorItem = player.getItemOnCursor();
            if (cursorItem != null && cursorItem.getType() != XMaterial.AIR.get()) {
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up the static click-cooldown map so it does not grow unboundedly.
        Button.removeClickCooldown(event.getPlayer().getUniqueId());
    }
}
