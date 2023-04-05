package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ConfigLocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

public class InventoryClickEventListener implements Listener {

    public InventoryClickEventListener(){
        Bukkit.getPluginManager().registerEvents(this,Main.getInstance());
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event){
        if(event.getWhoClicked() instanceof Player){
            Player player = (Player) event.getWhoClicked();
            if(event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() != null){
                if(event.getView().getTitle().equals("Eggs list")) {
                    event.setCancelled(true);
                    if (Main.getInstance().eggs.contains("Eggs.")) {
                        for (String keys : Main.getInstance().eggs.getConfigurationSection("Eggs.").getKeys(false)) {
                            if (event.getCurrentItem().getItemMeta().getLocalizedName().equals(keys)) {
                                ConfigLocationUtil location = new ConfigLocationUtil(Main.getInstance(), "Eggs." + keys);
                                if (location.loadBlockLocation() != null)
                                    player.teleport(location.loadLocation());
                                player.closeInventory();
                                player.sendMessage(Main.getInstance().getMessage("TeleportedToEggMessage").replaceAll("%ID%", keys));
                            }
                        }
                    }
                }
                if(Main.getInstance().getPlaceEggsPlayers().contains(player)){
                    if(event.getInventory().getViewers().contains(player)){
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

}
