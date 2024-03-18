package de.theredend2000.advancedegghunt.managers.inventorymanager.egginformation;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.managers.inventorymanager.IInventoryMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public abstract class InformationMenu implements IInventoryMenu {

    protected PlayerMenuUtility playerMenuUtility;
    protected Inventory inventory;
    protected ItemStack FILLER_GLASS = makeItem(XMaterial.GRAY_STAINED_GLASS_PANE, " ");

    public InformationMenu(PlayerMenuUtility playerMenuUtility) {
        this.playerMenuUtility = playerMenuUtility;
    }

    public abstract void setMenuItems(String eggId);

    public void open(String eggId) {
        inventory = Bukkit.createInventory(this, getSlots(), getMenuName());

        this.setMenuItems(eggId);

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

