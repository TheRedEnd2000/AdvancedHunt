package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.InventoryManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.IInventoryMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggplacelist.EggPlaceMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.sectionselection.CollectionSelectListMenu;
import de.theredend2000.advancedegghunt.managers.soundmanager.SoundManager;
import de.theredend2000.advancedegghunt.util.enums.DeletionTypes;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class InventoryClickEventListener implements Listener {

    private MessageManager messageManager;

    public InventoryClickEventListener(){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
        messageManager = Main.getInstance().getMessageManager();
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event){
        SoundManager soundManager = Main.getInstance().getSoundManager();
        InventoryManager inventoryManager = Main.getInstance().getInventoryManager();
        if (!(event.getWhoClicked() instanceof Player) || event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof IInventoryMenu) {
            event.setCancelled(true);
            if(event.getAction().equals(InventoryAction.HOTBAR_MOVE_AND_READD)) event.setCancelled(true);
            if (event.getCurrentItem() == null) {
                return;
            }
            IInventoryMenu menu = (IInventoryMenu) holder;
            menu.handleMenu(event);
        }

        if(player.getInventory().equals(event.getClickedInventory()) && player.getOpenInventory().getTitle().equals("Eggs place list")){
            Set<String> keys = Main.getInstance().getPluginConfig().getPlaceEggIds();
            for(String key : keys){
                if(event.getCurrentItem().getType().name().equalsIgnoreCase(Main.getInstance().getPluginConfig().getPlaceEggType(key))){
                    player.sendMessage(messageManager.getMessage(MessageKey.BLOCK_LISTED));
                    return;
                }
            }

            int nextNumber = 0;
            if (!keys.isEmpty()) {
                for (int i = 0; i <= keys.size(); i++) {
                    String key = Integer.toString(i);
                    if (!keys.contains(key)) {
                        nextNumber = i;
                        break;
                    }
                }
            }
            Main.getInstance().getPluginConfig().setPlaceEggType(nextNumber, event.getCurrentItem().getType().name().toUpperCase());
            Main.getInstance().getPluginConfig().saveData();
            new EggPlaceMenu(Main.getPlayerMenuUtility(player)).open();
        }

        switch (event.getView().getTitle()) {
            case "Advanced Egg Settings":
                if (event.getAction().equals(InventoryAction.HOTBAR_MOVE_AND_READD)) event.setCancelled(true);
                event.setCancelled(true);
                if (!event.getCurrentItem().getItemMeta().hasLocalizedName()) {
                    return;
                }
                switch (event.getCurrentItem().getItemMeta().getLocalizedName()) {
                    case "settings.close":
                        player.closeInventory();
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        break;
                    case "settings.foundoneegg":
                        Main.getInstance().getPluginConfig().setPlayerFoundOneEggRewards(!Main.getInstance().getPluginConfig().getPlayerFoundOneEggRewards());
                        Main.getInstance().getPluginConfig().saveData();
                        inventoryManager.createEggsSettingsInventory(player);
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        break;
                    case "settings.foundalleggs":
                        Main.getInstance().getPluginConfig().setPlayerFoundAllEggsReward(!Main.getInstance().getPluginConfig().getPlayerFoundAllEggsReward());
                        Main.getInstance().getPluginConfig().saveData();
                        inventoryManager.createEggsSettingsInventory(player);
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        break;
                    case "settings.updater":
                        Main.getInstance().getPluginConfig().setUpdater(!Main.getInstance().getPluginConfig().getUpdater());
                        Main.getInstance().getPluginConfig().saveData();
                        inventoryManager.createEggsSettingsInventory(player);
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        break;
                    case "settings.commandfeedback":
                        Main.getInstance().getPluginConfig().setDisableCommandFeedback(!Main.getInstance().getPluginConfig().getDisableCommandFeedback());
                        Main.getInstance().getPluginConfig().saveData();
                        inventoryManager.createEggsSettingsInventory(player);
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        break;
                    case "settings.soundvolume":
                        int currentVolume = Main.getInstance().getPluginConfig().getSoundVolume();
                        if (event.getAction() == InventoryAction.PICKUP_ALL) {
                            if (currentVolume == 15) {
                                player.sendMessage(messageManager.getMessage(MessageKey.SOUND_VOLUME));
                                return;
                            }
                            Main.getInstance().getPluginConfig().setSoundVolume(currentVolume + 1);

                        } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                            if (currentVolume == 0) {
                                player.sendMessage(messageManager.getMessage(MessageKey.SOUND_VOLUME));
                                return;
                            }
                            Main.getInstance().getPluginConfig().setSoundVolume(currentVolume - 1);
                        }
                        Main.getInstance().getPluginConfig().saveData();
                        inventoryManager.createEggsSettingsInventory(player);
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        break;
                    case "settings.armorstandglow":
                        int currentTime = Main.getInstance().getPluginConfig().getArmorstandGlow();
                        if (event.getAction() == InventoryAction.PICKUP_ALL) {
                            if (currentTime == 120) {
                                player.sendMessage(messageManager.getMessage(MessageKey.ARMORSTAND_GLOW));
                                return;
                            }
                            Main.getInstance().getPluginConfig().setArmorstandGlow(currentTime + 1);

                        } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                            if (currentTime == 0) {
                                player.sendMessage(messageManager.getMessage(MessageKey.ARMORSTAND_GLOW));
                                return;
                            }
                            Main.getInstance().getPluginConfig().setArmorstandGlow(currentTime - 1);
                        }
                        Main.getInstance().getPluginConfig().saveData();
                        inventoryManager.createEggsSettingsInventory(player);
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        break;
                    case "settings.showcoordinates":
                        Main.getInstance().getPluginConfig().setShowCoordinatesWhenEggFoundInProgressInventory(!Main.getInstance().getPluginConfig().getShowCoordinatesWhenEggFoundInProgressInventory());
                        Main.getInstance().getPluginConfig().saveData();
                        inventoryManager.createEggsSettingsInventory(player);
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        break;
                    case "settings.eggnearbyradius":
                        int currentRadius = Main.getInstance().getPluginConfig().getShowEggsNearbyMessageRadius();
                        if (event.getAction() == InventoryAction.PICKUP_ALL) {
                            if (currentRadius == 50) {
                                player.sendMessage(messageManager.getMessage(MessageKey.EGG_RADIUS));
                                return;
                            }
                            Main.getInstance().getPluginConfig().setShowEggsNearbyMessageRadius(currentRadius + 1);

                        } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                            if (currentRadius == 0) {
                                player.sendMessage(messageManager.getMessage(MessageKey.EGG_RADIUS));
                                return;
                            }
                            Main.getInstance().getPluginConfig().setShowEggsNearbyMessageRadius(currentRadius - 1);
                        }
                        Main.getInstance().getPluginConfig().saveData();
                        inventoryManager.createEggsSettingsInventory(player);
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        break;
                    case "settings.pluginprefix":
                        Main.getInstance().getPluginConfig().setPluginPrefixEnabled(!Main.getInstance().getPluginConfig().getPluginPrefixEnabled());
                        Main.getInstance().getPluginConfig().saveData();
                        inventoryManager.createEggsSettingsInventory(player);
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        break;
                    case "settings.firework":
                        Main.getInstance().getPluginConfig().setShowFireworkAfterEggFound(!Main.getInstance().getPluginConfig().getShowFireworkAfterEggFound());
                        Main.getInstance().getPluginConfig().saveData();
                        inventoryManager.createEggsSettingsInventory(player);
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        break;
                }
                break;
            case "Collection creator": {
                event.setCancelled(true);
                if (!event.getCurrentItem().getItemMeta().hasDisplayName()) {
                    return;
                }
                FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(player.getUniqueId());
                boolean enabled = playerConfig.getBoolean("CollectionEdit.enabled");
                switch (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName())) {
                    case "Close":
                        player.closeInventory();
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        break;
                    case "Name":
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        new AnvilGUI.Builder()
                                .onClose(stateSnapshot -> {
                                    if (!stateSnapshot.getText().isEmpty()) {
                                        playerConfig.set("CollectionEdit.Name", stateSnapshot.getText());
                                        Main.getInstance().getPlayerEggDataManager().savePlayerData(player.getUniqueId(), playerConfig);
                                        Main.getInstance().getInventoryManager().createAddCollectionMenu(player);
                                    }
                                })
                                .onClick((slot, stateSnapshot) -> {
                                    return Collections.singletonList(AnvilGUI.ResponseAction.close());
                                })
                                .text("Enter collection name")
                                .title("Collection name")
                                .plugin(Main.getInstance())
                                .open(player);
                        break;
                    case "Status":
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        playerConfig.set("CollectionEdit.enabled", !enabled);
                        Main.getInstance().getPlayerEggDataManager().savePlayerData(player.getUniqueId(), playerConfig);
                        Main.getInstance().getInventoryManager().createAddCollectionMenu(player);
                        break;
                    case "Back":
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        new CollectionSelectListMenu(Main.getPlayerMenuUtility(player)).open();
                        break;
                    case "Create":
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        String name = playerConfig.getString("CollectionEdit.Name");
                        if (name == null) {
                            player.sendMessage("§cPlease enter a name to continue.");
                            return;
                        }
                        if (!Main.getInstance().getEggDataManager().containsSectionFile(name)) {
                            Main.getInstance().getEggDataManager().createEggCollectionFile(name, enabled);
                            new CollectionSelectListMenu(Main.getPlayerMenuUtility(player)).open();
                            playerConfig.set("CollectionEdit", null);
                            Main.getInstance().getPlayerEggDataManager().savePlayerData(player.getUniqueId(), playerConfig);
                            Main.getInstance().getRequirementsManager().changeActivity(name, true);
                            Main.getInstance().getRequirementsManager().resetReset(name);
                        } else
                            player.sendMessage("§cThe name of the collection is already chosen.");
                        break;
                }
                break;
            }
            case "Collection editor": {
                event.setCancelled(true);
                String section = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
                if (!event.getCurrentItem().getItemMeta().hasDisplayName()) {
                    return;
                }
                FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(section);
                boolean enabled = placedEggs.getBoolean("Enabled");
                switch (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName())) {
                    case "Close":
                        player.closeInventory();
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        break;
                    case "Status":
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        placedEggs.set("Enabled", !enabled);
                        Main.getInstance().getEggDataManager().savePlacedEggs(section, placedEggs);
                        Main.getInstance().getInventoryManager().createEditCollectionMenu(player, section);
                        break;
                    case "Requirements":
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        Main.getInstance().getInventoryRequirementsManager().createSelectInventory(player, section);
                        break;
                    case "Reset (BETA)":
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        Main.getInstance().getResetInventoryManager().createSelectInventory(player, section);
                        break;
                    case "Back":
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        new CollectionSelectListMenu(Main.getPlayerMenuUtility(player)).open();
                        break;
                    case "Delete":
                        if (section.equalsIgnoreCase("default")) {
                            player.sendMessage("§cBecause of many issues it is not possible to delete the default section.\n§cIf you want to disable it please just change the status.");
                            return;
                        }
                        Main.getInstance().getRequirementsManager().removeAllEggBlocks(section, player.getUniqueId());
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        player.sendMessage(messageManager.getMessage(MessageKey.COLLECTION_DELETED).replaceAll("%COLLECTION%", section));
                        for (UUID uuids : Main.getInstance().getEggDataManager().savedPlayers()) {
                            FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids);
                            playerConfig.set("FoundEggs." + section, null);
                            playerConfig.set("SelectedSection", "default");
                            Main.getInstance().getPlayerEggDataManager().savePlayerData(uuids, playerConfig);
                        }
                        Main.getInstance().getEggDataManager().deleteCollection(section);
                        player.closeInventory();
                        break;
                    case "Deletion Types":
                        DeletionTypes deletionTypes = Main.getInstance().getPlayerEggDataManager().getDeletionType(player.getUniqueId());
                        switch (deletionTypes) {
                            case Noting:
                                Main.getInstance().getPlayerEggDataManager().setDeletionType(DeletionTypes.Player_Heads, player.getUniqueId());
                                break;
                            case Player_Heads:
                                Main.getInstance().getPlayerEggDataManager().setDeletionType(DeletionTypes.Everything, player.getUniqueId());
                                break;
                            case Everything:
                                Main.getInstance().getPlayerEggDataManager().setDeletionType(DeletionTypes.Noting, player.getUniqueId());
                                break;
                        }
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                        Main.getInstance().getInventoryManager().createEditCollectionMenu(player, section);
                        break;
                }
                break;
            }
        }
    }
}
