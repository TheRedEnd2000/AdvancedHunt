package de.theredend2000.advancedhunt.menu;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class Menu implements InventoryHolder {

    protected Inventory inventory;
    protected Player playerMenuUtility;
    protected Main plugin;
    protected Button[] buttons;
    protected boolean preventClose = false;
    protected ItemStack FILLER_GLASS = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName(" ").build();
    protected final Button FILLER_BUTTON = new Button(FILLER_GLASS, null);
    protected Menu previousMenu;

    public Menu(Player playerMenuUtility, Main plugin) {
        this.playerMenuUtility = playerMenuUtility;
        this.plugin = plugin;
    }

    public abstract String getMenuName();

    public abstract int getSlots();

    public abstract void handleMenu(InventoryClickEvent e);

    public abstract void setMenuItems();

    public void open() {
        inventory = Bukkit.createInventory(this, getSlots(), getMenuName());
        this.buttons = new Button[getSlots()];
        this.setMenuItems();
        this.addCloseOrBack();
        playerMenuUtility.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setButton(int slot, Button button) {
        if (slot >= 0 && slot < buttons.length) {
            buttons[slot] = button;
            inventory.setItem(slot, button.getIcon());
        }
    }

    public void addButton(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        setButton(slot, new Button(item, action));
    }

    public void addCloseOrBack(){
        ItemStack closeOrBack = previousMenu == null
                ? new ItemBuilder(Material.BARRIER).setDisplayName(plugin.getMessageManager().getMessage("gui.common.close")).build()
                : new ItemBuilder(Material.ARROW).setDisplayName(plugin.getMessageManager().getMessage("gui.common.back","%menu%", previousMenu.getMenuName())).build();

        addButton(getSlots() - 5, closeOrBack, (e) -> {
            if (previousMenu == null) {
                e.getWhoClicked().closeInventory();
            } else {
                openPreviousMenu();
            }
        });
    }

    /**
     * Adds a button that requires a specific permission to execute its action.
     * If the player lacks the permission, they receive an error message instead.
     * 
     * @param slot The slot to place the button
     * @param item The item to display
     * @param action The action to execute if permission is granted
     * @param permission The permission node to check
     */
    public void addButton(int slot, ItemStack item, Consumer<InventoryClickEvent> action, String permission) {
        setButton(slot, new Button(item, (e) -> {
            if (permission != null && !playerMenuUtility.hasPermission(permission)) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                return;
            }
            if (action != null) {
                action.accept(e);
            }
        }));
    }

    public void addStaticItem(int slot, ItemStack item) {
        if (item.isSimilar(FILLER_GLASS)) {
            setButton(slot, FILLER_BUTTON);
        } else {
            setButton(slot, new Button(item, null));
        }
    }

    public void fillBackground(ItemStack item) {
        Button buttonToUse = item.isSimilar(FILLER_GLASS) ? FILLER_BUTTON : new Button(item, null);
        
        for (int i = 0; i < getSlots(); i++) {
            if (buttons[i] == null) {
                setButton(i, buttonToUse);
            }
        }
    }
    
    public void fillBorders(ItemStack item) {
        int size = getSlots();
        int rows = size / 9;
        
        Button buttonToUse = item.isSimilar(FILLER_GLASS) ? FILLER_BUTTON : new Button(item, null);

        // Top and Bottom
        for (int i = 0; i < 9; i++) {
            if(buttons[i] == null) setButton(i, buttonToUse);
            if(buttons[size - 9 + i] == null) setButton(size - 9 + i, buttonToUse);
        }

        // Sides
        for (int i = 1; i < rows - 1; i++) {
            if(buttons[i * 9] == null) setButton(i * 9, buttonToUse);
            if(buttons[i * 9 + 8] == null) setButton(i * 9 + 8, buttonToUse);
        }
    }

    public void performClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot >= 0 && slot < buttons.length && buttons[slot] != null) {
            buttons[slot].onClick(event);
        }
        handleMenu(event); // Allow custom override logic if needed
    }
    
    public void onOpen(InventoryOpenEvent event) {}
    
    public void onClose(InventoryCloseEvent event) {}

    public Player getPlayer() {
        return playerMenuUtility;
    }
    
    public void refresh() {
        inventory.clear();
        this.buttons = new Button[getSlots()];
        this.setMenuItems();
        this.addCloseOrBack();
    }

    /**
     * Updates a single slot in the inventory without recreating the entire menu.
     * Useful for targeted updates like toggling states or updating dynamic content.
     * 
     * @param slot The slot index to update
     * @param newIcon The new ItemStack to display in that slot
     */
    public void updateSlot(int slot, ItemStack newIcon) {
        if (slot >= 0 && slot < buttons.length && buttons[slot] != null) {
            buttons[slot].setIcon(newIcon);
            inventory.setItem(slot, newIcon);
            playerMenuUtility.updateInventory();
        }
    }

    public Menu setPreviousMenu(Menu previousMenu) {
        this.previousMenu = previousMenu;
        return this;
    }

    public void openPreviousMenu() {
        if (previousMenu != null) {
            previousMenu.open();
        } else {
            playerMenuUtility.closeInventory();
        }
    }
}
