package de.theredend2000.advancedegghunt.managers.inventorymanager.hintInventory;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.soundmanager.SoundManager;
import de.theredend2000.advancedegghunt.util.ConfigLocationUtil;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Array;
import java.util.*;

public class HintInventoryCreator implements Listener {

    private Player player;
    private Inventory inventory;
    private boolean active;
    private int currentSlot;
    private int currentCount;
    private boolean clickedRight;
    private int lastClicked;

    public HintInventoryCreator(Player player,Inventory inventory,boolean active){
        Bukkit.getPluginManager().registerEvents(this,Main.getInstance());
        this.player = player;
        this.inventory = inventory;
        this.active = active;
        this.clickedRight = true;
        this.currentCount = 0;
        this.lastClicked = 100;
        open(player);
    }

    private void open(Player player){
        if(player != null) {
            player.openInventory(inventory);
            for (int i = 0; i<inventory.getSize();i++){inventory.setItem(i, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE).setDisplayname("§c").build());}
            startAnimating();
            Main.getInstance().getCooldownManager().setCooldown(player);
        }
    }

    private void startAnimating(){
        new BukkitRunnable() {
            @Override
            public void run() {
                if(active){
                    if(!clickedRight) {
                        fail(player);
                        player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.CLICKED_SAME));
                    }
                    inventory.clear();
                    currentSlot = getRandomSlot();
                    for (int i = 0; i<inventory.getSize();i++){inventory.setItem(i, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE).setDisplayname("§c").build());}
                    inventory.setItem(currentSlot,new ItemBuilder(XMaterial.LIME_STAINED_GLASS_PANE).setDisplayname("§aConfirm").setLore("§6"+(currentCount+1)+"§7/§6"+Main.getInstance().getConfig().getInt("Settings.HintCount")).build());
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
        SoundManager soundManager = Main.getInstance().getSoundManager();
        if(event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() != null){
            if(event.getInventory().equals(inventory)){
                event.setCancelled(true);
                if(currentSlot == lastClicked){
                    fail(player);
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.CLICKED_SAME));
                    return;
                }
                if(event.getSlot() == currentSlot){
                    currentCount++;
                    player.playSound(player.getLocation() ,soundManager.playInventorySuccessSound(),soundManager.getSoundVolume(), 1);
                    if(currentCount == Main.getInstance().getConfig().getInt("Settings.HintCount")){
                        player.closeInventory();
                        active = false;
                        player.playSound(player.getLocation() ,soundManager.playAllEggsFound(),soundManager.getSoundVolume(), 1);
                        player.sendMessage(getReward(player));
                    }
                    clickedRight = true;
                    lastClicked = currentSlot;
                }else{
                    fail(player);
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.CLICKED_SAME));
                }
            }
        }
    }

    public String getReward(Player player){
        Main plugin = Main.getInstance();
        for(String sections : plugin.getEggDataManager().savedEggSections()) {
            if (Main.getInstance().getEggManager().containsPlayer(player.getName())) {
                if (Main.getInstance().getEggManager().checkFoundAll(player, sections)) continue;
                int number = Main.getInstance().getEggManager().getRandomNotFoundEgg(player, sections);
                ConfigLocationUtil location = new ConfigLocationUtil(plugin, "PlacedEggs." + number + ".");
                if (location.loadLocation(sections) != null) {
                    int random = new Random().nextInt(2);
                    return Main.getInstance().getMessageManager().getMessage(MessageKey.EGG_HINT).replaceAll("%X%", random == 1 ? String.valueOf(location.loadLocation(sections).getBlockX()) : "§k1").replaceAll("%Y%", String.valueOf(location.loadLocation(sections).getBlockY())).replaceAll("%Z%", random == 0 ? String.valueOf(location.loadLocation(sections).getBlockZ()) : "§k1");
                }
            }
        }
        return null;
    }

    public void fail(Player player){
        player.closeInventory();
            player.playSound(player.getLocation() ,Main.getInstance().getSoundManager().playErrorSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
        active = false;
    }



}
