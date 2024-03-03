package de.theredend2000.advancedegghunt.managers.inventorymanager.leaderboardmenu;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.enums.LeaderboardSortTypes;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public abstract class LeaderboardPaginatedMenu extends LeadeboardMenu {

    protected int page = 0;
    protected int maxItemsPerPage = 28;
    protected int index = 0;

    public LeaderboardPaginatedMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
    }
    public void addMenuBorder(){
        inventory.setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19")).setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Left").build());

        inventory.setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ==")).setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Right").build());

        inventory.setItem(49, makeItem(XMaterial.BARRIER, "§4Close"));
        inventory.setItem(53, makeItem(XMaterial.EMERALD_BLOCK, "§aRefresh"));
        String selectedSection = Main.getInstance().getPlayerEggDataManager().getPlayerData(playerMenuUtility.getOwner().getUniqueId()).getString("SelectedSection");
        inventory.setItem(45, new ItemBuilder(XMaterial.PAPER).setDisplayname("§bSelected Collection").setLore("§7Shows your currently selected collection.", "", "§7Current: §6" + selectedSection, "", "§eClick to change.").build());
        LeaderboardSortTypes sortTypes = Main.getInstance().getSortTypeLeaderboard().get(playerMenuUtility.getOwner());
        switch (sortTypes){
            case ALL:
                inventory.setItem(51, new ItemBuilder(XMaterial.HOPPER).setDisplayname("§2Sort").setLore("", "§6 ➤Show the complete leaderboard", "§7Show only the top 10", "§7Show only the top 3", "§7Show only you", "", "§eClick to switch").build());
                break;
            case TOP10:
                inventory.setItem(51, new ItemBuilder(XMaterial.HOPPER).setDisplayname("§2Sort").setLore("", "§7Show the complete leaderboard", "§6➤ Show only the top 10", "§7Show only the top 3", "§7Show only you", "", "§eClick to switch").build());
                break;
            case TOP3:
                inventory.setItem(51, new ItemBuilder(XMaterial.HOPPER).setDisplayname("§2Sort").setLore("", "§7Show the complete leaderboard", "§7Show only the top 10", "§6➤ Show only the top 3", "§7Show only you", "", "§eClick to switch").build());
                break;
            case YOU:
                inventory.setItem(51, new ItemBuilder(XMaterial.HOPPER).setDisplayname("§2Sort").setLore("", "§7Show the complete leaderboard", "§7Show only the top 10", "§7Show only the top 3", "§6➤ Show only you", "", "§eClick to switch").build());
                break;
        }

        for (int i = 0; i < 10; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, super.FILLER_GLASS);
            }
        }

        inventory.setItem(17, super.FILLER_GLASS);
        inventory.setItem(18, super.FILLER_GLASS);
        inventory.setItem(26, super.FILLER_GLASS);
        inventory.setItem(27, super.FILLER_GLASS);
        inventory.setItem(35, super.FILLER_GLASS);
        inventory.setItem(36, super.FILLER_GLASS);

        for (int i = 44; i < 53; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, super.FILLER_GLASS);
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
}

