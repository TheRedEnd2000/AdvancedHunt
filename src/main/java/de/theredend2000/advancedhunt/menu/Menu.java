package de.theredend2000.advancedhunt.menu;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public abstract class Menu implements InventoryHolder {

    protected Inventory inventory;
    protected Player playerMenuUtility;
    protected Main plugin;
    protected Button[] buttons;
    protected boolean preventClose = false;
    protected ItemStack FILLER_GLASS = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).hideTooltip(true).build();
    protected ItemStack EXTRA_GLASS = new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).hideTooltip(true).build();
    protected final Button FILLER_BUTTON = new Button(FILLER_GLASS, null);
    protected Menu previousMenu;

    public Menu(Player playerMenuUtility, Main plugin) {
        this.playerMenuUtility = playerMenuUtility;
        this.plugin = plugin;
    }

    public abstract String getMenuName();

    public abstract int getSlots();

    public abstract void handleMenu(InventoryClickEvent e);

    public void handleDrag(InventoryDragEvent event) {
        Set<Integer> rawSlots = event.getRawSlots();
        int topSize = event.getView().getTopInventory().getSize();

        for (int slot : rawSlots) {
            if (slot < topSize) { // Slots 0 to size-1 are the top inventory
                event.setCancelled(true);
                ((Player) event.getWhoClicked()).updateInventory();
                return;
            }
        }
    }

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
                ? getCloseButton()
                : getBackButton();

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

    protected ItemStack buildActionItem(
            Material enabledMaterial,
            boolean enabled,
            String nameKey,
            String loreKeyEnabled,
            String loreKeyDisabled,
            String... loreArgsEnabled
    ) {
        Material material = enabled ? enabledMaterial : Material.BARRIER;
        String loreKey = enabled ? loreKeyEnabled : loreKeyDisabled;

        ItemBuilder builder = new ItemBuilder(material)
                .setDisplayName(plugin.getMessageManager().getMessage(nameKey, false));

        if (enabled && loreArgsEnabled != null && loreArgsEnabled.length > 0) {
            builder.setLore(plugin.getMessageManager().getMessageList(loreKey, false, loreArgsEnabled).toArray(new String[0]));
        } else {
            builder.setLore(plugin.getMessageManager().getMessageList(loreKey, false).toArray(new String[0]));
        }

        return builder.build();
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
        // Only trigger buttons if the click is in the top inventory (the menu)
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(inventory)) {
            int slot = event.getSlot();
            if (slot >= 0 && slot < buttons.length && buttons[slot] != null) {
                buttons[slot].onClick(event);
            }
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

    public ItemStack getCloseButton(){
        return new ItemBuilder(Material.PLAYER_HEAD)
                .setSkullTexture("ewogICJ0aW1lc3RhbXAiIDogMTc2NzUyNTYyMTI2MywKICAicHJvZmlsZUlkIiA6ICIxMTM0OTAxMTU3ZTE0Yzg0OTE1YTNjMGY3M2RmYzM0NSIsCiAgInByb2ZpbGVOYW1lIiA6ICJab2xlZWV5IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2JjMTc1ZjFkMzEyZjNjOTEyZmM1ZjY3MjAyNDNlMmZmMmU1NDk4YjgwMGZjNWI3MDEyNjFhZDM2NTViODJjYWIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==")
                .setDisplayName(plugin.getMessageManager().getMessage("gui.common.close"))
                .build();
    }

    public ItemStack getBackButton(){
        return new ItemBuilder(Material.PLAYER_HEAD)
                .setSkullTexture("ewogICJ0aW1lc3RhbXAiIDogMTc2NzUyMjU3ODMzMCwKICAicHJvZmlsZUlkIiA6ICJmMWFlNzVjYmE3MmU0YWUzYTU0Yzk5ZmYwMWFlZjQ1ZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJlcm1zaWVzIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Q1NjE3Y2UxMjllZDk1YzdiOTE1MTE5YjEwNzQ2ZjM0YzFjYzlkZmYwYWUzZTMzMjM4NjNlYmVkZDAwYmEyOWYiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==")
                .setDisplayName(plugin.getMessageManager().getMessage("gui.common.back","%menu%", previousMenu.getMenuName()))
                .build();
    }

    public ItemStack getWarningIcon(String displayName, List<String> lore){
        return new ItemBuilder(Material.PLAYER_HEAD)
                .setSkullTexture("ewogICJ0aW1lc3RhbXAiIDogMTc2NzUyMjY0NTA3MiwKICAicHJvZmlsZUlkIiA6ICI0OThjYTc2ZGYwODM0NzhmOGY0NjdjOGY1OTQwMjk1MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJHdWx0cm8iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDZlODRmZmM1NzcxY2VlOWFjMmZiMDMyN2YxMWMyZGRiN2EwOGFjMmYxNjVjNjFjMDUyMWI3YmI5N2VhZGY5ZCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9")
                .setDisplayName(displayName)
                .setLore(lore)
                .build();
    }
}
