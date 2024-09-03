package de.theredend2000.advancedegghunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.SoundManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.collection.CollectionCreator;
import de.theredend2000.advancedegghunt.managers.inventorymanager.collection.CollectionEditor;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.ItemHelper;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.enums.Permission;
import de.theredend2000.advancedegghunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Objects;

public class CollectionSelectMenu extends PaginatedInventoryMenu {

    public CollectionSelectMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Select collection", (short) 54);

        super.addMenuBorder();
        addMenuBorderButtons();
    }

    public void open() {
        getInventory().setContents(inventoryContent);
        setMenuItems();

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    public void addMenuBorderButtons() {
        if(Main.getInstance().getPermissionManager().checkPermission(playerMenuUtility.getOwner(), Permission.CreateCollection))
            inventoryContent[51] = new ItemBuilder(XMaterial.PLAYER_HEAD)
                    .setDisplayName(menuMessageManager.getMenuMessage(MenuMessageKey.ADD_COLLECTION_BUTTON))
                    .setSkullOwner(Main.getTexture("NWQ4NjA0YjllMTk1MzY3Zjg1YTIzZDAzZDlkZDUwMzYzOGZjZmIwNWIwMDMyNTM1YmM0MzczNDQyMjQ4M2JkZSJ9fX0="))
                    .build();

        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER)
                .setDisplayName(menuMessageManager.getMenuMessage(MenuMessageKey.CLOSE_BUTTON))
                .build();
        inventoryContent[53] = new ItemBuilder(XMaterial.EMERALD_BLOCK)
                .setDisplayName(menuMessageManager.getMenuMessage(MenuMessageKey.REFRESH_BUTTON))
                .build();
    }

    public void setMenuItems() {
        getInventory().setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.")
                .setDisplayName(menuMessageManager.getMenuMessage(MenuMessageKey.PREVIOUS_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19"))
                .build());
        getInventory().setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.")
                .setDisplayName(menuMessageManager.getMenuMessage(MenuMessageKey.NEXT_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ=="))
                .build());

        ArrayList<String> keys = new ArrayList<>(Main.getInstance().getEggDataManager().savedEggCollections());
        if(keys.isEmpty()){
            playerMenuUtility.getOwner().closeInventory();
            Main.getInstance().getMessageManager().sendMessage(playerMenuUtility.getOwner(), MessageKey.COLLECTION_SELECT_ERROR);
            return;
        }

        for(int i = 0; i < getMaxItemsPerPage(); i++) {
            index = getMaxItemsPerPage() * page + i;
            int slotIndex = ((9 + 1) + ((i / 7) * 9) + (i % 7));
            if(index >= keys.size()) break;
            if (keys.get(index) == null) {
                continue;
            }

            String selectedSection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
            int maxEggs = Main.getInstance().getEggManager().getMaxEggs(keys.get(index));
            boolean applied = selectedSection.equals(keys.get(index));
            boolean permission = Main.getInstance().getPermissionManager().checkPermission(playerMenuUtility.getOwner(), Permission.ChangeCollections);
            getInventory().setItem(slotIndex, new ItemBuilder(XMaterial.PAPER)
                    .withGlow(applied)
                    .setDisplayName(menuMessageManager.getMenuMessage(MenuMessageKey.CLOSE_BUTTON))
                    .setLore("", "§9Collection Information:", "§7   - Placed eggs: §6" + maxEggs, "", "§aNote:", "§7This collection applies to all actions that are carried out.", "§7It can be changed at any time in this menu.", "", "§eLEFT-CLICK to select.", permission ? "§eRIGHT-CLICK to edit." : "§7§mRIGHT-CLICK to edit.")
                    .setCustomId(keys.get(index))
                    .build());
        }
    }

    public int getMaxItemsPerPage() {
        return maxItemsPerPage;
    }

    public int getMaxPages(){
        int keys = Main.getInstance().getEggDataManager().savedEggCollections().size();
        if(keys == 0) return 1;
        return (int) Math.ceil((double) keys / getMaxItemsPerPage());
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        String selectedCollection = ItemHelper.getItemId(Objects.requireNonNull(event.getCurrentItem()));
        SoundManager soundManager = Main.getInstance().getSoundManager();
        Player player = (Player) event.getWhoClicked();
        MessageManager messageManager = Main.getInstance().getMessageManager();

        ArrayList<String> keys = new ArrayList<>(Main.getInstance().getEggDataManager().savedEggCollections());
        for(String collection : keys){
            if (!ItemHelper.hasItemId(event.getCurrentItem()) ||
                    !ItemHelper.getItemId(event.getCurrentItem()).equals(collection)) {
                continue;
            }
            switch (event.getAction()) {
                case PICKUP_ALL:
                    Main.getInstance().getPlayerEggDataManager().savePlayerCollection(player.getUniqueId(), collection);
                    player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    open();
                    messageManager.sendMessage(player, MessageKey.COLLECTION_SELECTION, "%SELECTION%", collection);
                    return;
                case PICKUP_HALF:
                    if (Main.getInstance().getPermissionManager().checkPermission(player, Permission.ChangeCollections)) {
                        new CollectionEditor(Main.getPlayerMenuUtility(player)).open(selectedCollection);
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    } else {
                        messageManager.sendMessage(player, MessageKey.COLLECTION_EDIT_NO_PERMISSION);
                        player.playSound(player.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                    }
                    return;
            }
        }

        XMaterial material = XMaterial.matchXMaterial(event.getCurrentItem());
        switch (material) {
            case BARRIER:
                player.closeInventory();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case EMERALD_BLOCK:
                if (Main.getInstance().getRefreshCooldown().containsKey(player.getName())) {
                    if (Main.getInstance().getRefreshCooldown().get(player.getName()) > System.currentTimeMillis()) {
                        messageManager.sendMessage(player, MessageKey.COLLECTION_REFRESH_COOLDOWN);
                        player.playSound(player.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                        return;
                    }
                }
                Main.getInstance().getRefreshCooldown().put(player.getName(), System.currentTimeMillis() + (3 * 1000));
                open();
                messageManager.sendMessage(player, MessageKey.COLLECTION_REFRESH_SUCCESS);
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case PLAYER_HEAD:
                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Left")) {
                    if (page == 0) {
                        messageManager.sendMessage(player, MessageKey.FIRST_PAGE);
                        player.playSound(player.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                    } else {
                        page = page - 1;
                        open();
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Right")) {
                    if (!((index + 1) >= keys.size())) {
                        page = page + 1;
                        open();
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    } else {
                        messageManager.sendMessage(player, MessageKey.LAST_PAGE);
                        player.playSound(player.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Add collection")) {
                    if(Main.getInstance().getPermissionManager().checkPermission(player, Permission.CreateCollection)) {
                        new CollectionCreator(Main.getPlayerMenuUtility(player)).open();
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    } else {
                        messageManager.sendMessage(player, MessageKey.PERMISSION_ERROR, "%PERMISSION%", Permission.CreateCollection.toString());
                        player.playSound(player.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                    }
                }
                break;
        }
    }
}

