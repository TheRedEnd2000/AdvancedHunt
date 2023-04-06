package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.versions.VersionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractItemEvent implements Listener {

    public PlayerInteractItemEvent(){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event){
        Player player = event.getPlayer();
        if(event.getItemDrop().getItemStack().getType().equals(Material.PLAYER_HEAD)){
            if(event.getItemDrop().getItemStack().getItemMeta() != null){
                if(event.getItemDrop().getItemStack().getItemMeta().hasLocalizedName()){
                    if(event.getItemDrop().getItemStack().getItemMeta().getLocalizedName().equals("egghunt.finish")){
                        if(Main.getInstance().getPlaceEggsPlayers().contains(player)) {
                            event.getItemDrop().remove();
                            Bukkit.dispatchCommand(player, "egghunt placeEggs");
                        }
                    }
                }
            }
        }
    }

}
