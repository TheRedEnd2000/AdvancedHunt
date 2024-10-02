package de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.individual;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.ItemHelper;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.ArrayList;

public class IndividualConfirmMenu extends PaginatedInventoryMenu {
    private MessageManager messageManager;
    private Main plugin;
    private String preset;
    private String id;
    private String collection;
    public IndividualConfirmMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Confirm loading", (short) 27);
        this.plugin = Main.getInstance();
        this.messageManager = this.plugin.getMessageManager();

        addMenuButtons();
    }

    public void open(String preset, String id, String collection) {
        this.collection = collection;
        this.preset = preset;
        this.id = id;

        getInventory().setContents(inventoryContent);

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    public void addMenuButtons() {
        inventoryContent[11] = new ItemBuilder(XMaterial.GREEN_CONCRETE)
                .setCustomId("rewards_individual_confirm.confirm")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REWARDS_CONFIRM_MENU_CONFIRM))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REWARDS_CONFIRM_MENU_CONFIRM))
                .build();
        inventoryContent[15] = new ItemBuilder(XMaterial.RED_CONCRETE)
                .setCustomId("rewards_individual_confirm.cancel")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REWARDS_CONFIRM_MENU_CANCEL))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REWARDS_CONFIRM_MENU_CANCEL))
                .build();
    }

    public int getMaxPages(){
        IndividualPresetDataManager presetDataManager = Main.getInstance().getIndividualPresetDataManager();
        ArrayList<String> keys = new ArrayList<>(presetDataManager.savedPresets());
        if(keys.isEmpty()) return 1;
        return (int) Math.ceil((double) keys.size() / maxItemsPerPage);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if(event.getCurrentItem() == null) return;

        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "rewards_individual_confirm.confirm":
                plugin.getIndividualPresetDataManager().loadPresetIntoAllEggs(preset,collection,player);
                player.closeInventory();
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case "rewards_individual_confirm.cancel":
                new IndividualPresetsMenu(super.playerMenuUtility).open(id, collection);
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
        }
    }
}
