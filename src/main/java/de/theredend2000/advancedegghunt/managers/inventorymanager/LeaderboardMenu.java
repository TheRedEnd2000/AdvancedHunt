package de.theredend2000.advancedegghunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.managers.soundmanager.SoundManager;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.enums.LeaderboardSortTypes;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.*;

public class LeaderboardMenu extends PaginatedInventoryMenu {

    public LeaderboardMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Eggs leaderboard", (short) 54);
    }

    public void open() {
        super.addMenuBorder();
        addMenuBorder();
        getInventory().setContents(inventoryContent);
        setMenuItems();

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    public void addMenuBorder() {
        inventoryContent[48] = new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19")).setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Left").build();

        inventoryContent[50] = new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ==")).setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Right").build();

        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build();
        inventoryContent[53] = new ItemBuilder(XMaterial.EMERALD_BLOCK).setDisplayname("§aRefresh").build();

        String selectedSection = Main.getInstance().getPlayerEggDataManager().getPlayerData(playerMenuUtility.getOwner().getUniqueId()).getString("SelectedSection");
        inventoryContent[45] = new ItemBuilder(XMaterial.PAPER).setDisplayname("§bSelected Collection")
                .setLore("§7Shows your currently selected collection.", "", "§7Current: §6" + selectedSection, "", "§eClick to change.").build();
    }

    public void setMenuItems() {
        String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        addMenuBorder();
        ArrayList<String> keys = new ArrayList<>();
        HashMap<String, Integer> leaderboard = new HashMap<>();

        LeaderboardSortTypes sortTypes = Main.getInstance().getSortTypeLeaderboard().get(playerMenuUtility.getOwner());
        ItemBuilder itemBuilder = new ItemBuilder(XMaterial.HOPPER).setDisplayname("§2Sort");
        switch (sortTypes){
            case ALL:
                itemBuilder = itemBuilder.setLore("", "§6 ➤Show the complete leaderboard", "§7Show only the top 10", "§7Show only the top 3", "§7Show only you", "", "§eClick to switch");
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

        if (Main.getInstance().getEggDataManager().savedPlayers().size() == 0) {
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Player").setLore("§7There are no players in the leaderboard.").build());
            return;
        }
        for(UUID uuid : Main.getInstance().getEggDataManager().savedPlayers()) {
            FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuid);
            leaderboard.put(playerConfig.getString("FoundEggs." + collection + ".Name"), playerConfig.getInt("FoundEggs." + collection + ".Count"));
        }
        for(int i = 0; i < leaderboard.size(); i++)
            keys.add(String.valueOf(i));
        if (keys == null || keys.isEmpty()) {
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Player").setLore("§7There are no players in the leaderboard.").build());
            return;
        }
        List<Map.Entry<String, Integer>> leaderList = new ArrayList<>(leaderboard.entrySet());
        if (leaderList == null || leaderList.isEmpty() || leaderList.get(0).getKey() == null) {
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Player").setLore("§7There are no players in the leaderboard.").build());
            return;
        }
        for(int i = 0; i < getMaxItemsPerPage(); i++) {
            index = getMaxItemsPerPage() * page + i;
            if(index >= keys.size()) break;
            if (keys.get(index) == null) {
                continue;
            }
            int slotIndex = ((9 + 1) + ((i / 7) * 9) + (i % 7));

            leaderList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
            String playerName = leaderList.get(i).getKey();
            int count = leaderList.get(i).getValue();
            int maxEggs = Main.getInstance().getEggManager().getMaxEggs(collection);
            switch (sortTypes) { //TODO: Move top 3 and Top 10 to loop declaration
                case ALL:
                    getInventory().setItem(slotIndex, new ItemBuilder(XMaterial.PLAYER_HEAD).setOwner(playerName).setDisplayname("§6§l" + (i + 1) + "§6th §2§n" + playerName + (playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : "")).setLore("", "§7Eggs Found: §3" + count, "§7Eggs Remaining: §3" + (maxEggs - count), "§7Max Eggs: §3" + maxEggs, "", 9 >= i ? "§eTHIS PLAYER IS IN THE TOP 10!" : "§c" + (i - 9) + " place behind 10th place").build());
                    break;
                case TOP3:
                    if (i < 3) {
                        getInventory().setItem(slotIndex, new ItemBuilder(XMaterial.PLAYER_HEAD).setOwner(playerName).setDisplayname("§6§l" + (i + 1) + "§6th §2§n" + playerName + (playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : "")).setLore("", "§7Eggs Found: §3" + count, "§7Eggs Remaining: §3" + (maxEggs - count), "§7Max Eggs: §3" + maxEggs, "", "§eTHIS PLAYER IS IN THE TOP 10!").build());
                    }
                    break;
                case TOP10:
                    if (i < 10) {
                        getInventory().setItem(slotIndex, new ItemBuilder(XMaterial.PLAYER_HEAD).setOwner(playerName).setDisplayname("§6§l" + (i + 1) + "§6th §2§n" + playerName + (playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : "")).setLore("", "§7Eggs Found: §3" + count, "§7Eggs Remaining: §3" + (maxEggs - count), "§7Max Eggs: §3" + maxEggs, "", "§eTHIS PLAYER IS IN THE TOP 10!").build());
                    }
                    break;
                case YOU:
                    if (playerName.equals(playerMenuUtility.getOwner().getName()))
                        getInventory().setItem(slotIndex, new ItemBuilder(XMaterial.PLAYER_HEAD).setOwner(playerName).setDisplayname("§6§l" + (i + 1) + "§6th §2§n" + playerName + (playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : "")).setLore("", "§7Eggs Found: §3" + count, "§7Eggs Remaining: §3" + (maxEggs - count), "§7Max Eggs: §3" + maxEggs, "", 9 >= i ? "§eTHIS PLAYER IS IN THE TOP 10!" : "§c" + (i - 9) + " place behind 10th place").build());
                    break;
            }
        }
    }

    public int getMaxItemsPerPage() {
        return maxItemsPerPage;
    }

    public int getMaxPages(){
        ArrayList<String> keys = new ArrayList<>();
        HashMap<String, Integer> leaderboard = new HashMap<>();
        if(Main.getInstance().getEggDataManager().savedPlayers() != null){
            for(UUID uuid : Main.getInstance().getEggDataManager().savedPlayers()) {
                FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuid);
                leaderboard.put(playerConfig.getString("FoundEggs.Name"), playerConfig.getInt("FoundEggs.Count"));
            }
            for(int i = 0; i < leaderboard.size(); i++)
                keys.add(String.valueOf(i));
        }
        if(keys.isEmpty()) return 1;
        return (int) Math.ceil((double) keys.size() / getMaxItemsPerPage());
    }

    @Override
    public String getMenuName() {
        return null;
    }

    @Override
    public int getSlots() {
        return 0;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        SoundManager soundManager = Main.getInstance().getSoundManager();
        Player player = (Player) event.getWhoClicked();
        ArrayList<String> keys = new ArrayList<>();
        for(UUID uuids : Main.getInstance().getEggDataManager().savedPlayers())
            keys.add(String.valueOf(uuids));

        if(event.getCurrentItem().getType().equals(Material.PAPER) && ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Selected Collection")){
            new CollectionSelectMenu(Main.getPlayerMenuUtility(player)).open();
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
                        open();
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Right")) {
                    if (!((index + 1) >= keys.size())) {
                        page = page + 1;
                        open();
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

