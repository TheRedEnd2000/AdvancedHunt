package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.PermissionManager.PermissionManager;
import de.theredend2000.advancedegghunt.util.enums.Permission;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class InventoryOpenEventListener implements Listener {

    @EventHandler
    public void onOpen(InventoryOpenEvent event){
        if(event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (event.getView().getTitle().equals("Egg Rewards") && !Main.getInstance().getPermissionManager().checkPermission(player, Permission.OpenRewards)) {
                player.kickPlayer("§4YOU ARE NOT ALLOWED IN THIS INVENTORY");
            }
        }
    }

}
