package de.theredend2000.advancedegghunt.versions.managers.inventorymanager.egginformation;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;

public abstract class InformationPaginatedMenu extends InformationMenu {

    protected int page = 0;
    protected int maxItemsPerPage = 28;
    protected int index = 0;

    public InformationPaginatedMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
    }
    public void addMenuBorder(String eggId){
        inventory.setItem(48, new ItemBuilder(Material.PLAYER_HEAD).setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19")).setLore("§6Page: §7(§b"+(page+1)+"§7/§b"+getMaxPages(eggId)+"§7)","","§eClick to scroll.").setDisplayname("§2Left").build());

        inventory.setItem(50, new ItemBuilder(Material.PLAYER_HEAD).setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ==")).setLore("§6Page: §7(§b"+(page+1)+"§7/§b"+getMaxPages(eggId)+"§7)","","§eClick to scroll.").setDisplayname("§2Right").build());

        inventory.setItem(49, makeItem(Material.BARRIER, "§4Close"));
        inventory.setItem(53, makeItem(Material.EMERALD_BLOCK, "§aRefresh"));
        inventory.setItem(45, new ItemBuilder(Material.PLAYER_HEAD).setSkullOwner(Main.getTexture("NWYxMzNlOTE5MTlkYjBhY2VmZGMyNzJkNjdmZDg3YjRiZTg4ZGM0NGE5NTg5NTg4MjQ0NzRlMjFlMDZkNTNlNiJ9fX0=")).setDisplayname("§eBack").build());

        for (int i = 1; i < 10; i++) {
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
        inventory.setItem(44, super.FILLER_GLASS);

        for (int i = 46; i < 53; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, super.FILLER_GLASS);
            }
        }
    }

    public int getMaxItemsPerPage() {
        return maxItemsPerPage;
    }
    public int getMaxPages(String eggId){
        ArrayList<String> keys = new ArrayList<>();
        for(String uuids : Main.getInstance().eggs.getConfigurationSection("FoundEggs.").getKeys(false)){
            if(Main.getInstance().eggs.contains("FoundEggs."+uuids+"."+eggId)){
                Collections.addAll(keys, Main.getInstance().eggs.getString("FoundEggs."+uuids+".Name"));
            }
        }
        if(keys.isEmpty()) return 1;
        return (int) Math.ceil((double) keys.size() / getMaxItemsPerPage());
    }
}

