package de.theredend2000.advancedhunt.menu.common;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;

/**
 * Shared menu pattern where a player places a single item into the center slot,
 * then confirms or cancels.
 *
 * Subclasses provide message keys and confirm behavior.
 */
public abstract class SingleItemInputMenu extends Menu {

    protected static final int ITEM_SLOT = 22;

    private final Menu returnMenu;
    private final boolean consumeItemOnConfirm;

    private boolean confirmed = false;

    protected SingleItemInputMenu(Player player, Main plugin, Menu returnMenu, boolean consumeItemOnConfirm) {
        super(player, plugin);
        this.returnMenu = returnMenu;
        this.consumeItemOnConfirm = consumeItemOnConfirm;
        if (returnMenu != null) {
            setPreviousMenu(returnMenu);
        }
    }

    /**
     * Prefix for GUI keys, e.g. "gui.rewards.add.item" or "gui.place_presets.add".
     */
    protected abstract String getGuiKeyBase();

    /** Message key used when the slot is empty. */
    protected abstract String getNoItemErrorKey();

    /** Validate the placed item. Default: allow any non-air item. */
    protected boolean isValidItem(ItemStack item) {
        return item != null && item.getType() != XMaterial.AIR.get();
    }

    /** Message key when validation fails; return null to send nothing. */
    protected String getInvalidItemErrorKey() {
        return null;
    }

    /** Called after validation passes. Receives a clone of the slot item. */
    protected abstract void onConfirm(ItemStack itemSnapshot);

    /** Called when the menu is canceled. Default: open return menu. */
    protected void onCancel() {
        if (returnMenu != null) {
            returnMenu.open();
        } else {
            openPreviousMenu();
        }
    }

    /** Override if you need a dynamic title with placeholders. */
    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage(getGuiKeyBase() + ".title", false);
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        int slot = e.getRawSlot();

        if (slot == ITEM_SLOT) {
            e.setCancelled(false);
        }

        if (slot >= getSlots()) {
            e.setCancelled(false);
        }
    }

    @Override
    public void handleDrag(InventoryDragEvent event) {
        Set<Integer> rawSlots = event.getRawSlots();
        // 1.8 compatibility: avoid calling methods on InventoryView (API differs across versions).
        int topSize = event.getInventory().getSize();

        for (int slot : rawSlots) {
            if (slot < topSize) {
                if (slot != ITEM_SLOT) {
                    event.setCancelled(true);
                    ((Player) event.getWhoClicked()).updateInventory();
                    return;
                }
            }
        }
    }

    @Override
    public void addCloseOrBack() {
    }

    @Override
    public void setMenuItems() {
        fillBorders(FILLER_GLASS);

        ItemStack placeholder = new ItemBuilder(XMaterial.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setDisplayName(plugin.getMessageManager().getMessage(getGuiKeyBase() + ".placeholder.name", false))
                .setLore(plugin.getMessageManager().getMessageList(getGuiKeyBase() + ".placeholder.lore", false))
                .build();
        addStaticItem(ITEM_SLOT, placeholder);

        ItemStack info = new ItemBuilder(XMaterial.BOOK)
                .setDisplayName(plugin.getMessageManager().getMessage(getGuiKeyBase() + ".info.name", false))
                .setLore(plugin.getMessageManager().getMessageList(getGuiKeyBase() + ".info.lore", false).toArray(new String[0]))
                .build();
        addStaticItem(4, info);

        ItemStack confirm = new ItemBuilder(XMaterial.LIME_CONCRETE)
                .setDisplayName(plugin.getMessageManager().getMessage(getGuiKeyBase() + ".confirm.name", false))
                .setLore(plugin.getMessageManager().getMessageList(getGuiKeyBase() + ".confirm.lore", false).toArray(new String[0]))
                .build();
        addButton(39, confirm, e -> confirmClicked());

        ItemStack cancel = new ItemBuilder(XMaterial.RED_CONCRETE)
                .setDisplayName(plugin.getMessageManager().getMessage(getGuiKeyBase() + ".cancel.name", false))
                .setLore(plugin.getMessageManager().getMessageList(getGuiKeyBase() + ".cancel.lore", false).toArray(new String[0]))
                .build();
        addButton(41, cancel, e -> cancelClicked());

        fillBackground(FILLER_GLASS);

        inventory.setItem(ITEM_SLOT, null);
        buttons[ITEM_SLOT] = null;
    }

    private void confirmClicked() {
        ItemStack item = inventory.getItem(ITEM_SLOT);
        if (item == null || item.getType() == XMaterial.AIR.get()) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(getNoItemErrorKey()));
            return;
        }

        if (!isValidItem(item)) {
            String invalidKey = getInvalidItemErrorKey();
            if (invalidKey != null && !invalidKey.trim().isEmpty()) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(invalidKey));
            }
            return;
        }

        confirmed = true;
        ItemStack snapshot = item.clone();

        if (!consumeItemOnConfirm) {
            // Return the item; menu actions should not consume player items.
            giveItemBack(item);
            inventory.setItem(ITEM_SLOT, null);
        }

        onConfirm(snapshot);
    }

    private void cancelClicked() {
        confirmed = true;

        ItemStack item = inventory.getItem(ITEM_SLOT);
        if (item != null && item.getType() != XMaterial.AIR.get()) {
            giveItemBack(item);
        }

        onCancel();
    }

    private void giveItemBack(ItemStack item) {
        if (item == null || item.getType() == XMaterial.AIR.get()) return;

        Map<Integer, ItemStack> leftover = playerMenuUtility.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(i -> playerMenuUtility.getWorld().dropItemNaturally(playerMenuUtility.getLocation(), i));
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (!confirmed) {
            ItemStack item = inventory.getItem(ITEM_SLOT);
            if (item != null && item.getType() != XMaterial.AIR.get()) {
                giveItemBack(item);
            }

            // Reopen return menu after a tick (avoid close event timing issues)
            if (returnMenu != null) {
                Bukkit.getScheduler().runTaskLater(plugin, returnMenu::open, 1L);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, this::openPreviousMenu, 1L);
            }
        }
    }
}
