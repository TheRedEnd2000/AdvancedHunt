package de.theredend2000.advancedegghunt.managers.inventorymanager.leaderboardmenu;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.managers.inventorymanager.sectionselection.SelectionSelectListMenu;
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

public class EggLeaderboardMenu extends LeaderboardPaginatedMenu {

    public EggLeaderboardMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
    }

    @Override
    public String getMenuName() {
        return "Eggs leaderboard";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        SoundManager soundManager = Main.getInstance().getSoundManager();
        Player p = (Player) e.getWhoClicked();
        ArrayList<String> keys = new ArrayList<>();
        for(UUID uuids : Main.getInstance().getEggDataManager().savedPlayers())
            keys.add(String.valueOf(uuids));

        if(e.getCurrentItem().getType().equals(Material.PAPER) && ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Selected Collection")){
            new SelectionSelectListMenu(Main.getPlayerMenuUtility(p)).open();
        }

        switch (e.getCurrentItem().getType()) {
            case BARRIER:
                p.closeInventory();
                p.playSound(p.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case EMERALD_BLOCK:
                if (Main.getInstance().getRefreshCooldown().containsKey(p.getName())) {
                    if (Main.getInstance().getRefreshCooldown().get(p.getName()) > System.currentTimeMillis()) {
                        p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.WAIT_REFRESH));
                        p.playSound(p.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                        return;
                    }
                }
                Main.getInstance().getRefreshCooldown().put(p.getName(), System.currentTimeMillis() + (3 * 1000));
                new EggLeaderboardMenu(Main.getPlayerMenuUtility(p)).open();
                p.playSound(p.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case PLAYER_HEAD:
                if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Left")) {
                    if (page == 0) {
                        p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                        p.playSound(p.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                    } else {
                        page = page - 1;
                        super.open();
                        p.playSound(p.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Right")) {
                    if (!((index + 1) >= keys.size())) {
                        page = page + 1;
                        super.open();
                        p.playSound(p.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    } else {
                        p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                        p.playSound(p.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                    }
                }
                break;
            case HOPPER:
                if (!ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Sort")) {
                    return;
                }
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
                p.playSound(p.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                super.open();
                break;
        }
    }

    @Override
    public void setMenuItems() {
        String section = Main.getInstance().getEggManager().getEggSectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        addMenuBorder();
        ArrayList<String> keys = new ArrayList<>();
        HashMap<String, Integer> leaderboard = new HashMap<>();
        if(Main.getInstance().getEggDataManager().savedPlayers().size() != 0){
            for(UUID uuid : Main.getInstance().getEggDataManager().savedPlayers()) {
                FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuid);
                leaderboard.put(playerConfig.getString("FoundEggs."+section+".Name"),playerConfig.getInt("FoundEggs."+section+".Count"));
            }
            for(int i = 0; i < leaderboard.size(); i++)
                keys.add(String.valueOf(i));
        }else
            inventory.setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Player").setLore("§7There are no players in the leaderboard.").build());
        if(keys != null && !keys.isEmpty()) {
            List<Map.Entry<String, Integer>> leaderList = new ArrayList<>(leaderboard.entrySet());
            if(leaderList != null && !leaderList.isEmpty() && leaderList.get(0).getKey() != null){
                for(int i = 0; i < getMaxItemsPerPage(); i++) {
                    index = getMaxItemsPerPage() * page + i;
                    if(index >= keys.size()) break;
                    if (keys.get(index) == null) {
                        continue;
                    }
                    LeaderboardSortTypes sortTypes = Main.getInstance().getSortTypeLeaderboard().get(playerMenuUtility.getOwner());
                    leaderList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
                    String playerName = leaderList.get(i).getKey();
                    int count = leaderList.get(i).getValue();
                    int maxEggs = Main.getInstance().getEggManager().getMaxEggs(section);
                    switch (sortTypes) {
                        case ALL:
                            inventory.addItem(new ItemBuilder(XMaterial.PLAYER_HEAD).setOwner(playerName).setDisplayname("§6§l" + (i + 1) + "§6th §2§n" + playerName + (playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : "")).setLore("", "§7Eggs Found: §3" + count, "§7Eggs Remaining: §3" + (maxEggs - count), "§7Max Eggs: §3" + maxEggs, "", 9 >= i ? "§eTHIS PLAYER IS IN THE TOP 10!" : "§c" + (i - 9) + " place behind 10th place").build());
                            break;
                        case TOP3:
                            if (i < 3) {
                                inventory.addItem(new ItemBuilder(XMaterial.PLAYER_HEAD).setOwner(playerName).setDisplayname("§6§l" + (i + 1) + "§6th §2§n" + playerName + (playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : "")).setLore("", "§7Eggs Found: §3" + count, "§7Eggs Remaining: §3" + (maxEggs - count), "§7Max Eggs: §3" + maxEggs, "", "§eTHIS PLAYER IS IN THE TOP 10!").build());
                            }
                            break;
                        case TOP10:
                            if (i < 10) {
                                inventory.addItem(new ItemBuilder(XMaterial.PLAYER_HEAD).setOwner(playerName).setDisplayname("§6§l" + (i + 1) + "§6th §2§n" + playerName + (playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : "")).setLore("", "§7Eggs Found: §3" + count, "§7Eggs Remaining: §3" + (maxEggs - count), "§7Max Eggs: §3" + maxEggs, "", "§eTHIS PLAYER IS IN THE TOP 10!").build());
                            }
                            break;
                        case YOU:
                            if (playerName.equals(playerMenuUtility.getOwner().getName()))
                                inventory.addItem(new ItemBuilder(XMaterial.PLAYER_HEAD).setOwner(playerName).setDisplayname("§6§l" + (i + 1) + "§6th §2§n" + playerName + (playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : "")).setLore("", "§7Eggs Found: §3" + count, "§7Eggs Remaining: §3" + (maxEggs - count), "§7Max Eggs: §3" + maxEggs, "", 9 >= i ? "§eTHIS PLAYER IS IN THE TOP 10!" : "§c" + (i - 9) + " place behind 10th place").build());
                            break;
                    }
                }
            }else
                inventory.setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Player").setLore("§7There are no players in the leaderboard.").build());
        }else
            inventory.setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Player").setLore("§7There are no players in the leaderboard.").build());
    }
}

