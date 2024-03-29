package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.HintMenu;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

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

        if (HintMenu.hintMenuInstances.containsKey(player.getUniqueId())) {
            HintMenu.hintMenuInstances.get(player.getUniqueId()).cancelHintMenu();
            player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.EGG_HINT_CANCELLED));
        }
    }

}
