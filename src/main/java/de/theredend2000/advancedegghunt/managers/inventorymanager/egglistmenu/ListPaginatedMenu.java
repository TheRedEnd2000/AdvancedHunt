package de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ItemBuilder;

public abstract class ListPaginatedMenu extends ListMenu {

    protected int page = 0;
    protected int maxItemsPerPage = 28;
    protected int index = 0;

    public ListPaginatedMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
    }
    public void addMenuBorder(){
        inventory.setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19")).setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Left").build());

        inventory.setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ==")).setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Right").build());

        inventory.setItem(49, makeItem(XMaterial.BARRIER, "§4Close"));
        inventory.setItem(53, makeItem(XMaterial.EMERALD_BLOCK, "§aRefresh"));
        String selectedSection = Main.getInstance().getPlayerEggDataManager().getPlayerData(playerMenuUtility.getOwner().getUniqueId()).getString("SelectedSection");
        inventory.setItem(45, new ItemBuilder(XMaterial.PAPER).setDisplayname("§bSelected Collection").setLore("§7Shows your currently selected collection.", "", "§7Current: §6" + selectedSection, "", "§eClick to change.").build());

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
        String section = Main.getInstance().getEggManager().getEggSectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        int keys = Main.getInstance().getEggManager().getMaxEggs(section);
        if(keys == 0) return 1;
        return (int) Math.ceil((double) keys / getMaxItemsPerPage());
    }
}

