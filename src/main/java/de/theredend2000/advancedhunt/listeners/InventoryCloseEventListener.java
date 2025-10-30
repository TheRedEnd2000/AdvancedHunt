package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.inventorymanager.HintMenu;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.IInventoryMenu;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.InventoryMenu;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public class InventoryCloseEventListener implements Listener {

    public InventoryCloseEventListener(){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event){
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof IInventoryMenu) {
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), player::updateInventory, 4L);
        }

        if (HintMenu.hintMenuInstances.containsKey(player.getUniqueId())) {
            HintMenu.hintMenuInstances.get(player.getUniqueId()).cancelHintMenu();
            player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.EGG_HINT_CANCELLED));
        }
    }
}
