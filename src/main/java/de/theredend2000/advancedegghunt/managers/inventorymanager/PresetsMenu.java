package de.theredend2000.advancedegghunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.PresetDataManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;

public class PresetsMenu extends PaginatedInventoryMenu {
    private MessageManager messageManager;
    private Main plugin;
    private String id;
    private String collection;
    public PresetsMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Presets", (short) 54);
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
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER).setDisplayname("§cClose").build();
        inventoryContent[45] = new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=")).setDisplayname("§eBack").build();
    }

    public void setMenuItems() {
        getInventory().setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Left")
                .setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19")).build());
        getInventory().setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Right")
                .setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ==")).build());

        PresetDataManager presetDataManager = Main.getInstance().getPresetDataManager();
        ArrayList<String> keys = new ArrayList<>();
        if(presetDataManager.savedPresets().size() >= 1){
            keys.addAll(presetDataManager.savedPresets());
        }else
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Presets").setLore("§7Create new one to select them.").build());
        if (keys == null || keys.isEmpty()) {
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Presets").setLore("§7Create new one to select them.").build());
            return;
        }
        for(int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if(index >= keys.size()) break;
            if (keys.get(index) != null){
                String defaultPreset = plugin.getPluginConfig().getDefaultLoadingPreset();
                getInventory().addItem(new ItemBuilder(XMaterial.PAPER).setDisplayname("§b§l" + keys.get(index)).setDefaultLore(presetDataManager.getAllCommandsAsLore(keys.get(index), keys.get(index).equals(defaultPreset))).setLocalizedName(keys.get(index)).build());
            }
        }
    }

    public int getMaxPages(){
        PresetDataManager presetDataManager = Main.getInstance().getPresetDataManager();
        ArrayList<String> keys = new ArrayList<>(presetDataManager.savedPresets());
        if(keys.isEmpty()) return 1;
        return (int) Math.ceil((double) keys.size() / maxItemsPerPage);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        PresetDataManager presetDataManager = Main.getInstance().getPresetDataManager();
        if(event.getCurrentItem() == null) return;

        for(String presetName : presetDataManager.savedPresets()){
            if(event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) continue;
            if (!ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals(presetName)) {
                continue;
            }
            switch (event.getAction()) {
                case PICKUP_ALL:
                    player.sendMessage(messageManager.getMessage(MessageKey.PRESET_LOADED).replaceAll("%PRESET%", presetName));
                    presetDataManager.loadPresetIntoEggCommands(presetName, collection, id);
                    new EggRewardsMenu(Main.getPlayerMenuUtility(super.playerMenuUtility.getOwner())).open(id, collection);
                    break;
                case PICKUP_HALF:
                    if (!plugin.getPluginConfig().getDefaultLoadingPreset().equals(presetName)) {
                        presetDataManager.deletePreset(presetName);
                        player.sendMessage(messageManager.getMessage(MessageKey.PRESET_DELETE).replaceAll("%PRESET%", presetName));
                    } else
                        player.sendMessage(messageManager.getMessage(MessageKey.PRESET_NOT_DELETE_DEFAULT).replaceAll("%PRESET%", presetName));
                    open(id, collection);
                    break;
                case CLONE_STACK:
                    plugin.getPluginConfig().setDefaultLoadingPreset(presetName);
                    player.sendMessage(messageManager.getMessage(MessageKey.PRESET_DEFAULT).replaceAll("%PRESET%", presetName));
                    open(id, collection);
                    break;
            }
            player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
            return;
        }

        XMaterial material = XMaterial.matchXMaterial(event.getCurrentItem());
        switch (material) {
            case BARRIER:
                player.closeInventory();
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case PLAYER_HEAD:
                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Left")) {
                    if (page == 0) {
                        player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                        player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    } else {
                        page = page - 1;
                        open(id, collection);
                        player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Right")) {
                    if (!((index + 1) >= presetDataManager.savedPresets().size())) {
                        page = page + 1;
                        open(id, collection);
                        player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    } else {
                        player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                        player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Back")) {
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    new EggRewardsMenu(Main.getPlayerMenuUtility(super.playerMenuUtility.getOwner())).open(id, collection);
                }
                break;
        }
    }
}

