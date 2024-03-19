package de.theredend2000.advancedegghunt.managers.inventorymanager.common;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;


public abstract class InventoryMenu implements IInventoryMenu {
    protected PlayerMenuUtility playerMenuUtility;
    private Inventory inventory;
    protected final ItemStack FILLER_GLASS;
    private final String inventoryName;
    protected final short slots;
    protected ItemStack[] inventoryContent;

    public InventoryMenu(PlayerMenuUtility playerMenuUtility, String inventoryName, short slots) {
        this(playerMenuUtility, inventoryName, slots, XMaterial.GRAY_STAINED_GLASS_PANE);
    }

    public InventoryMenu(PlayerMenuUtility playerMenuUtility, String inventoryName, short slots, XMaterial fillerMaterial) {
        this.playerMenuUtility = playerMenuUtility;
        this.slots = slots % 9 == 0? slots : (short) (slots - (slots % 9));
        this.inventoryContent = new ItemStack[this.slots];
        this.inventoryName = inventoryName;

        this.FILLER_GLASS = new ItemBuilder(fillerMaterial).setDisplayname(" ").build();
    }

    protected void addMenuBorder()
    {
        int lastRowStartIndex = 9 * ((slots / 9) - 1);
        for (int column = 1; column < 9; column++) {
            inventoryContent[column] = FILLER_GLASS;
            inventoryContent[column + lastRowStartIndex] = FILLER_GLASS;
        }

        for (int row = 0; row < slots / 9 ; row++) {
            inventoryContent[row * 9] = FILLER_GLASS;
            inventoryContent[(row * 9) + 8] = FILLER_GLASS;
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack instanceof InventoryButton) {
            ((InventoryButton)itemStack).onClick(event.getClick(), (Player)event.getWhoClicked());
        }
    }

    @NotNull
    @Override
    public final Inventory getInventory() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, slots, inventoryName);
            inventory.setContents(inventoryContent);
        }

        return inventory;
    }
}
