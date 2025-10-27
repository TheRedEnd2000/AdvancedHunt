package de.theredend2000.advancedhunt.managers.inventorymanager.eggrewards.individual;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemHelper;
import de.theredend2000.advancedhunt.util.PlayerMenuUtility;
import de.theredend2000.advancedhunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;

public class IndividualPresetsMenu extends PaginatedInventoryMenu {
    private MessageManager messageManager;
    private Main plugin;
    private String id;
    private String collection;
    public IndividualPresetsMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Individual Presets", (short) 54);
        this.plugin = Main.getInstance();
        this.messageManager = this.plugin.getMessageManager();

        super.addMenuBorder();
        addMenuBorderButtons();
    }

    public void open(String id, String collection) {
        this.collection = collection;
        this.id = id;

        getInventory().setContents(inventoryContent);
        setMenuItems();

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    public void addMenuBorderButtons() {
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER)
                .setCustomId("rewards_individual_preset.close")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.CLOSE_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.CLOSE_BUTTON))
                .build();
        inventoryContent[45] = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("rewards_individual_preset.back")
                .setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0="))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.BACK_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.BACK_BUTTON))
                .build();
    }

    public void setMenuItems() {
        getInventory().setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("rewards_individual_preset.previous_page")
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.PREVIOUS_PAGE_BUTTON,"%CURRENT_PAGE%", String.valueOf(page + 1),"%MAX_PAGES%", String.valueOf(getMaxPages())))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.PREVIOUS_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19"))
                .build());
        getInventory().setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("rewards_individual_preset.next_page")
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.NEXT_PAGE_BUTTON,"%CURRENT_PAGE%", String.valueOf(page + 1),"%MAX_PAGES%", String.valueOf(getMaxPages())))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.NEXT_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ=="))
                .build());

        IndividualPresetDataManager presetDataManager = Main.getInstance().getIndividualPresetDataManager();
        ArrayList<String> keys = new ArrayList<>();
        if(!presetDataManager.savedPresets().isEmpty()){
            keys.addAll(presetDataManager.savedPresets());
        }
        if (keys.isEmpty() || presetDataManager.savedPresets().isEmpty()) {
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS)
                    .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.LIST_ERROR))
                    .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.LIST_ERROR))
                    .build());
            return;
        }
        for(int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if(index >= keys.size()) break;
            if (keys.get(index) != null){
                String defaultPreset = plugin.getPluginConfig().getDefaultIndividualLoadingPreset();
                getInventory().addItem(new ItemBuilder(XMaterial.PAPER)
                        .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.PRESET_INDIVIDUAL,"%PRESET%", keys.get(index)))
                        .setLore(presetDataManager.getAllCommandsAsLore(keys.get(index), keys.get(index).equals(defaultPreset)))
                        .setCustomId(keys.get(index))
                        .build());
            }
        }
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

        for(String presetName : presetDataManager.savedPresets()){
            if (!ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals(presetName)) {
                continue;
            }
            switch (event.getAction()) {
                case PICKUP_ALL:
                    player.sendMessage(messageManager.getMessage(MessageKey.PRESET_LOADED).replaceAll("%PRESET%", presetName));
                    presetDataManager.loadPresetIntoEggCommands(presetName, collection, id);
                    new IndividualEggRewardsMenu(Main.getPlayerMenuUtility(super.playerMenuUtility.getOwner())).open(id, collection);
                    break;
                case PICKUP_HALF:
                    if (!plugin.getPluginConfig().getDefaultIndividualLoadingPreset().equals(presetName)) {
                        presetDataManager.deletePreset(presetName);
                        player.sendMessage(messageManager.getMessage(MessageKey.PRESET_DELETE).replaceAll("%PRESET%", presetName));
                    } else
                        player.sendMessage(messageManager.getMessage(MessageKey.PRESET_NOT_DELETE_DEFAULT).replaceAll("%PRESET%", presetName));
                    open(id, collection);
                    break;
                case CLONE_STACK:
                    plugin.getPluginConfig().setDefaultIndividualLoadingPreset(presetName);
                    player.sendMessage(messageManager.getMessage(MessageKey.PRESET_DEFAULT).replaceAll("%PRESET%", presetName));
                    open(id, collection);
                    break;
                case DROP_ONE_SLOT:
                    new IndividualConfirmMenu(super.playerMenuUtility).open(presetName, id, collection);
                    break;
            }
            player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
            return;
        }

        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "rewards_individual_preset.close":
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), player::closeInventory,3L);
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case "rewards_individual_preset.back":
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                new IndividualEggRewardsMenu(Main.getPlayerMenuUtility(super.playerMenuUtility.getOwner())).open(id, collection);
                break;
            case "rewards_individual_preset.previous_page":
                if (page == 0) {
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                } else {
                    page = page - 1;
                    this.open(id, collection);
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
                break;
            case "rewards_individual_preset.next_page":
                if (!((index + 1) >= presetDataManager.savedPresets().size())) {
                    page = page + 1;
                    this.open(id, collection);
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                } else {
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
                break;
        }
    }
}

