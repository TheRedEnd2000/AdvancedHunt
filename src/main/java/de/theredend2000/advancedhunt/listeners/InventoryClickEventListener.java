package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.IInventoryMenu;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.IInventoryMenuOpen;
import de.theredend2000.advancedhunt.util.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class InventoryClickEventListener implements Listener {

    private MessageManager messageManager;
    private Main plugin;

    public InventoryClickEventListener(){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
        messageManager = Main.getInstance().getMessageManager();
        plugin = Main.getInstance();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if the click type involves the number keys (hotbar buttons 1-9)
        if (event.getClick().isKeyboardClick()) {
            InventoryHolder holder = event.getInventory().getHolder();
            if (holder instanceof IInventoryMenu) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event){
        if (!(event.getWhoClicked() instanceof Player) ||
                event.getCurrentItem() == null ||
                event.getCurrentItem().getItemMeta() == null) {
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof IInventoryMenu) {
            event.setCancelled(true);
            if(event.getAction().equals(InventoryAction.HOTBAR_MOVE_AND_READD)) event.setCancelled(true);
            if (event.getCurrentItem() == null) {
                return;
            }
            IInventoryMenu menu = (IInventoryMenu) holder;
            menu.handleMenu(event);
        }
    }

    @EventHandler
    public void onOpenInventory(InventoryOpenEvent event){
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof IInventoryMenuOpen) {
            IInventoryMenuOpen menu = (IInventoryMenuOpen) holder;
            menu.onOpen(event);
        }
    }
}
