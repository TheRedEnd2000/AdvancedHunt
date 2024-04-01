package de.theredend2000.advancedegghunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.SoundManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.enums.LeaderboardSortTypes;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build();
        inventoryContent[53] = new ItemBuilder(XMaterial.EMERALD_BLOCK).setDisplayname("§aRefresh").build();

        String selectedCollection = Main.getInstance().getPlayerEggDataManager().getPlayerData(playerMenuUtility.getOwner().getUniqueId()).getString("SelectedSection");
        inventoryContent[45] = new ItemBuilder(XMaterial.PAPER).setDisplayname("§bSelected Collection")
                .setLore("§7Shows your currently selected collection.", "", "§7Current: §6" + selectedCollection, "", "§eClick to change.").build();
    }

    public void setMenuItems() {
        getInventory().setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Left")
                .setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19")).build());
        getInventory().setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Right")
                .setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ==")).build());

        String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        addMenuBorderButtons();

        LeaderboardSortTypes sortTypes = Main.getInstance().getSortTypeLeaderboard().get(playerMenuUtility.getOwner());
        ItemBuilder itemBuilder = new ItemBuilder(XMaterial.HOPPER).setDisplayname("§2Sort");
        switch (sortTypes){
            case ALL:
                itemBuilder = itemBuilder.setLore("", "§6 ➤ Show the complete leaderboard", "§7Show only the top 10", "§7Show only the top 3", "§7Show only you", "", "§eClick to switch");
                break;
            case TOP10:
                itemBuilder = itemBuilder.setLore("", "§7Show the complete leaderboard", "§6➤ Show only the top 10", "§7Show only the top 3", "§7Show only you", "", "§eClick to switch");
                break;
            case TOP3:
                itemBuilder = itemBuilder.setLore("", "§7Show the complete leaderboard", "§7Show only the top 10", "§6➤ Show only the top 3", "§7Show only you", "", "§eClick to switch");
                break;
            case YOU:
                itemBuilder = itemBuilder.setLore("", "§7Show the complete leaderboard", "§7Show only the top 10", "§7Show only the top 3", "§6➤ Show only you", "", "§eClick to switch");
                break;
        }
        getInventory().setItem(51, itemBuilder.build());

        if (Main.getInstance().getEggDataManager().savedPlayers().isEmpty()) {
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Player").setLore("§7There are no players in the leaderboard.").build());
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
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Player").setLore("§7There are no players in the leaderboard.").build());
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
                        .setDisplayname("§6§l" + (i + 1) + "§6th §2§n" + playerName + (playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : ""))
                        .setLore("", "§7Eggs Found: §3" + count, "§7Eggs Remaining: §3" + (maxEggs - count), "§7Max Eggs: §3" + maxEggs, "", 9 >= index ? "§eTHIS PLAYER IS IN THE TOP 10!" : "§c" + (index - 9) + " place behind 10th place").build());

                return;
            }
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Player").setLore("§7There are no players in the leaderboard.").build());
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
                            .setDisplayname("§6§l" + (index + 1) + "§6th §2§n" + playerName + (playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : ""))
                            .setLore("", "§7Eggs Found: §3" + count, "§7Eggs Remaining: §3" + (maxEggs - count), "§7Max Eggs: §3" + maxEggs, "", 9 >= index ? "§eTHIS PLAYER IS IN THE TOP 10!" : "§c" + (index - 9) + " place behind 10th place").build());
                    break;
                case TOP3:
                    numberOfPlayers = 3;
                    if (i < 3) {
                        getInventory().setItem(slotIndex, new ItemBuilder(XMaterial.PLAYER_HEAD).setOwner(playerName)
                                .setDisplayname("§6§l" + (index + 1) + "§6th §2§n" + playerName + (playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : ""))
                                .setLore("", "§7Eggs Found: §3" + count, "§7Eggs Remaining: §3" + (maxEggs - count), "§7Max Eggs: §3" + maxEggs, "", "§eTHIS PLAYER IS IN THE TOP 10!").build());
                    }
                    break;
                case TOP10:
                    numberOfPlayers = 10;
                    if (i < 10) {
                        getInventory().setItem(slotIndex, new ItemBuilder(XMaterial.PLAYER_HEAD).setOwner(playerName)
                                .setDisplayname("§6§l" + (index + 1) + "§6th §2§n" + playerName + (playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : ""))
                                .setLore("", "§7Eggs Found: §3" + count, "§7Eggs Remaining: §3" + (maxEggs - count), "§7Max Eggs: §3" + maxEggs, "", "§eTHIS PLAYER IS IN THE TOP 10!").build());
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

        if(event.getCurrentItem().getType().equals(Material.PAPER) && ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Selected Collection")){
            new CollectionSelectMenu(Main.getPlayerMenuUtility(player)).open();
            return;
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
                        player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.WAIT_REFRESH));
                        player.playSound(player.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                        return;
                    }
                }
                Main.getInstance().getRefreshCooldown().put(player.getName(), System.currentTimeMillis() + (3 * 1000));
                open();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case PLAYER_HEAD:
                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Left")) {
                    if (page == 0) {
                        player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                        player.playSound(player.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                    } else {
                        page = page - 1;
                        reopen();
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Right")) {
                    if (!((index + 1) >= numberOfPlayers)) {
                        page = page + 1;
                        reopen();
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    } else {
                        player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                        player.playSound(player.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                    }
                }
                break;
            case HOPPER:
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

