package de.theredend2000.advancedegghunt.managers.inventorymanager.collection;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.SoundManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.CollectionSelectMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.ResetMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.InventoryMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.requirements.RequirementMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.ItemHelper;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.enums.DeletionTypes;
import de.theredend2000.advancedegghunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Random;
import java.util.UUID;

public class CollectionEditor extends InventoryMenu {
    private MessageManager messageManager;

    public CollectionEditor(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Collection editor", (short) 45);
        messageManager = Main.getInstance().getMessageManager();
    }

    public void open(String collection) {
        super.addMenuBorder();
        addMenuBorderButtons();
        menuContent(collection);

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    private void addMenuBorderButtons() {
        inventoryContent[36] = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("collection_editor.back")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.BACK_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.BACK_BUTTON))
                .setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0="))
                .build();
        inventoryContent[40] = new ItemBuilder(XMaterial.BARRIER)
                .setCustomId("collection_editor.close")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.CLOSE_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.CLOSE_BUTTON))
                .build();
    }

    private void menuContent(String collection) {
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        boolean enabled = placedEggs.getBoolean("Enabled");
        boolean oneplayer = placedEggs.getBoolean("OnePlayer");
        boolean hideforplayer = placedEggs.getBoolean("HideForPlayer");
        DeletionTypes deletionType = Main.getInstance().getPlayerEggDataManager().getDeletionType(super.playerMenuUtility.getOwner().getUniqueId());

        getInventory().setItem(4, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("collection_editor.collection_selected")
                .setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(new Random().nextInt(7)))
                .setDisplayName("§6" + collection)
                .build());
        getInventory().setItem(20, new ItemBuilder(enabled ? XMaterial.LIME_DYE : XMaterial.RED_DYE)
                .setCustomId("collection_editor.status")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.COLLECTION_EDITOR_STATUS))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.COLLECTION_EDITOR_STATUS, "%STATUS%", (enabled ? "&aEnabled" : "&cDisabled")))
                .build());

        getInventory().setItem(24, new ItemBuilder(XMaterial.RED_STAINED_GLASS)
                .setCustomId("collection_editor.delete")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.COLLECTION_EDITOR_DELETE))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.COLLECTION_EDITOR_DELETE))
                .build());

        getInventory().setItem(13, new ItemBuilder(XMaterial.COMPARATOR)
                .setCustomId("collection_editor.requirements")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.COLLECTION_EDITOR_REQUIREMENTS))
                .setLore(Main.getInstance().getRequirementsManager().getListRequirementsLore(collection))
                .build());

        getInventory().setItem(31, new ItemBuilder(XMaterial.REPEATER)
                .setCustomId("collection_editor.reset")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.COLLECTION_EDITOR_RESET))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.COLLECTION_EDITOR_RESET, "%RESET_TIME%", Main.getInstance().getRequirementsManager().getConvertedTime(collection)))
                .build());

        getInventory().setItem(42, new ItemBuilder(hideforplayer ? XMaterial.LIME_DYE : XMaterial.RED_DYE)
                .setCustomId("collection_editor.hide")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.COLLECTION_EDITOR_HIDE_FOR_PLAYER))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.COLLECTION_EDITOR_HIDE_FOR_PLAYER))
                .build());

        getInventory().setItem(43, new ItemBuilder(oneplayer ? XMaterial.LIME_DYE : XMaterial.RED_DYE)
                .setCustomId("collection_editor.only_one")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.COLLECTION_EDITOR_ONE_PLAYER))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.COLLECTION_EDITOR_ONE_PLAYER))
                .build());

        getInventory().setItem(44, new ItemBuilder(XMaterial.WOODEN_AXE)
                .setCustomId("collection_editor.delete_type")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.COLLECTION_EDITOR_DELETE_TYPE))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.COLLECTION_EDITOR_DELETE_TYPE,
                        "%NOTHING_TYPE%", (deletionType == DeletionTypes.Noting ? "&b➤ " : "&7"),
                        "%PLAYER_HEADS_TYPE%", (deletionType == DeletionTypes.Player_Heads ? "&b➤ " : "&7"),
                        "%EVERYTHING_TYPE%", (deletionType == DeletionTypes.Everything ? "&b➤ " : "&7")))
                .build());
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        String collection = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
        if (!event.getCurrentItem().getItemMeta().hasDisplayName()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        SoundManager soundManager = Main.getInstance().getSoundManager();

        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        boolean enabled = placedEggs.getBoolean("Enabled");
        boolean oneplayer = placedEggs.getBoolean("OnePlayer");
        boolean hideForPlayer = placedEggs.getBoolean("HideForPlayer");

        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "collection_editor.close":
                player.closeInventory();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "collection_editor.back":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new CollectionSelectMenu(Main.getPlayerMenuUtility(player)).open();
                break;
            case "collection_editor.status":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                placedEggs.set("Enabled", !enabled);
                Main.getInstance().getEggDataManager().savePlacedEggs(collection);
                menuContent(collection);
                break;
            case "collection_editor.requirements":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new RequirementMenu(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "collection_editor.reset":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new ResetMenu(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "collection_editor.delete":
                if (collection.equalsIgnoreCase("default")) {
                    messageManager.sendMessage(player, MessageKey.COLLECTION_DEFAULT_UNDELETABLE);
                    return;
                }
                Main.getInstance().getRequirementsManager().removeAllEggBlocks(collection, player.getUniqueId());
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                messageManager.sendMessage(player, MessageKey.COLLECTION_DELETED, "%COLLECTION%", collection);
                for (UUID uuids : Main.getInstance().getEggDataManager().savedPlayers()) {
                    FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids);
                    playerConfig.set("FoundEggs." + collection, null);
                    playerConfig.set("SelectedSection", "default");
                    Main.getInstance().getPlayerEggDataManager().savePlayerData(uuids, playerConfig);
                }
                Main.getInstance().getEggDataManager().deleteCollection(collection);
                player.closeInventory();
                break;
            case "collection_editor.delete_type":
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
                menuContent(collection);
                break;
            case "collection_editor.hide":
                /*player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                placedEggs.set("HideForPlayer", !hideForPlayer);
                Main.getInstance().getEggDataManager().savePlacedEggs(collection, placedEggs);
                menuContent(collection);*/
                messageManager.sendMessage(player, MessageKey.FEATURE_COMING_SOON);
                break;
            case "collection_editor.only_one":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                placedEggs.set("OnePlayer", !oneplayer);
                Main.getInstance().getEggDataManager().savePlacedEggs(collection);
                if(oneplayer && placedEggs.contains("PlacedEggs."))
                    for(String eggIDs : placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false))
                        Main.getInstance().getEggManager().markEggAsFound(collection, eggIDs, false);
                menuContent(collection);
                break;
        }
    }
}
