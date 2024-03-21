package de.theredend2000.advancedegghunt.managers.inventorymanager.collection;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.SoundManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.CollectionSelectMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.ResetMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.InventoryMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.requirements.RequirementSelection;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.enums.DeletionTypes;
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
        inventoryContent[36] = new ItemBuilder(XMaterial.PLAYER_HEAD).setDisplayname("§eBack").setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=")).build();
        inventoryContent[40] = new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build();
    }

    private void menuContent(String collection) {
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        boolean enabled = placedEggs.getBoolean("Enabled");
        DeletionTypes deletionType = Main.getInstance().getPlayerEggDataManager().getDeletionType(super.playerMenuUtility.getOwner().getUniqueId());

        getInventory().setItem(4, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(new Random().nextInt(7))).setDisplayname("§6" + collection).build());
        //inventory.setItem(20, new ItemBuilder(XMaterial.PAPER).setDisplayname("§3Rename").setLore("§7Currently: " + name, "", "§eClick to change.").build());
        getInventory().setItem(20, new ItemBuilder(enabled ? XMaterial.LIME_DYE : XMaterial.RED_DYE).setDisplayname("§3Status").setLore("§7Currently: " + (enabled ? "§aEnabled" : "§cDisabled"), "", "§eClick to toggle.").build());
        getInventory().setItem(24, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4Delete").setLore("§8Check if your deletion type is correct. (WOODEN_AXE)", "", "§4§lYOU CAN NOT UNDO THIS", "", "§eClick to delete.").build());
        getInventory().setItem(13, new ItemBuilder(XMaterial.COMPARATOR).setDisplayname("§3Requirements").setDefaultLore(Main.getInstance().getRequirementsManager().getListRequirementsLore(collection)).build());
        getInventory().setItem(31, new ItemBuilder(XMaterial.REPEATER).setDisplayname("§3Reset §e§l(BETA)").setLore("", "§cResets after:", "§6  " + Main.getInstance().getRequirementsManager().getConvertedTime(collection), "", "§4If the time get changed, the value", "§4of the current cooldown of the", "§4player will not change!", "", "§eClick to change.").build());

        getInventory().setItem(44,  new ItemBuilder(XMaterial.WOODEN_AXE).setDisplayname("§3Deletion Types")
                .setLore("§8Every player can configure that himself.",
                        "§7Change what happens after the deletion",
                        "§7of an collection.",
                        "",
                        (deletionType == DeletionTypes.Noting ? "§b➤ " : "§7") + "Nothing", "§8All blocks that were eggs will stay. (includes player heads)",
                        (deletionType == DeletionTypes.Player_Heads ? "§b➤ " : "§7") + "Player Heads", "§8All blocks that are player heads will be removed.",
                        (deletionType == DeletionTypes.Everything ? "§b➤ " : "§7") + "Everything", "§8All blocks and will be set to air. (includes player heads)",
                        "",
                        "§eClick to change.").build());
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
        switch (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName())) {
            case "Close":
                player.closeInventory();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "Status":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                placedEggs.set("Enabled", !enabled);
                Main.getInstance().getEggDataManager().savePlacedEggs(collection, placedEggs);
                menuContent(collection);
                break;
            case "Requirements":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new RequirementSelection(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "Reset (BETA)":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new ResetMenu(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "Back":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new CollectionSelectMenu(Main.getPlayerMenuUtility(player)).open();
                break;
            case "Delete":
                if (collection.equalsIgnoreCase("default")) {
                    player.sendMessage("§cBecause of many issues it is not possible to delete the default section.\n§cIf you want to disable it please just change the status.");
                    return;
                }
                Main.getInstance().getRequirementsManager().removeAllEggBlocks(collection, player.getUniqueId());
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                player.sendMessage(messageManager.getMessage(MessageKey.COLLECTION_DELETED).replaceAll("%COLLECTION%", collection));
                for (UUID uuids : Main.getInstance().getEggDataManager().savedPlayers()) {
                    FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids);
                    playerConfig.set("FoundEggs." + collection, null);
                    playerConfig.set("SelectedSection", "default");
                    Main.getInstance().getPlayerEggDataManager().savePlayerData(uuids, playerConfig);
                }
                Main.getInstance().getEggDataManager().deleteCollection(collection);
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
                menuContent(collection);
                break;
        }
    }

    @Override
    public String getMenuName() {
        return null;
    }

    @Override
    public int getSlots() {
        return this.slots;
    }
}
