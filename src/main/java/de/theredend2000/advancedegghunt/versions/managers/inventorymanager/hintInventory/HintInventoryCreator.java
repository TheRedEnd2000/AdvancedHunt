package de.theredend2000.advancedegghunt.versions.managers.inventorymanager.hintInventory;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ConfigLocationUtil;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.versions.VersionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class HintInventoryCreator implements Listener {

    private Player player;
    private Inventory inventory;
    private boolean active;
    private int currentSlot;
    private int currentCount;
    private boolean clickedRight;

    public HintInventoryCreator(Player player,Inventory inventory,boolean active){
        Bukkit.getPluginManager().registerEvents(this,Main.getInstance());
        this.player = player;
        this.inventory = inventory;
        this.active = active;
        this.clickedRight = true;
        this.currentCount = 0;
        open(player);
    }

    private void open(Player player){
        if(player != null) {
            player.openInventory(inventory);
            for (int i = 0; i<inventory.getSize();i++){inventory.setItem(i, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayname("§c").build());}
            startAnimating();
            Main.getInstance().getCooldownManager().setCooldown(player);
        }
    }

    private void startAnimating(){
        new BukkitRunnable() {
            @Override
            public void run() {
                if(active){
                    if(!clickedRight)
                        fail(player);
                    inventory.clear();
                    currentSlot = getRandomSlot();
                    for (int i = 0; i<inventory.getSize();i++){inventory.setItem(i, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayname("§c").build());}
                    inventory.setItem(currentSlot,new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayname("§aConfirm").setLore("§6"+(currentCount+1)+"§7/§6"+Main.getInstance().getConfig().getInt("Settings.HintCount")).build());
                    clickedRight = false;
                }else
                    cancel();
            }
        }.runTaskTimer(Main.getInstance(),40,Main.getInstance().getConfig().getInt("Settings.HintUpdateTime"));
    }

    private int getRandomSlot(){
        return new Random().nextInt(inventory.getSize());
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event){
        if(event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() != null){
            if(event.getInventory().equals(inventory)){
                event.setCancelled(true);
                if(event.getSlot() == currentSlot){
                    currentCount++;
                    player.playSound(player.getLocation() ,VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                    if(currentCount == Main.getInstance().getConfig().getInt("Settings.HintCount")){
                        player.closeInventory();
                        active = false;
                        player.playSound(player.getLocation() ,VersionManager.getSoundManager().playAllEggsFound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                        getReward(player);
                    }
                    clickedRight = true;
                }else{
                    fail(player);
                }
            }
        }
    }

    public void getReward(Player player){
        Main plugin = Main.getInstance();
        if(VersionManager.getEggManager().containsPlayer(player.getName())){
            if(!VersionManager.getEggManager().checkFoundAll(player)){
                int number = VersionManager.getEggManager().getRandomNotFoundEgg(player);
                ConfigLocationUtil location = new ConfigLocationUtil(plugin, "Eggs." + number + ".");
                if (location.loadBlockLocation() != null) {
                    int random = new Random().nextInt(2);
                    player.sendMessage(Main.getInstance().getMessage("EggHintFound").replaceAll("%X%", random == 1 ? String.valueOf(location.loadLocation().getBlockX()) : "§k1").replaceAll("%Y%", String.valueOf(location.loadLocation().getBlockY())).replaceAll("%Z%", random == 0 ? String.valueOf(location.loadLocation().getBlockZ()) : "§k1"));
                }
            }
        }
    }

    public void fail(Player player){
        player.closeInventory();
        player.playSound(player.getLocation() ,VersionManager.getSoundManager().playErrorSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
        active = false;
    }



}
