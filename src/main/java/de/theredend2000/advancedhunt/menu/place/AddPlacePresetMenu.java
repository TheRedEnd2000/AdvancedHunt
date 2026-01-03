package de.theredend2000.advancedhunt.menu.place;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemSerializer;
import de.theredend2000.advancedhunt.util.ItemsAdderAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * A menu for creating a place preset by placing a block item in the center slot.
 * The block is not consumed; it is returned to the player on confirm/cancel/close.
 */
public class AddPlacePresetMenu extends Menu {

    private static final int ITEM_SLOT = 22;

    private final String group;
    private final PlacePresetListMenu parentMenu;
    private boolean confirmed = false;

    public AddPlacePresetMenu(Player player, Main plugin, String group, PlacePresetListMenu parentMenu) {
        super(player, plugin);
        this.group = group;
        this.parentMenu = parentMenu;
        setPreviousMenu(parentMenu);
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.place_presets.add.title", false,
                "%group%", group);
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
        int topSize = event.getView().getTopInventory().getSize();

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

        ItemStack placeholder = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.add.placeholder.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.add.placeholder.lore", false))
                .build();
        addStaticItem(ITEM_SLOT, placeholder);

        ItemStack info = new ItemBuilder(Material.BOOK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.add.info.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.add.info.lore", false).toArray(new String[0]))
                .build();
        addStaticItem(4, info);

        ItemStack confirm = new ItemBuilder(Material.LIME_CONCRETE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.add.confirm.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.add.confirm.lore", false).toArray(new String[0]))
                .build();
        addButton(39, confirm, e -> confirmAdd());

        ItemStack cancel = new ItemBuilder(Material.RED_CONCRETE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.add.cancel.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.add.cancel.lore", false).toArray(new String[0]))
                .build();
        addButton(41, cancel, e -> cancelAndReturn());

        fillBackground(FILLER_GLASS);

        inventory.setItem(ITEM_SLOT, null);
        buttons[ITEM_SLOT] = null;
    }

    private void confirmAdd() {
        if (!playerMenuUtility.hasPermission("advancedhunt.admin.place_presets")) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
            return;
        }

        ItemStack item = inventory.getItem(ITEM_SLOT);
        if (item == null || item.getType() == Material.AIR) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.no_item"));
            return;
        }

        if (!item.getType().isBlock() && !ItemsAdderAdapter.isCustomBlockItem(item)) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.not_a_block"));
            return;
        }

        // Save as a single block (amount 1) while preserving ItemAdder NBT/meta
        ItemStack blockSnapshot = item.clone();
        blockSnapshot.setAmount(1);

        String itemData = ItemSerializer.serialize(blockSnapshot);
        if (itemData == null || itemData.isBlank()) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.serialize_failed"));
            return;
        }

        confirmed = true;

        // Return the item (preset creation should not consume player items)
        giveItemBack(item);
        inventory.setItem(ITEM_SLOT, null);

        String name = parentMenu.generateUniqueNameForCreate(group, blockSnapshot);

        plugin.getPlacePresetManager().createPreset(group, name, itemData).whenComplete((ok, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (ex != null || !Boolean.TRUE.equals(ok)) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.create_failed"));
                parentMenu.open();
                return;
            }

            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_presets.created",
                    "%group%", group,
                    "%name%", name));
            parentMenu.open();
        }));
    }

    private void cancelAndReturn() {
        confirmed = true;

        ItemStack item = inventory.getItem(ITEM_SLOT);
        if (item != null && item.getType() != Material.AIR) {
            giveItemBack(item);
        }

        openPreviousMenu();
    }

    private void giveItemBack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        var leftover = playerMenuUtility.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(i -> playerMenuUtility.getWorld().dropItemNaturally(playerMenuUtility.getLocation(), i));
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (!confirmed) {
            ItemStack item = inventory.getItem(ITEM_SLOT);
            if (item != null && item.getType() != Material.AIR) {
                giveItemBack(item);
            }

            Bukkit.getScheduler().runTaskLater(plugin, parentMenu::open, 1L);
        }
    }
}
