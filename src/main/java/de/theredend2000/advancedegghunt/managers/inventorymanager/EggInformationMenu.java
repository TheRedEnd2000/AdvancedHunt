package de.theredend2000.advancedegghunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.ItemHelper;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public class EggInformationMenu extends PaginatedInventoryMenu {

    public EggInformationMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Egg information", (short) 54);

        super.addMenuBorder();
        addMenuBorderButtons();
    }

    public void open(String eggId) {
        Main.getInstance().setLastOpenedInventory(getInventory());
        getInventory().setContents(inventoryContent);
        setMenuItems(eggId);

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    public void addMenuBorderButtons(){
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER)
                .setCustomId("egg_info.close")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.CLOSE_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.CLOSE_BUTTON))
                .build();
        inventoryContent[53] = new ItemBuilder(XMaterial.EMERALD_BLOCK)
                .setCustomId("egg_info.refresh")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REFRESH_BUTTON))
                .setLore(menuMessageManager.getMenuItemName(MenuMessageKey.REFRESH_BUTTON))
                .build();
        inventoryContent[45] = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("egg_info.back")
                .setSkullOwner(Main.getTexture("NWYxMzNlOTE5MTlkYjBhY2VmZGMyNzJkNjdmZDg3YjRiZTg4ZGM0NGE5NTg5NTg4MjQ0NzRlMjFlMDZkNTNlNiJ9fX0="))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.BACK_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.BACK_BUTTON))
                .build();
        String selectedCollection = Main.getInstance().getPlayerEggDataManager().getPlayerData(playerMenuUtility.getOwner().getUniqueId()).getString("SelectedSection");
        inventoryContent[46] = new ItemBuilder(XMaterial.PAPER)
                .setCustomId("egg_info.collection_selected")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SELECTED_COLLECTION_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SELECTED_COLLECTION_BUTTON, "%COLLECTION%", selectedCollection))
                .build();
    }

    public void setMenuItems(String eggId) {
        getInventory().setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("egg_info.previous_page")
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.PREVIOUS_PAGE_BUTTON,"%CURRENT_PAGE%",String.valueOf(page + 1),"%MAX_PAGES%",String.valueOf(getMaxPages(eggId))))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.PREVIOUS_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19"))
                .build());
        getInventory().setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("egg_info.next_page")
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.NEXT_PAGE_BUTTON,"%CURRENT_PAGE%",String.valueOf(page + 1),"%MAX_PAGES%",String.valueOf(getMaxPages(eggId))))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.NEXT_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ=="))
                .build());

        ArrayList<String> keys = new ArrayList<>();
        ArrayList<String> uuid = new ArrayList<>();
        String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        for(UUID uuids : Main.getInstance().getEggDataManager().savedPlayers()){
            FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids);
            if(playerConfig.contains("FoundEggs." + collection + "." + eggId)){
                Collections.addAll(keys, playerConfig.getString("FoundEggs." + collection + ".Name"));
                Collections.addAll(uuid, String.valueOf(uuids));
            }
        }

        if(keys != null && !keys.isEmpty()) {
            for(int i = 0; i < getMaxItemsPerPage(); i++) {
                index = getMaxItemsPerPage() * page + i;
                if(index >= keys.size()) break;
                if (keys.get(index) != null){
                    String maxEggs = String.valueOf(Main.getInstance().getEggManager().getMaxEggs(collection));
                    String date = Main.getInstance().getEggManager().getEggDateCollected(uuid.get(index), eggId, collection);
                    String time = Main.getInstance().getEggManager().getEggTimeCollected(uuid.get(index), eggId, collection);
                    String eggsFound = Main.getInstance().getPlayerEggDataManager().getPlayerData(UUID.fromString(uuid.get(index))).getString("FoundEggs." + collection + ".Count");
                    getInventory().addItem(new ItemBuilder(XMaterial.PLAYER_HEAD)
                            .setOwner(keys.get(index))
                            .setDisplayName("§6§l" + keys.get(index) + " §7(" + uuid.get(index) + ")")
                            .setLore("§7" + keys.get(index) + " has found the §2egg #" + eggId + "§7.", "", "§9Information of " + keys.get(index) + ":", "§7Eggs found: §6" + eggsFound + "/" + maxEggs, "", "§9Collected:", "§7Date: §6" + date, "§7Time: §6" + time)
                            .setCustomId(keys.get(index))
                            .build());
                }
            }
        }else
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS)
                    .setDisplayName("§4§lNo Founds")
                    .setLore("§7No player has found this egg yet.")
                    .build());
    }


    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String id = ItemHelper.getItemId(getInventory().getItem(0));
        ArrayList<String> keys = new ArrayList<>();
        String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        for(UUID uuids : Main.getInstance().getEggDataManager().savedPlayers()){
            if(Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids).contains("FoundEggs." + id)){
                Collections.addAll(keys, Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids).getString("FoundEggs." + collection + ".Name"));
            }
        }

        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "egg_info.back":
                new EggListMenu(Main.getPlayerMenuUtility(player)).open();
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case "egg_info.close":
                player.closeInventory();
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case "egg_info.refresh":
                if (Main.getInstance().getRefreshCooldown().containsKey(player.getName())) {
                    if (Main.getInstance().getRefreshCooldown().get(player.getName()) > System.currentTimeMillis()) {
                        player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.WAIT_REFRESH));
                        player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                        return;
                    }
                }
                Main.getInstance().getRefreshCooldown().put(player.getName(), System.currentTimeMillis() + (3 * 1000));
                this.open(ItemHelper.getItemId(getInventory().getItem(0)));
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case "egg_info.collection_selected":
                new CollectionSelectMenu(Main.getPlayerMenuUtility(player)).open();
                return;
            case "egg_info.previous_page":
                if (page == 0) {
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                } else {
                    page = page - 1;
                    this.open(id);
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
                break;
            case "egg_info.next_page":
                if (!((index + 1) >= keys.size())) {
                    page = page + 1;
                    this.open(id);
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                } else {
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
                break;
        }
    }

    public int getMaxItemsPerPage() {
        return maxItemsPerPage;
    }

    public int getMaxPages(String eggId){
        ArrayList<String> keys = new ArrayList<>();
        for(UUID uuids : Main.getInstance().getEggDataManager().savedPlayers()){
            FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids);
            if(playerConfig.contains("FoundEggs." + eggId)){
                Collections.addAll(keys, playerConfig.getString("FoundEggs.Name"));
            }
        }
        if(keys.isEmpty()) return 1;
        return (int) Math.ceil((double) keys.size() / getMaxItemsPerPage());
    }
}

