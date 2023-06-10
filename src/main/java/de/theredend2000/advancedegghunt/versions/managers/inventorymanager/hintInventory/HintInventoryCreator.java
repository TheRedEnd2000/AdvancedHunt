package de.theredend2000.advancedegghunt.versions.managers.inventorymanager.hintInventory;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;
import java.util.Random;

public class HintInventoryCreator implements Listener {

    private Player player;
    private Inventory inventory;
    private boolean active;
    private int currentSlot;

    public HintInventoryCreator(Player player,Inventory inventory,boolean active){
        Bukkit.getPluginManager().registerEvents(this,Main.getInstance());
        this.player = player;
        this.inventory = inventory;
        this.active = active;
        open(player);
    }

    private void open(Player player){
        if(player != null) {
            player.openInventory(inventory);
            startAnimating();
        }
    }

    private void startAnimating(){
        new BukkitRunnable() {
            @Override
            public void run() {
                if(active){
                    inventory.clear();
                    currentSlot = getRandomSlot();
                    inventory.setItem(currentSlot,new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayname("§aEnter").build());
                }else
                    cancel();
            }
        }.runTaskTimer(Main.getInstance(),60,20);
    }

    private int getRandomSlot(){
        return new Random().nextInt(inventory.getSize());
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event){
        if(event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() != null){
            if(event.getInventory().equals(inventory)){
                event.setCancelled(true);
                if(Objects.requireNonNull(event.getClickedInventory()).getItem(currentSlot) != null){
                    player.sendMessage("§aEnter ture");
                }
            }
        }
    }



}
