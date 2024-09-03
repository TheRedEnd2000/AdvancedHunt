package de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.individual;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

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

        getInventory().setContents(inventoryContent);

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    public void addMenuButtons() {
        inventoryContent[11] = new ItemBuilder(XMaterial.GREEN_CONCRETE)
                .setDisplayName("§aConfirm")
                .setLore("","§7By clicking this button you will load","§7this preset into all placed eggs.","","§eClick to confirm.")
                .build();
        inventoryContent[15] = new ItemBuilder(XMaterial.RED_CONCRETE)
                .setDisplayName("§cCancel")
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
        IndividualPresetDataManager presetDataManager = Main.getInstance().getIndividualPresetDataManager();
        if(event.getCurrentItem() == null) return;

        XMaterial material = XMaterial.matchXMaterial(event.getCurrentItem());
        switch (material) {
            case RED_CONCRETE:
                new IndividualPresetsMenu(super.playerMenuUtility).open(id, collection);
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case GREEN_CONCRETE:
                plugin.getIndividualPresetDataManager().loadPresetIntoAllEggs(preset,collection,player);
                player.closeInventory();
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
        }
    }
}
