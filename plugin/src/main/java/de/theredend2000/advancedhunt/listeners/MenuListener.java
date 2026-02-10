package de.theredend2000.advancedhunt.listeners;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Menu;
import org.bukkit.Bukkit;
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

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MenuListener implements Listener {

    private final Main plugin;
    /** Players who closed a Menu inventory this tick. Cleared automatically next tick. */
    private final Set<UUID> closedThisTick = Collections.newSetFromMap(new ConcurrentHashMap<>());

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

        if (event.getClickedInventory() != null) {
            menu.performClick(event);
        }

        player.updateInventory();
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

            // Record that this player closed a menu this tick, so we can detect same-tick open
            UUID uuid = player.getUniqueId();
            closedThisTick.add(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> closedThisTick.remove(uuid));
            
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
            Menu menu = (Menu) event.getInventory().getHolder();
            Player player = (Player) event.getPlayer();

            // If the player closed a menu on this same tick, close the newly opened one
            if (closedThisTick.remove(player.getUniqueId())) {
                Bukkit.getScheduler().runTask(plugin, player::closeInventory);
                return;
            }

            menu.onOpen(event);
        }
    }
}
