package de.theredend2000.advancedhunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.SoundManager;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.IInventoryMenuOpen;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemHelper;
import de.theredend2000.advancedhunt.util.PlayerMenuUtility;
import de.theredend2000.advancedhunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.ArrayList;

public class EggProgressMenu extends PaginatedInventoryMenu implements IInventoryMenuOpen {

    public EggProgressMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Egg progress", (short) 54);

        super.addMenuBorder();
        addMenuBorderButtons();
    }

    public void open() {
        Main.getInstance().setLastOpenedInventory(getInventory(), playerMenuUtility.getOwner());
        getInventory().setContents(inventoryContent);
        setMenuItems();

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    public void addMenuBorderButtons() {
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER)
                .setCustomId("egg_progress.close")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.CLOSE_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.CLOSE_BUTTON))
                .build();
        inventoryContent[53] = new ItemBuilder(XMaterial.EMERALD_BLOCK)
                .setCustomId("egg_progress.refresh")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REFRESH_BUTTON))
                .setLore(menuMessageManager.getMenuItemName(MenuMessageKey.REFRESH_BUTTON))
                .build();
        String selectedSection = Main.getInstance().getPlayerEggDataManager().getPlayerData(playerMenuUtility.getOwner().getUniqueId()).getString("SelectedSection");
        inventoryContent[45] = new ItemBuilder(XMaterial.PAPER)
                .setCustomId("egg_progress.collection_selected")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SELECTED_COLLECTION_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SELECTED_COLLECTION_BUTTON, "%COLLECTION%", selectedSection))
                .build();
    }

    public void setMenuItems() {
        getInventory().setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("egg_progress.previous_page")
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.PREVIOUS_PAGE_BUTTON,"%CURRENT_PAGE%", String.valueOf(page + 1),"%MAX_PAGES%", String.valueOf(getMaxPages())))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.PREVIOUS_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19"))
                .build());
        getInventory().setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("egg_progress.next_page")
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.NEXT_PAGE_BUTTON,"%CURRENT_PAGE%", String.valueOf(page + 1),"%MAX_PAGES%", String.valueOf(getMaxPages())))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.NEXT_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ=="))
                .build());

        String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        addMenuBorderButtons();
        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("PlacedEggs.")){
            keys.addAll(placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false));
        }else
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS)
                    .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.LIST_ERROR))
                    .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.LIST_ERROR))
                    .build());

        if (keys == null || keys.isEmpty()) {
            return;
        }

        for(int i = 0; i < getMaxItemsPerPage(); i++) {
            index = getMaxItemsPerPage() * page + i;
            if(index >= keys.size()) break;
            if (keys.get(index) == null) {
                continue;
            }
            int slotIndex = ((9 + 1) + ((i / 7) * 9) + (i % 7));

            boolean showCoordinates = Main.getInstance().getPluginConfig().getShowCoordinatesWhenEggFoundInProgressInventory();
            String x = placedEggs.getString("PlacedEggs." + keys.get(index) + ".X");
            String y = placedEggs.getString("PlacedEggs." + keys.get(index) + ".Y");
            String z = placedEggs.getString("PlacedEggs." + keys.get(index) + ".Z");
            boolean hasFound = Main.getInstance().getEggManager().hasFound(playerMenuUtility.getOwner(), keys.get(index), collection);
            String date = Main.getInstance().getEggManager().getEggDateCollected(playerMenuUtility.getOwner().getUniqueId().toString(), keys.get(index), collection);
            String time = Main.getInstance().getEggManager().getEggTimeCollected(playerMenuUtility.getOwner().getUniqueId().toString(), keys.get(index), collection);
            XMaterial item = Main.getInstance().getEggManager().getBlockMaterialOfEgg(keys.get(index), collection);
            boolean isSkull = item == XMaterial.PLAYER_HEAD || item == XMaterial.PLAYER_WALL_HEAD;
            String texture = Main.getInstance().getEggManager().getHeadTextureValue(keys.get(index), collection);
            if(showCoordinates && hasFound){
                getInventory().setItem(slotIndex, new ItemBuilder(item)
                .setSkullOwner(isSkull ? texture : "")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.EGGPROGRESS_LOCATION_FOUND,"%ID%", keys.get(index)))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.EGGPROGRESS_LOCATION_FOUND,"%LOCATION_X%", x,"%LOCATION_Y%", y,"%LOCATION_Z%", z,"%DATE%", date,"%TIME%", time))
                .setCustomId(keys.get(index))
                .build());
            }else if(hasFound && !showCoordinates) {
                getInventory().setItem(slotIndex, new ItemBuilder(item)
                        .setSkullOwner(isSkull ? texture : "")
                        .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.EGGPROGRESS_FOUND,"%ID%", keys.get(index)))
                        .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.EGGPROGRESS_FOUND,"%DATE%", date,"%TIME%", time))
                        .setCustomId(keys.get(index))
                        .build());
            }else
                getInventory().setItem(slotIndex, new ItemBuilder(item)
                        .setSkullOwner(isSkull ? texture : "")
                        .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.EGGPROGRESS_NOT_FOUND,"%ID%", keys.get(index)))
                        .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.EGGPROGRESS_NOT_FOUND))
                        .setCustomId(keys.get(index))
                        .build());
        }
    }

    public int getMaxItemsPerPage() {
        return maxItemsPerPage;
    }

    public int getMaxPages(){
        String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        int keys = Main.getInstance().getEggManager().getMaxEggs(collection);
        if(keys == 0) return 1;
        return (int) Math.ceil((double) keys / getMaxItemsPerPage());
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        SoundManager soundManager = Main.getInstance().getSoundManager();
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        Player player = (Player) event.getWhoClicked();

        ArrayList<String> keys = new ArrayList<>();
        if (placedEggs.contains("PlacedEggs.")){
            keys.addAll(placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false));
        }

        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "egg_progress.close":
                player.closeInventory();
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case "egg_progress.refresh":
                if (Main.getInstance().getRefreshCooldown().containsKey(player.getName())) {
                    if (Main.getInstance().getRefreshCooldown().get(player.getName()) > System.currentTimeMillis()) {
                        player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.WAIT_REFRESH));
                        player.playSound(player.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                        return;
                    }
                }
                Main.getInstance().getRefreshCooldown().put(player.getName(), System.currentTimeMillis() + (3 * 1000));
                open();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "egg_progress.collection_selected":
                new CollectionSelectMenu(Main.getPlayerMenuUtility(player)).open();
                return;
            case "egg_progress.previous_page":
                if (page == 0) {
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                } else {
                    page = page - 1;
                    this.open();
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
                break;
            case "egg_progress.next_page":
                if (!((index + 1) >= keys.size())) {
                    page = page + 1;
                    this.open();
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                } else {
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
                break; 
        }
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Main.getInstance().setLastOpenedInventory(getInventory(), playerMenuUtility.getOwner());
        getInventory().setContents(inventoryContent);
        setMenuItems();
    }
}