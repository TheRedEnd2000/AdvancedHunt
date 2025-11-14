package de.theredend2000.advancedhunt.managers.inventorymanager.collection;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.SoundManager;
import de.theredend2000.advancedhunt.managers.inventorymanager.CollectionSelectMenu;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.InventoryMenu;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemHelper;
import de.theredend2000.advancedhunt.util.PlayerMenuUtility;
import de.theredend2000.advancedhunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageManager;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
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
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.COLLECTION_CREATOR_NAME, "%NAME%", (name != null ? name : "§cnone")))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.COLLECTION_CREATOR_NAME, "%NAME%", (name != null ? name : "§cnone")))
                .build());

        getInventory().setItem(22, new ItemBuilder(enabled ? XMaterial.LIME_DYE : XMaterial.RED_DYE)
                .setCustomId("collection_creator.status")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.COLLECTION_CREATOR_STATUS))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.COLLECTION_CREATOR_STATUS, "%STATUS%", (enabled ? "§aEnabled" : "§cDisabled")))
                .build());

        getInventory().setItem(24, new ItemBuilder(XMaterial.COMPARATOR)
                .setCustomId("collection_creator.requirements")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.COLLECTION_CREATOR_REQUIREMENTS))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.COLLECTION_CREATOR_REQUIREMENTS))
                .build());

        getInventory().setItem(44, new ItemBuilder(XMaterial.EMERALD_BLOCK)
                .setCustomId("collection_creator.create")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.COLLECTION_CREATOR_CREATE))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.COLLECTION_CREATOR_CREATE))
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
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), player::closeInventory,3L);
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
                            player.closeInventory();
                        })
                        .onClick((slot, stateSnapshot) -> {
                            if (slot == AnvilGUI.Slot.OUTPUT) {
                                name = stateSnapshot.getText();
                                Main.getInstance().getPlayerEggDataManager().savePlayerData(player.getUniqueId(), playerConfig);

                                menuContent();
                                open();

                                return Collections.singletonList(AnvilGUI.ResponseAction.close());
                            }

                            return Collections.emptyList();
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
                    new CollectionSelectMenu(playerMenuUtility).selectCollection(name,player,true);
                    new CollectionSelectMenu(Main.getPlayerMenuUtility(player)).open();
                    playerConfig.set("CollectionEdit", null);
                    Main.getInstance().getPlayerEggDataManager().savePlayerData(player.getUniqueId(), playerConfig);
                    Main.getInstance().getRequirementsManager().changeActivity(name, true);
                    Main.getInstance().getRequirementsManager().resetReset(name);
                    Main.getInstance().getGlobalPresetDataManager().loadPresetIntoCollectionCommands(Main.getInstance().getPluginConfig().getDefaultGlobalLoadingPreset(), name);
                    Main.getInstance().getEggManager().spawnEggParticle();
                } else {
                    messageManager.sendMessage(player, MessageKey.COLLECTION_NAME_EXISTS);
                }
                break;
        }
    }
}
