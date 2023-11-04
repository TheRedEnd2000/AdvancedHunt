package de.theredend2000.advancedegghunt.versions.managers.inventorymanager.leaderboardmenu;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.enums.LeaderboardSortTypes;
import de.theredend2000.advancedegghunt.versions.VersionManager;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
        Player p = (Player) e.getWhoClicked();
        ArrayList<String> keys = new ArrayList<>();
        if(Main.getInstance().eggs.contains("FoundEggs."))
            keys.addAll(Main.getInstance().eggs.getConfigurationSection("FoundEggs.").getKeys(false));
        if (e.getCurrentItem().getType().equals(Material.BARRIER)) {
            p.closeInventory();
            p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
        }else if (e.getCurrentItem().getType().equals(Material.EMERALD_BLOCK)) {
            if(Main.getInstance().getRefreshCooldown().containsKey(p.getName())){
                if(Main.getInstance().getRefreshCooldown().get(p.getName()) > System.currentTimeMillis()){
                    p.sendMessage(Main.getInstance().getMessage("RefreshWaitMessage"));
                    p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventoryFailedSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                    return;
                }
            }
            Main.getInstance().getRefreshCooldown().put(p.getName(), System.currentTimeMillis()+ (3*1000));
            new EggLeaderboardMenu(Main.getPlayerMenuUtility(p)).open();
            p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
        }else if(e.getCurrentItem().getType().equals(Material.PLAYER_HEAD)){
            if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Left")){
                if (page == 0){
                    p.sendMessage(Main.getInstance().getMessage("AlreadyOnFirstPageMessage"));
                    p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventoryFailedSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                }else{
                    page = page - 1;
                    super.open();
                    p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                }
            }else if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Right")){
                if (!((index + 1) >= keys.size())){
                    page = page + 1;
                    super.open();
                    p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                }else{
                    p.sendMessage(Main.getInstance().getMessage("AlreadyOnLastPageMessage"));
                    p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventoryFailedSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                }
            }
        }else if(e.getCurrentItem().getType().equals(Material.HOPPER)){
            if(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Sort")){
                LeaderboardSortTypes sortTypes = Main.getInstance().getSortTypeLeaderboard().get(playerMenuUtility.getOwner());
                Main.getInstance().getSortTypeLeaderboard().remove(playerMenuUtility.getOwner());
                switch (sortTypes){
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
                p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventoryFailedSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                super.open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        addMenuBorder();
        ArrayList<String> keys = new ArrayList<>();
        HashMap<String, Integer> leaderboard = new HashMap<>();
        if(Main.getInstance().eggs.contains("FoundEggs.")){
            for(String uuid : Main.getInstance().eggs.getConfigurationSection("FoundEggs.").getKeys(false)) {
                leaderboard.put(Main.getInstance().eggs.getString("FoundEggs."+uuid+".Name"),Main.getInstance().eggs.getInt("FoundEggs."+uuid+".Count"));
            }
            for(int i = 0; i < leaderboard.size(); i++)
                keys.add(String.valueOf(i));
        }else
            inventory.setItem(22, new ItemBuilder(Material.RED_STAINED_GLASS).setDisplayname("§4§lNo Player").setLore("§7There are no players in the leaderboard.").build());
        if(keys != null && !keys.isEmpty()) {
            List<Map.Entry<String, Integer>> leaderList = new ArrayList<>(leaderboard.entrySet());
            for(int i = 0; i < getMaxItemsPerPage(); i++) {
                index = getMaxItemsPerPage() * page + i;
                if(index >= keys.size()) break;
                if (keys.get(index) != null){
                    LeaderboardSortTypes sortTypes = Main.getInstance().getSortTypeLeaderboard().get(playerMenuUtility.getOwner());
                    leaderList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
                    String playerName = leaderList.get(i).getKey();
                    int count = leaderList.get(i).getValue();
                    int maxEggs = VersionManager.getEggManager().getMaxEggs();
                    switch (sortTypes){
                        case ALL:
                            inventory.addItem(new ItemBuilder(Material.PLAYER_HEAD).setOwner(playerName).setDisplayname("§6§l"+(i+1)+"§6th §2§n"+playerName+ (playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : "")).setLore("","§7Eggs Found: §3"+count,"§7Eggs Remaining: §3"+(maxEggs-count),"§7Max Eggs: §3"+maxEggs,"",9 >= i ? "§eTHIS PLAYER IS IN THE TOP 10!" : "§c"+(i-9)+" place behind 10th place").build());
                            break;
                        case TOP3:
                            if(i < 3){
                                inventory.addItem(new ItemBuilder(Material.PLAYER_HEAD).setOwner(playerName).setDisplayname("§6§l"+(i+1)+"§6th §2§n"+playerName+ (playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : "")).setLore("","§7Eggs Found: §3"+count,"§7Eggs Remaining: §3"+(maxEggs-count),"§7Max Eggs: §3"+maxEggs,"","§eTHIS PLAYER IS IN THE TOP 10!").build());
                            }
                            break;
                        case TOP10:
                            if(i < 10){
                                inventory.addItem(new ItemBuilder(Material.PLAYER_HEAD).setOwner(playerName).setDisplayname("§6§l"+(i+1)+"§6th §2§n"+playerName+ (playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : "")).setLore("","§7Eggs Found: §3"+count,"§7Eggs Remaining: §3"+(maxEggs-count),"§7Max Eggs: §3"+maxEggs,"","§eTHIS PLAYER IS IN THE TOP 10!").build());
                            }
                            break;
                        case YOU:
                            if(playerName.equals(playerMenuUtility.getOwner().getName()))
                                inventory.addItem(new ItemBuilder(Material.PLAYER_HEAD).setOwner(playerName).setDisplayname("§6§l"+(i+1)+"§6th §2§n"+playerName+ (playerName.equals(playerMenuUtility.getOwner().getName()) ? "§r §a§lYOU" : "")).setLore("","§7Eggs Found: §3"+count,"§7Eggs Remaining: §3"+(maxEggs-count),"§7Max Eggs: §3"+maxEggs,"",9 >= i ? "§eTHIS PLAYER IS IN THE TOP 10!" : "§c"+(i-9)+" place behind 10th place").build());
                            break;
                    }
                }
            }
        }
    }
}

