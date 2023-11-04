package de.theredend2000.advancedegghunt.versions.managers.inventorymanager.egginformation;

import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public abstract class InformationMenu implements InventoryHolder {

    protected PlayerMenuUtility playerMenuUtility;
    protected Inventory inventory;
    protected ItemStack FILLER_GLASS = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");

    public InformationMenu(PlayerMenuUtility playerMenuUtility) {
        this.playerMenuUtility = playerMenuUtility;
    }
    public abstract String getMenuName();
    public abstract int getSlots();
    public abstract void handleMenu(InventoryClickEvent e);
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

    public ItemStack makeItem(Material material, String displayName, String... lore) {

        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(displayName);

        itemMeta.setLore(Arrays.asList(lore));
        item.setItemMeta(itemMeta);

        return item;
    }

}

