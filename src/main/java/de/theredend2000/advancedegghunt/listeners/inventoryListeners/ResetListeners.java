package de.theredend2000.advancedegghunt.listeners.inventoryListeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.soundmanager.SoundManager;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ResetListeners implements Listener {

    private Main plugin;
    private MessageManager messageManager;

    public ResetListeners(Main plugin){
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this,plugin);
        messageManager = plugin.getMessageManager();
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent event) {
        SoundManager soundManager = plugin.getSoundManager();
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() != null) {
                if (event.getView().getTitle().equals("Reset - Selection")) {
                    event.setCancelled(true);
                    String section = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
                    FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(section);
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
                            case "Reset - Year":
                                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                                int currentYear = placedEggs.getInt("Reset.Year");

                                if (event.getAction() == InventoryAction.PICKUP_ALL) {
                                    placedEggs.set("Reset.Year", currentYear + 1);
                                } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                                    if (currentYear - 1 >= 0) {
                                        placedEggs.set("Reset.Year", currentYear - 1);
                                    }
                                } else if (event.getClick() == ClickType.MIDDLE) {
                                    placedEggs.set("Reset.Year", 0);
                                }
                                plugin.getEggDataManager().savePlacedEggs(section, placedEggs);
                                plugin.getResetInventoryManager().createSelectInventory(player,section);
                                break;
                            case "Reset - Month":
                                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                                int currentMonth = placedEggs.getInt("Reset.Month");

                                if (event.getAction() == InventoryAction.PICKUP_ALL) {
                                    placedEggs.set("Reset.Month", currentMonth + 1);
                                } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                                    if (currentMonth - 1 >= 0) {
                                        placedEggs.set("Reset.Month", currentMonth - 1);
                                    }
                                } else if (event.getClick() == ClickType.MIDDLE) {
                                    placedEggs.set("Reset.Month", 0);
                                }
                                plugin.getEggDataManager().savePlacedEggs(section, placedEggs);
                                plugin.getResetInventoryManager().createSelectInventory(player,section);
                                break;
                            case "Reset - Day":
                                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                                int currentDay = placedEggs.getInt("Reset.Day");

                                if (event.getAction() == InventoryAction.PICKUP_ALL) {
                                    placedEggs.set("Reset.Day", currentDay + 1);
                                } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                                    if (currentDay - 1 >= 0) {
                                        placedEggs.set("Reset.Day", currentDay - 1);
                                    }
                                } else if (event.getClick() == ClickType.MIDDLE) {
                                    placedEggs.set("Reset.Day", 0);
                                }
                                plugin.getEggDataManager().savePlacedEggs(section, placedEggs);
                                plugin.getResetInventoryManager().createSelectInventory(player,section);
                                break;
                            case "Reset - Hour":
                                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                                int currentHour = placedEggs.getInt("Reset.Hour");

                                if (event.getAction() == InventoryAction.PICKUP_ALL) {
                                    placedEggs.set("Reset.Hour", currentHour + 1);
                                } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                                    if (currentHour - 1 >= 0) {
                                        placedEggs.set("Reset.Hour", currentHour - 1);
                                    }
                                } else if (event.getClick() == ClickType.MIDDLE) {
                                    placedEggs.set("Reset.Hour", 0);
                                }
                                plugin.getEggDataManager().savePlacedEggs(section, placedEggs);
                                plugin.getResetInventoryManager().createSelectInventory(player,section);
                                break;
                            case "Reset - Minute":
                                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                                int currentMin = placedEggs.getInt("Reset.Minute");

                                if (event.getAction() == InventoryAction.PICKUP_ALL) {
                                    placedEggs.set("Reset.Minute", currentMin + 1);
                                } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                                    if (currentMin - 1 >= 0) {
                                        placedEggs.set("Reset.Minute", currentMin - 1);
                                    }
                                } else if (event.getClick() == ClickType.MIDDLE) {
                                    placedEggs.set("Reset.Minute", 0);
                                }
                                plugin.getEggDataManager().savePlacedEggs(section, placedEggs);
                                plugin.getResetInventoryManager().createSelectInventory(player,section);
                                break;
                            case "Reset - Second":
                                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                                int currentSec = placedEggs.getInt("Reset.Second");

                                if (event.getAction() == InventoryAction.PICKUP_ALL) {
                                    placedEggs.set("Reset.Second", currentSec + 1);
                                } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                                    if (currentSec - 1 >= 0) {
                                        placedEggs.set("Reset.Second", currentSec - 1);
                                    }
                                } else if (event.getClick() == ClickType.MIDDLE) {
                                    placedEggs.set("Reset.Second", 0);
                                }
                                plugin.getEggDataManager().savePlacedEggs(section, placedEggs);
                                plugin.getResetInventoryManager().createSelectInventory(player,section);
                                break;
                            case "Reset all":
                                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                                plugin.getRequirementsManager().resetReset(section);
                                plugin.getResetInventoryManager().createSelectInventory(player,section);
                                break;
                        }
                    }
                }
            }
        }
    }
}
