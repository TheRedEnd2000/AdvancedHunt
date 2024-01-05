package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggplacelist.EggPlaceMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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

    @EventHandler
    public void onInteract(PlayerInteractEvent event){
        Player player = event.getPlayer();
        if(event.getItem() != null) {
            if (event.getItem().getType().equals(Material.NETHER_STAR) && event.getItem().getItemMeta() != null) {
                if (event.getItem().getItemMeta().hasLocalizedName()) {
                    if (event.getItem().getItemMeta().getLocalizedName().equals("egghunt.eggs")) {
                        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_AIR))
                            new EggPlaceMenu(Main.getPlayerMenuUtility(player)).open();
                    }
                }
            }
        }
    }

}
