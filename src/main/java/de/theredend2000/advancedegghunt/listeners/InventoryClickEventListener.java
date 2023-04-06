package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ConfigLocationUtil;
import de.theredend2000.advancedegghunt.versions.VersionManager;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.paginatedMenu.Menu;
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
import org.bukkit.inventory.InventoryHolder;
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
                if(Main.getInstance().getPlaceEggsPlayers().contains(player)){
                    if(event.getInventory().getViewers().contains(player)){
                        event.setCancelled(true);
                    }
                }
                InventoryHolder holder = event.getInventory().getHolder();
                if (holder instanceof Menu) {
                    event.setCancelled(true);
                    if (event.getCurrentItem() == null) {
                        return;
                    }
                    Menu menu = (Menu) holder;
                    menu.handleMenu(event);
                }
                if(event.getView().getTitle().equals("Advanced Egg Settings")){
                    event.setCancelled(true);
                    if(event.getCurrentItem().getItemMeta().hasLocalizedName()){
                        switch (event.getCurrentItem().getItemMeta().getLocalizedName()){
                            case "settings.close":
                                player.closeInventory();
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                break;
                            case "settings.foundoneegg":
                                Main.getInstance().getConfig().set("Settings.PlayerFoundOneEggRewards",!Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundOneEggRewards"));
                                Main.getInstance().saveConfig();
                                VersionManager.getInventoryManager().createEggsSettingsInventory(player);
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                break;
                            case "settings.foundalleggs":
                                Main.getInstance().getConfig().set("Settings.PlayerFoundAllEggsReward",!Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundAllEggsReward"));
                                Main.getInstance().saveConfig();
                                VersionManager.getInventoryManager().createEggsSettingsInventory(player);
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                break;
                            case "settings.updater":
                                Main.getInstance().getConfig().set("Settings.Updater",!Main.getInstance().getConfig().getBoolean("Settings.Updater"));
                                Main.getInstance().saveConfig();
                                VersionManager.getInventoryManager().createEggsSettingsInventory(player);
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                break;
                            case "settings.commandfeedback":
                                Main.getInstance().getConfig().set("Settings.DisableCommandFeedback",!Main.getInstance().getConfig().getBoolean("Settings.DisableCommandFeedback"));
                                Main.getInstance().saveConfig();
                                VersionManager.getInventoryManager().createEggsSettingsInventory(player);
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                break;
                        }
                    }
                }
            }
        }
    }

}
