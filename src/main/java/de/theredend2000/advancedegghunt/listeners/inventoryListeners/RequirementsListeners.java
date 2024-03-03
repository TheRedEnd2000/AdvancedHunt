package de.theredend2000.advancedegghunt.listeners.inventoryListeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.soundmanager.SoundManager;
import de.theredend2000.advancedegghunt.util.DateTimeUtil;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;

public class RequirementsListeners implements Listener {

    private Main plugin;
    private MessageManager messageManager;

    public RequirementsListeners(Main plugin){
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        messageManager = plugin.getMessageManager();
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent event) {
        SoundManager soundManager = plugin.getSoundManager();
        if (!(event.getWhoClicked() instanceof Player) ||
                event.getCurrentItem() == null ||
                event.getCurrentItem().getItemMeta() == null ||
                !event.getCurrentItem().getItemMeta().hasDisplayName()) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        switch (title) {
            case "Requirements - Selection": {
                event.setCancelled(true);
                String section = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
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
                        plugin.getRequirementsManager().changeActivity(section, true);
                        player.sendMessage(messageManager.getMessage(MessageKey.ACTIVATE_REQUIREMENTS));
                        plugin.getInventoryRequirementsManager().createSelectInventory(player, section);
                        break;
                    case "Deactivate all":
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        plugin.getRequirementsManager().changeActivity(section, false);
                        player.sendMessage(messageManager.getMessage(MessageKey.DEACTIVATE_REQUIREMENTS));
                        plugin.getInventoryRequirementsManager().createSelectInventory(player, section);
                        break;
                    case "Requirements Order":
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        player.sendMessage("§cThis feature is coming soon.");
                        break;
                }
                break;
            }
            case "Requirements - Hours": {
                event.setCancelled(true);
                String section = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
                FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(section);
                for (int i = 0; i < 24; i++) {
                    if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Hour " + i)) {
                        boolean enabled = placedEggs.getBoolean("Requirements.Hours." + i);
                        placedEggs.set("Requirements.Hours." + i, !enabled);
                        plugin.getEggDataManager().savePlacedEggs(section, placedEggs);
                        plugin.getInventoryRequirementsManager().createHourInventory(player, section);
                    }
                }
                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Close"))
                    player.closeInventory();
                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Back"))
                    plugin.getInventoryRequirementsManager().createSelectInventory(player, section);
                break;
            }
            case "Requirements - Weekday": {
                event.setCancelled(true);
                String section = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
                FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(section);
                for (String weekdays : new ArrayList<>(DateTimeUtil.getWeekList())) {
                    if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals(weekdays)) {
                        boolean enabled = placedEggs.getBoolean("Requirements.Weekday." + weekdays);
                        placedEggs.set("Requirements.Weekday." + weekdays, !enabled);
                        plugin.getEggDataManager().savePlacedEggs(section, placedEggs);
                        plugin.getInventoryRequirementsManager().createWeekdayInventory(player, section);
                    }
                }
                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Close"))
                    player.closeInventory();
                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Back"))
                    plugin.getInventoryRequirementsManager().createSelectInventory(player, section);
                break;
            }
            case "Requirements - Month": {
                event.setCancelled(true);
                String section = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
                FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(section);
                for (String month : new ArrayList<>(DateTimeUtil.getMonthList())) {
                    if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals(month)) {
                        boolean enabled = placedEggs.getBoolean("Requirements.Month." + month);
                        placedEggs.set("Requirements.Month." + month, !enabled);
                        plugin.getEggDataManager().savePlacedEggs(section, placedEggs);
                        plugin.getInventoryRequirementsManager().createMonthInventory(player, section);
                    }
                }
                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Close"))
                    player.closeInventory();
                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Back"))
                    plugin.getInventoryRequirementsManager().createSelectInventory(player, section);
                break;
            }
            case "Requirements - Year": {
                event.setCancelled(true);
                String section = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
                FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(section);
                int currentYear = DateTimeUtil.getCurrentYear();
                for (int year = currentYear; year < (currentYear + 28); year++) {
                    if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Year " + year)) {
                        boolean enabled = placedEggs.getBoolean("Requirements.Year." + year);
                        placedEggs.set("Requirements.Year." + year, !enabled);
                        plugin.getEggDataManager().savePlacedEggs(section, placedEggs);
                        plugin.getInventoryRequirementsManager().createYearInventory(player, section);
                    }
                }
                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Close"))
                    player.closeInventory();
                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Back"))
                    plugin.getInventoryRequirementsManager().createSelectInventory(player, section);
                break;
            }
            case "Requirements - Season": {
                event.setCancelled(true);
                String section = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
                FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(section);
                for (String season : new ArrayList<>(DateTimeUtil.getSeasonList())) {
                    if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals(season)) {
                        boolean enabled = placedEggs.getBoolean("Requirements.Season." + season);
                        placedEggs.set("Requirements.Season." + season, !enabled);
                        plugin.getEggDataManager().savePlacedEggs(section, placedEggs);
                        plugin.getInventoryRequirementsManager().createSeasonInventory(player, section);
                    }
                }
                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Close"))
                    player.closeInventory();
                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Back"))
                    plugin.getInventoryRequirementsManager().createSelectInventory(player, section);
                break;
            }
        }
    }
}
