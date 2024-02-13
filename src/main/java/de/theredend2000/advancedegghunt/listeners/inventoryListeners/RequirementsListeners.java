package de.theredend2000.advancedegghunt.listeners.inventoryListeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.sectionselection.SelectionSelectListMenu;
import de.theredend2000.advancedegghunt.managers.soundmanager.SoundManager;
import de.theredend2000.advancedegghunt.util.DateTimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.UUID;

public class RequirementsListeners implements Listener {

    private Main plugin;

    public RequirementsListeners(Main plugin){
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this,plugin);
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent event) {
        SoundManager soundManager = plugin.getSoundManager();
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
                if (event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() != null) {
                    if (event.getView().getTitle().equals("Requirements - Selection")) {
                        event.setCancelled(true);
                        String section = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
                        if (event.getCurrentItem().getItemMeta().hasDisplayName()) {
                            switch (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName())) {
                                case "Close":
                                    player.closeInventory();
                                    player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                                    break;
                                case "Back":
                                    player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                                    plugin.getInventoryManager().createEditCollectionMenu(player, section);
                                    break;
                                case "Selection - Hours":
                                    player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                                    plugin.getInventoryRequirementsManager().createHourInventory(player, section);
                                    break;
                                case "Selection - Date":
                                    player.sendMessage("§cThis requirement section is currently unavailable.");
                                    break;
                                case "Selection - Weekday":
                                    player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                                    plugin.getInventoryRequirementsManager().createWeekdayInventory(player, section);
                                    break;
                                case "Selection - Month":
                                    player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                                    plugin.getInventoryRequirementsManager().createMonthInventory(player, section);
                                    break;
                                case "Selection - Year":
                                    player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                                    plugin.getInventoryRequirementsManager().createYearInventory(player, section);
                                    break;
                                case "Selection - Season":
                                    player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                                    plugin.getInventoryRequirementsManager().createSeasonInventory(player, section);
                                    break;
                                case "Activate all":
                                    player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                                    plugin.getRequirementsManager().changeActivity(section,true);
                                    player.sendMessage("§aActivated all requirements.");
                                    break;
                                case "Deactivate all":
                                    player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                                    plugin.getRequirementsManager().changeActivity(section,false);
                                    player.sendMessage("§cDeactivated all requirements.");
                                    break;
                            }
                        }
                    } else if (event.getView().getTitle().equals("Requirements - Hours")) {
                        event.setCancelled(true);
                        String section = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
                        if (event.getCurrentItem().getItemMeta().hasDisplayName()) {
                            FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(section);
                            for (int i = 0; i < 24; i++) {
                                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Hour " + i)) {
                                    boolean enabled = placedEggs.getBoolean("Requirements.Hours." + i);
                                    placedEggs.set("Requirements.Hours." + i, !enabled);
                                    plugin.getEggDataManager().savePlacedEggs(section, placedEggs);
                                    player.sendMessage("§aChanged hour " + i + " to §6" + enabled);
                                    plugin.getInventoryRequirementsManager().createHourInventory(player, section);
                                }
                            }
                            if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Close"))
                                player.closeInventory();
                            if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Back"))
                                plugin.getInventoryRequirementsManager().createSelectInventory(player,section);
                        }
                    } else if (event.getView().getTitle().equals("Requirements - Weekday")) {
                        event.setCancelled(true);
                        String section = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
                        if (event.getCurrentItem().getItemMeta().hasDisplayName()) {
                            FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(section);
                            for (String weekdays : new ArrayList<>(DateTimeUtil.getWeekList())) {
                                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals(weekdays)) {
                                    boolean enabled = placedEggs.getBoolean("Requirements.Weekday." + weekdays);
                                    placedEggs.set("Requirements.Weekday." + weekdays, !enabled);
                                    plugin.getEggDataManager().savePlacedEggs(section, placedEggs);
                                    player.sendMessage("§aChanged " + weekdays + " to §6" + enabled);
                                    plugin.getInventoryRequirementsManager().createWeekdayInventory(player, section);
                                }
                            }
                            if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Close"))
                                player.closeInventory();
                            if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Back"))
                                plugin.getInventoryRequirementsManager().createSelectInventory(player,section);
                        }
                    }else if (event.getView().getTitle().equals("Requirements - Month")) {
                        event.setCancelled(true);
                        String section = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
                        if (event.getCurrentItem().getItemMeta().hasDisplayName()) {
                            FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(section);
                            for (String month : new ArrayList<>(DateTimeUtil.getMonthList())) {
                                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals(month)) {
                                    boolean enabled = placedEggs.getBoolean("Requirements.Month." + month);
                                    placedEggs.set("Requirements.Month." + month, !enabled);
                                    plugin.getEggDataManager().savePlacedEggs(section, placedEggs);
                                    player.sendMessage("§aChanged " + month + " to §6" + enabled);
                                    plugin.getInventoryRequirementsManager().createMonthInventory(player, section);
                                }
                            }
                            if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Close"))
                                player.closeInventory();
                            if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Back"))
                                plugin.getInventoryRequirementsManager().createSelectInventory(player,section);
                        }
                    }else if (event.getView().getTitle().equals("Requirements - Year")) {
                        event.setCancelled(true);
                        String section = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
                        if (event.getCurrentItem().getItemMeta().hasDisplayName()) {
                            FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(section);
                            int currentYear = DateTimeUtil.getCurrentYear();
                            for (int year = currentYear; year < (currentYear+28);year++) {
                                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Year "+year)) {
                                    boolean enabled = placedEggs.getBoolean("Requirements.Year." + year);
                                    placedEggs.set("Requirements.Year." + year, !enabled);
                                    plugin.getEggDataManager().savePlacedEggs(section, placedEggs);
                                    player.sendMessage("§aChanged " + year + " to §6" + enabled);
                                    plugin.getInventoryRequirementsManager().createYearInventory(player, section);
                                }
                            }
                            if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Close"))
                                player.closeInventory();
                            if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Back"))
                                plugin.getInventoryRequirementsManager().createSelectInventory(player,section);
                        }
                    }else if (event.getView().getTitle().equals("Requirements - Season")) {
                        event.setCancelled(true);
                        String section = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
                        if (event.getCurrentItem().getItemMeta().hasDisplayName()) {
                            FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(section);
                            for (String season : new ArrayList<>(DateTimeUtil.getSeasonList())) {
                                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals(season)) {
                                    boolean enabled = placedEggs.getBoolean("Requirements.Season." + season);
                                    placedEggs.set("Requirements.Season." + season, !enabled);
                                    plugin.getEggDataManager().savePlacedEggs(section, placedEggs);
                                    player.sendMessage("§aChanged " + season + " to §6" + enabled);
                                    plugin.getInventoryRequirementsManager().createSeasonInventory(player, section);
                                }
                            }
                            if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Close"))
                                player.closeInventory();
                            if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Back"))
                                plugin.getInventoryRequirementsManager().createSelectInventory(player,section);
                        }
                    }
                }
            }
        }

}
