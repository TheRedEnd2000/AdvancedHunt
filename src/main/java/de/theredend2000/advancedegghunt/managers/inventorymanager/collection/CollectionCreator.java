package de.theredend2000.advancedegghunt.managers.inventorymanager.collection;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.SoundManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.CollectionSelectMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.InventoryMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.ChatColor;
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
        inventoryContent[40] = new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build();
    }

    private void menuContent() {
        FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(super.playerMenuUtility.getOwner().getUniqueId());
        getInventory().setItem(20, new ItemBuilder(XMaterial.PAPER).setDisplayname("§3Name").setLore("§7Currently: " + (name != null ? name : "§cnone"), "", "§eClick to change.").build());
        getInventory().setItem(22, new ItemBuilder(enabled ? XMaterial.LIME_DYE : XMaterial.RED_DYE).setDisplayname("§3Status").setLore("§7Currently: " + (enabled ? "§aEnabled" : "§cDisabled"), "", "§eClick to toggle.").build());
        getInventory().setItem(24, new ItemBuilder(XMaterial.COMPARATOR).setDisplayname("§3Requirements").setLore("§cYou can change the requirements", "§cafter creating the new collection.", "", "§7All Requirements will be active", "§7on creating a new collection.").build());
        getInventory().setItem(44, new ItemBuilder(XMaterial.EMERALD_BLOCK).setDisplayname("§2Create").setLore("", "§eClick to create.").build());
        getInventory().setItem(36, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=")).setDisplayname("§eBack").build());
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (!event.getCurrentItem().getItemMeta().hasDisplayName()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        SoundManager soundManager = Main.getInstance().getSoundManager();

        FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(player.getUniqueId());
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
            case "Status":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                enabled = !enabled;
                Main.getInstance().getPlayerEggDataManager().savePlayerData(player.getUniqueId(), playerConfig);
                menuContent();
                break;
            case "Back":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new CollectionSelectMenu(Main.getPlayerMenuUtility(player)).open();
                break;
            case "Create":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                if (name == null) {
                    player.sendMessage("§cPlease enter a name to continue.");
                    return;
                }
                if (!Main.getInstance().getEggDataManager().containsSectionFile(name)) {
                    Main.getInstance().getEggDataManager().createEggCollectionFile(name, enabled);
                    new CollectionSelectMenu(Main.getPlayerMenuUtility(player)).open();
                    playerConfig.set("CollectionEdit", null);
                    Main.getInstance().getPlayerEggDataManager().savePlayerData(player.getUniqueId(), playerConfig);
                    Main.getInstance().getRequirementsManager().changeActivity(name, true);
                    Main.getInstance().getRequirementsManager().resetReset(name);
                    Main.getInstance().getEggManager().spawnEggParticle();
                } else
                    player.sendMessage("§cThe name of the collection is already chosen.");
                break;
        }
    }
}
