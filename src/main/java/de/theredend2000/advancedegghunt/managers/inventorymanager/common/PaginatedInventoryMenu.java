package de.theredend2000.advancedegghunt.managers.inventorymanager.common;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.PlayerMenuUtility;


public abstract class PaginatedInventoryMenu extends InventoryMenu {
    protected int maxItemsPerPage;
    protected int page = 0;
    protected int index = 0;

    public PaginatedInventoryMenu(PlayerMenuUtility playerMenuUtility, String inventoryName, short slots, XMaterial fillerMaterial) {
        super(playerMenuUtility, inventoryName, slots, fillerMaterial);
        this.maxItemsPerPage = 7 * ((this.slots / 9) - 2);
    }

    public PaginatedInventoryMenu(PlayerMenuUtility playerMenuUtility, String inventoryName, short slots) {
        super(playerMenuUtility, inventoryName, slots);
        this.maxItemsPerPage = 7 * ((this.slots / 9) - 2);
    }

    public PaginatedInventoryMenu(PlayerMenuUtility playerMenuUtility, String inventoryName, short slots, int maxItemsPerPage) {
        super(playerMenuUtility, inventoryName, slots);
        this.maxItemsPerPage = maxItemsPerPage;
    }
}
