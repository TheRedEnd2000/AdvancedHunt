package de.theredend2000.advancedegghunt.managers.inventorymanager.collection;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.SoundManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.CollectionSelectMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.InventoryMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.ItemHelper;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Collections;

public class CollectionCreator extends InventoryMenu {
    private MessageManager messageManager;
    private String name;
    private boolean enabled;

    public CollectionCreator(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Collection creator", (short) 45);
        messageManager = Main.getInstance().getMessageManager();
    }

    public void open() {
        super.addMenuBorder();
        addMenuBorderButtons();
        menuContent();

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    private void addMenuBorderButtons() {
        inventoryContent[40] = new ItemBuilder(XMaterial.BARRIER)
                .setCustomId("collection_creator.close")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.CLOSE_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.CLOSE_BUTTON))
                .build();
    }

    private void menuContent() {
        getInventory().setItem(20, new ItemBuilder(XMaterial.PAPER)
                .setCustomId("collection_creator.name")
                .setDisplayName("§3Name")
                .setLore("§7Currently: " + (name != null ? name : "§cnone"), "", "§eClick to change.")
                .build());
        getInventory().setItem(22, new ItemBuilder(enabled ? XMaterial.LIME_DYE : XMaterial.RED_DYE)
                .setCustomId("collection_creator.status")
                .setDisplayName("§3Status")
                .setLore("§7Currently: " + (enabled ? "§aEnabled" : "§cDisabled"), "", "§eClick to toggle.")
                .build());
        getInventory().setItem(24, new ItemBuilder(XMaterial.COMPARATOR)
                .setDisplayName("§3Requirements")
                .setLore("§cYou can change the requirements", "§cafter creating the new collection.", "", "§7All Requirements will be active", "§7on creating a new collection.")
                .build());
        getInventory().setItem(44, new ItemBuilder(XMaterial.EMERALD_BLOCK)
                .setCustomId("collection_creator.create")
                .setDisplayName("§2Create")
                .setLore("", "§eClick to create.")
                .build());
        getInventory().setItem(36, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("collection_creator.back")
                .setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0="))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.BACK_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.BACK_BUTTON))
                .build());
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (!event.getCurrentItem().getItemMeta().hasDisplayName()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        SoundManager soundManager = Main.getInstance().getSoundManager();

        FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(player.getUniqueId());
        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "collection_creator.close":
                player.closeInventory();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "collection_creator.back":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new CollectionSelectMenu(Main.getPlayerMenuUtility(player)).open();
                break;
            case "collection_creator.name":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new AnvilGUI.Builder()
                        .onClose(stateSnapshot -> {
                            if (!stateSnapshot.getText().isEmpty()) {
                                name = stateSnapshot.getText();
                                Main.getInstance().getPlayerEggDataManager().savePlayerData(player.getUniqueId(), playerConfig);
                                menuContent();
                                open();
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
            case "collection_creator.status":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                enabled = !enabled;
                Main.getInstance().getPlayerEggDataManager().savePlayerData(player.getUniqueId(), playerConfig);
                menuContent();
                break;
            case "collection_creator.create":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                if (name == null) {
                    messageManager.sendMessage(player, MessageKey.COLLECTION_NAME_REQUIRED);
                    return;
                }
                if (!Main.getInstance().getEggDataManager().containsSectionFile(name)) {
                    Main.getInstance().getEggDataManager().createEggCollectionFile(name, enabled);
                    new CollectionSelectMenu(Main.getPlayerMenuUtility(player)).open();
                    playerConfig.set("CollectionEdit", null);
                    Main.getInstance().getPlayerEggDataManager().savePlayerData(player.getUniqueId(), playerConfig);
                    Main.getInstance().getRequirementsManager().changeActivity(name, true);
                    Main.getInstance().getRequirementsManager().resetReset(name);
                    Main.getInstance().getGlobalPresetDataManager().loadPresetIntoCollectionCommands(Main.getInstance().getPluginConfig().getDefaultGlobalLoadingPreset(),name);
                    Main.getInstance().getEggManager().spawnEggParticle();
                } else {
                    messageManager.sendMessage(player, MessageKey.COLLECTION_NAME_EXISTS);
                }
                break;
        }
    }
}
