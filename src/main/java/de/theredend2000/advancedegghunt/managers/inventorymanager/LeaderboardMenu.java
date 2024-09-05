package de.theredend2000.advancedegghunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.SoundManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.ItemHelper;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.enums.LeaderboardSortTypes;
import de.theredend2000.advancedegghunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.*;

public class LeaderboardMenu extends PaginatedInventoryMenu {
    private int numberOfPlayers = 0;

    public LeaderboardMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Eggs leaderboard", (short) 54);

        super.addMenuBorder();
        addMenuBorderButtons();
    }

    public void open() {
        Main.getInstance().setLastOpenedInventory(getInventory(),playerMenuUtility.getOwner());
        page = 0;
        getInventory().setContents(inventoryContent);
        setMenuItems();

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    public void reopen() {
        getInventory().setContents(inventoryContent);
        setMenuItems();
    }

    public void addMenuBorderButtons() {
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER)
                .setCustomId("leaderboard.close")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.CLOSE_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.CLOSE_BUTTON))
                .build();
        inventoryContent[53] = new ItemBuilder(XMaterial.EMERALD_BLOCK)
                .setCustomId("leaderboard.refresh")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REFRESH_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REFRESH_BUTTON))
                .build();

        String selectedCollection = Main.getInstance().getPlayerEggDataManager().getPlayerData(playerMenuUtility.getOwner().getUniqueId()).getString("SelectedSection");
        inventoryContent[45] = new ItemBuilder(XMaterial.PAPER)
                .setCustomId("leaderboard.collection_selected")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SELECTED_COLLECTION_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SELECTED_COLLECTION_BUTTON, "%COLLECTION%", selectedCollection))
                .build();
    }

    public void setMenuItems() {
        getInventory().setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("leaderboard.previous_page")
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.PREVIOUS_PAGE_BUTTON,"%CURRENT_PAGE%",String.valueOf(page + 1),"%MAX_PAGES%",String.valueOf(getMaxPages())))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.PREVIOUS_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19"))
                .build());
        getInventory().setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("leaderboard.next_page")
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.NEXT_PAGE_BUTTON,"%CURRENT_PAGE%",String.valueOf(page + 1),"%MAX_PAGES%",String.valueOf(getMaxPages())))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.NEXT_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ=="))
                .build());

        String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        addMenuBorderButtons();

        LeaderboardSortTypes sortTypes = Main.getInstance().getSortTypeLeaderboard().get(playerMenuUtility.getOwner());
        ItemBuilder itemBuilder = new ItemBuilder(XMaterial.HOPPER)
                .setCustomId("leaderboard.sort")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.LEADERBOARD_SORT));
        switch (sortTypes){
            case ALL:
                itemBuilder = itemBuilder.setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.LEADERBOARD_SORT,"%SELECTED_ALL%","§6➤ ","%SELECTED_10%","§7","%SELECTED_3%","§7","%SELECTED_YOU%","§7"));
                break;
            case TOP10:
                itemBuilder = itemBuilder.setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.LEADERBOARD_SORT,"%SELECTED_ALL%","§7","%SELECTED_10%","§6➤ ","%SELECTED_3%","§7","%SELECTED_YOU%","§7"));
                break;
            case TOP3:
                itemBuilder = itemBuilder.setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.LEADERBOARD_SORT,"%SELECTED_ALL%","§7","%SELECTED_10%","§7","%SELECTED_3%","§6➤ ","%SELECTED_YOU%","§7"));
                break;
            case YOU:
                itemBuilder = itemBuilder.setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.LEADERBOARD_SORT,"%SELECTED_ALL%","§7","%SELECTED_10%","§7","%SELECTED_3%","§7","%SELECTED_YOU%","§6➤ "));
                break;
        }
        getInventory().setItem(51, itemBuilder.build());

        if (Main.getInstance().getEggDataManager().savedPlayers().isEmpty()) {
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS)
                .setDisplayName("§4§lNo Player")
                .setLore("§7There are no players in the leaderboard.")
                .build());
            return;
        }

        HashMap<String, Integer> leaderboard = new HashMap<>();

        for(UUID uuid : Main.getInstance().getEggDataManager().savedPlayers()) {
            FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuid);
            if (!playerConfig.contains("FoundEggs") || !playerConfig.contains("FoundEggs." + collection)) continue;

            leaderboard.put(playerConfig.getString("FoundEggs." + collection + ".Name"), playerConfig.getInt("FoundEggs." + collection + ".Count"));
        }

        List<Map.Entry<String, Integer>> leaderList = new ArrayList<>(leaderboard.entrySet());
        numberOfPlayers = leaderList.size();

        if (leaderList.isEmpty()) {
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS)
                    .setDisplayName("§4§lNo Player")
                    .setLore("§7There are no players in the leaderboard.")
                    .build());
            return;
        }

        leaderList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        int maxEggs = Main.getInstance().getEggManager().getMaxEggs(collection);

        if (sortTypes.equals(LeaderboardSortTypes.YOU)) {
            index = 0;
            numberOfPlayers = 1;

            for(int i = 0; i < leaderboard.size(); i++) {
                String playerName = leaderList.get(i).getKey();
                if (!playerName.equals(playerMenuUtility.getOwner().getName())) {
                    continue;
                }

                int count = leaderList.get(i).getValue();
                getInventory().addItem(new ItemBuilder(XMaterial.PLAYER_HEAD)
                        .setOwner(playerName)
                        .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.LEADERBOARD_PLAYER,"%PLACE%",String.valueOf(index+1),"%PLAYER_NAME%",playerName,"%PLAYER_HIMSELF%",(playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : "")))
                        .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.LEADERBOARD_PLAYER,"%EGGS_FOUND%", String.valueOf(count),"%EGGS_REMAINING%",String.valueOf(maxEggs-count),"%EGGS_MAX%",String.valueOf(maxEggs),"%IS_IN_TOP_TEN%",(9 >= index ? "§eTHIS PLAYER IS IN THE TOP 10!" : "§c" + (index - 9) + " place behind 10th place")))
                        .build());

                return;
            }
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS)
                    .setDisplayName("§4§lNo Player")
                    .setLore("§7There are no players in the leaderboard.")
                    .build());
            return;
        }

        for(int i = 0; i < getMaxItemsPerPage(); i++) {
            index = (getMaxItemsPerPage() * page) + i;
            if(index >= leaderboard.size()) break;
            int slotIndex = ((9 + 1) + ((i / 7) * 9) + (i % 7));

            String playerName = leaderList.get(index).getKey();
            int count = leaderList.get(index).getValue();

            switch (sortTypes) {
                case ALL:
                    getInventory().setItem(slotIndex, new ItemBuilder(XMaterial.PLAYER_HEAD).setOwner(playerName)
                            .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.LEADERBOARD_PLAYER,"%PLACE%",String.valueOf(index+1),"%PLAYER_NAME%",playerName,"%PLAYER_HIMSELF%",(playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : "")))
                            .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.LEADERBOARD_PLAYER,"%EGGS_FOUND%", String.valueOf(count),"%EGGS_REMAINING%",String.valueOf(maxEggs-count),"%EGGS_MAX%",String.valueOf(maxEggs),"%IS_IN_TOP_TEN%",(9 >= index ? "§eTHIS PLAYER IS IN THE TOP 10!" : "§c" + (index - 9) + " place behind 10th place")))
                            .build());
                    break;
                case TOP3:
                    numberOfPlayers = 3;
                    if (i < 3) {
                        getInventory().setItem(slotIndex, new ItemBuilder(XMaterial.PLAYER_HEAD).setOwner(playerName)
                                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.LEADERBOARD_PLAYER,"%PLACE%",String.valueOf(index+1),"%PLAYER_NAME%",playerName,"%PLAYER_HIMSELF%",(playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : "")))
                                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.LEADERBOARD_PLAYER,"%EGGS_FOUND%", String.valueOf(count),"%EGGS_REMAINING%",String.valueOf(maxEggs-count),"%EGGS_MAX%",String.valueOf(maxEggs),"%IS_IN_TOP_TEN%","§eTHIS PLAYER IS IN THE TOP 10!"))
                                .build());
                    }
                    break;
                case TOP10:
                    numberOfPlayers = 10;
                    if (i < 10) {
                        getInventory().setItem(slotIndex, new ItemBuilder(XMaterial.PLAYER_HEAD).setOwner(playerName)
                                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.LEADERBOARD_PLAYER,"%PLACE%",String.valueOf(index+1),"%PLAYER_NAME%",playerName,"%PLAYER_HIMSELF%",(playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : "")))
                                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.LEADERBOARD_PLAYER,"%EGGS_FOUND%", String.valueOf(count),"%EGGS_REMAINING%",String.valueOf(maxEggs-count),"%EGGS_MAX%",String.valueOf(maxEggs),"%IS_IN_TOP_TEN%","§eTHIS PLAYER IS IN THE TOP 10!"))
                                .build());
                    }
                    break;
            }
        }
    }

    public int getMaxItemsPerPage() {
        return maxItemsPerPage;
    }

    public int getMaxPages(){
        HashMap<String, Integer> leaderboard = new HashMap<>();
        if(Main.getInstance().getEggDataManager().savedPlayers() != null){
            for(UUID uuid : Main.getInstance().getEggDataManager().savedPlayers()) {
                FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuid);
                leaderboard.put(playerConfig.getString("FoundEggs.Name"), playerConfig.getInt("FoundEggs.Count"));
            }
        }
        if(leaderboard.isEmpty()) return 1;
        return (int) Math.ceil((double) leaderboard.size() / getMaxItemsPerPage());
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        SoundManager soundManager = Main.getInstance().getSoundManager();
        Player player = (Player) event.getWhoClicked();

        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "leaderboard.collection_selected":
                new CollectionSelectMenu(Main.getPlayerMenuUtility(player)).open();
                return;
            case "leaderboard.next_page":
                if (!((index + 1) >= numberOfPlayers)) {
                    page = page + 1;
                    this.open();
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                } else {
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
                break;
            case "leaderboard.previous_page":
                if (page == 0) {
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                } else {
                    page = page - 1;
                    this.open();
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
                break;
            case "leaderboard.close":
                player.closeInventory();
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case "leaderboard.refresh":
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
            case "leaderboard.sort":
                LeaderboardSortTypes sortTypes = Main.getInstance().getSortTypeLeaderboard().get(playerMenuUtility.getOwner());
                Main.getInstance().getSortTypeLeaderboard().remove(playerMenuUtility.getOwner());
                switch (sortTypes) {
                    case ALL:
                        Main.getInstance().getSortTypeLeaderboard().put(playerMenuUtility.getOwner(), LeaderboardSortTypes.TOP10);
                        break;
                    case TOP10:
                        Main.getInstance().getSortTypeLeaderboard().put(playerMenuUtility.getOwner(), LeaderboardSortTypes.TOP3);
                        break;
                    case TOP3:
                        Main.getInstance().getSortTypeLeaderboard().put(playerMenuUtility.getOwner(), LeaderboardSortTypes.YOU);
                        break;
                    case YOU:
                        Main.getInstance().getSortTypeLeaderboard().put(playerMenuUtility.getOwner(), LeaderboardSortTypes.ALL);
                        break;
                }
                player.playSound(player.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                open();
                break;
        }
    }
}