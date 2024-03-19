package de.theredend2000.advancedegghunt.managers.inventorymanager.collectionselection;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.IInventoryMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public abstract class SelectionSelectMenu implements IInventoryMenu {

    protected PlayerMenuUtility playerMenuUtility;
    protected Inventory inventory;
    protected ItemStack FILLER_GLASS = makeItem(XMaterial.GRAY_STAINED_GLASS_PANE, " ");

    public SelectionSelectMenu(PlayerMenuUtility playerMenuUtility) {
        this.playerMenuUtility = playerMenuUtility;
    }

    public abstract void setMenuItems();

    public void open() {
        inventory = Bukkit.createInventory(this, getSlots(), getMenuName());

        this.setMenuItems();

        playerMenuUtility.getOwner().openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public ItemStack makeItem(XMaterial material, String displayName, String... lore) {

        ItemStack item = material.parseItem();
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(displayName);

        itemMeta.setLore(Arrays.asList(lore));
        item.setItemMeta(itemMeta);

        return item;
    }
}

