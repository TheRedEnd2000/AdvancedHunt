package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.inventorymanager.EggPlaceMenu;
import de.theredend2000.advancedhunt.util.ItemHelper;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractItemEvent implements Listener {

    public PlayerInteractItemEvent(){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event){
        Player player = event.getPlayer();
        if (event.getItemDrop().getItemStack().getItemMeta() == null ||
                !ItemHelper.hasItemId(event.getItemDrop().getItemStack())) {
            return;
        }

        switch (ItemHelper.getItemId(event.getItemDrop().getItemStack())) {
            case "egghunt.finish":
                if(Main.getInstance().getPlacePlayers().contains(player)) {
                    event.getItemDrop().remove();
                    Main.getInstance().getEggManager().finishEggPlacing(player);
                    Main.getInstance().getPlacePlayers().remove(player);
                    Main.getInstance().getMessageManager().sendMessage(player, MessageKey.LEAVE_PLACEMODE);
                }
                return;
            case "egghunt.eggs":
                event.setCancelled(true);
                return;
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event){
        Player player = event.getPlayer();
        if (event.getItem() == null ||
                !event.getItem().getType().equals(Material.NETHER_STAR) ||
                event.getItem().getItemMeta() == null ||
                !ItemHelper.hasItemId(event.getItem()) ||
                !ItemHelper.getItemId(event.getItem()).equals("egghunt.eggs") ||
                !event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && !event.getAction().equals(Action.RIGHT_CLICK_AIR)) {
            return;
        }
        new EggPlaceMenu(Main.getPlayerMenuUtility(player)).open();
    }
    @EventHandler
    public void closeMenu(InventoryCloseEvent event) {
        /*Bukkit.broadcastMessage("closed");
        Player player = (Player) event.getPlayer();
        Bukkit.broadcastMessage("closed 2");
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), player::updateInventory, 4L);*/
    }
}
