package de.theredend2000.advancedhunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.SoundManager;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.IInventoryMenuOpen;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedhunt.util.ConfigLocationUtil;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemHelper;
import de.theredend2000.advancedhunt.util.PlayerMenuUtility;
import de.theredend2000.advancedhunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class EggListMenu extends PaginatedInventoryMenu implements IInventoryMenuOpen {

    public EggListMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, StringUtils.capitalize(Main.getInstance().getPluginConfig().getPluginNamePlural()) + "s list", (short) 54);

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
                .setCustomId("egg_list.close")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.CLOSE_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.CLOSE_BUTTON))
                .build();
        inventoryContent[53] = new ItemBuilder(XMaterial.EMERALD_BLOCK)
                .setCustomId("egg_list.refresh")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REFRESH_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REFRESH_BUTTON))
                .build();

        String selectedSection = Main.getInstance().getPlayerEggDataManager().getPlayerData(playerMenuUtility.getOwner().getUniqueId()).getString("SelectedSection");
        inventoryContent[45] = new ItemBuilder(XMaterial.PAPER)
                .setCustomId("egg_list.collection_selected")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SELECTED_COLLECTION_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SELECTED_COLLECTION_BUTTON, "%COLLECTION%", selectedSection))
                .build();
    }

    public void setMenuItems() {
        getInventory().setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("egg_list.previous_page")
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.PREVIOUS_PAGE_BUTTON,"%CURRENT_PAGE%", String.valueOf(page + 1),"%MAX_PAGES%", String.valueOf(getMaxPages())))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.PREVIOUS_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19"))
                .build());
        getInventory().setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("egg_list.next_page")
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.NEXT_PAGE_BUTTON,"%CURRENT_PAGE%", String.valueOf(page + 1),"%MAX_PAGES%", String.valueOf(getMaxPages())))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.NEXT_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ=="))
                .build());

        String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
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
            String x = placedEggs.getString("PlacedEggs." + keys.get(index) + ".X");
            String y = placedEggs.getString("PlacedEggs." + keys.get(index) + ".Y");
            String z = placedEggs.getString("PlacedEggs." + keys.get(index) + ".Z");
            String date = Main.getInstance().getEggManager().getEggDatePlaced(keys.get(index), collection);
            String time = Main.getInstance().getEggManager().getEggTimePlaced(keys.get(index), collection);
            int timesFound = Main.getInstance().getEggManager().getTimesFound(keys.get(index), collection);
            int slotIndex = ((9 + 1) + ((i / 7) * 9) + (i % 7));
            ItemStack item = Main.getInstance().getEggManager().getBlockMaterialOfEgg(keys.get(index), collection);
            boolean isSkull = XMaterial.matchXMaterial(item.getType()) == XMaterial.PLAYER_HEAD || XMaterial.matchXMaterial(item.getType()) == XMaterial.PLAYER_WALL_HEAD;
            String texture = Main.getInstance().getEggManager().getHeadTextureValue(keys.get(index), collection);
            getInventory().setItem(slotIndex, new ItemBuilder(item)
                    .setCustomId("egg_list.id." + keys.get(index))
                    .setSkullOwner(isSkull ? texture : "")
                    .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.EGGSLIST_EGG,"%TREASURE_ID%", keys.get(index)))
                    .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.EGGSLIST_EGG,"%LOCATION_X%", x,"%LOCATION_Y%", y,"%LOCATION_Z%", z,"%TIMES_FOUND%", String.valueOf(timesFound),"%DATE%", date,"%TIME%", time))
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
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        SoundManager soundManager = Main.getInstance().getSoundManager();
        Player player = (Player) event.getWhoClicked();

        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("PlacedEggs.")){
            keys.addAll(placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false));
            for(String id : placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false)){
                if (!ItemHelper.hasItemId(event.getCurrentItem()) ||
                        !ItemHelper.getItemId(event.getCurrentItem()).equals(id)) {
                    continue;
                }
                if(event.getAction() == InventoryAction.PICKUP_ALL){
                    ConfigLocationUtil location = new ConfigLocationUtil(Main.getInstance(), "PlacedEggs." + id);
                    if (location.loadLocation(collection) != null)
                        player.teleport(location.loadLocation(collection).add(0.5, 0, 0.5));
                    player.closeInventory();
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.TELEPORT_TO_EGG).replaceAll("%ID%", id));
                    player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    return;
                }else if(event.getAction() == InventoryAction.PICKUP_HALF){
                    new EggInformationMenu(Main.getPlayerMenuUtility(player)).open(id);
                    player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    return;
                }
            }
        }

        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "egg_list.close":
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), player::closeInventory,3L);
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case "egg_list.refresh":
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
            case "egg_list.collection_selected":
                new CollectionSelectMenu(Main.getPlayerMenuUtility(player)).open();
                return;
            case "egg_list.previous_page":
                if (page == 0) {
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                } else {
                    page = page - 1;
                    this.open();
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
                break;
            case "egg_list.next_page":
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

