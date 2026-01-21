package de.theredend2000.advancedhunt.menu.place;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.menu.common.ConfirmationMenu;
import de.theredend2000.advancedhunt.model.PlaceItem;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemSerializer;
import de.theredend2000.advancedhunt.util.ItemsAdderAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class PlaceItemActionsMenu extends Menu {

    private final PlaceItem preset;

    public PlaceItemActionsMenu(Player player, Main plugin, PlaceItem preset) {
        super(player, plugin);
        this.preset = preset;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.place_items.actions.title", false,
                "%name%", preset.getName());
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        // handled by buttons
    }

    @Override
    public void setMenuItems() {
        fillBorders(FILLER_GLASS);

        addButton(12, new ItemBuilder(XMaterial.CHEST)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_items.actions.give.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_items.actions.give.lore", false).toArray(new String[0]))
                .build(), e -> {
            give();
            openPreviousMenu();
        });

        addButton(14, new ItemBuilder(XMaterial.TNT)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_items.actions.delete.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_items.actions.delete.lore", false).toArray(new String[0]))
                .build(), e -> handleDelete());

        fillBackground(FILLER_GLASS);
    }

    private void give() {
        ItemStack item = ItemSerializer.deserialize(preset.getItemData());
        if (item == null || item.getType() == XMaterial.AIR.get()) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_items.deserialize_failed"));
            return;
        }

        if (!item.getType().isBlock() && !ItemsAdderAdapter.isCustomBlockItem(item) && !ItemsAdderAdapter.isCustomFurniture(item)) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_items.not_a_block"));
            return;
        }

        ItemStack toGive = item.clone();
        toGive.setAmount(1);
        Map<Integer, ItemStack> leftovers = playerMenuUtility.getInventory().addItem(toGive);
        for (ItemStack leftover : leftovers.values()) {
            playerMenuUtility.getWorld().dropItemNaturally(playerMenuUtility.getLocation(), leftover);
        }

        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_items.given",
                "%group%", preset.getGroup(),
                "%name%", preset.getName()));
    }

    private void handleDelete() {
        if (!playerMenuUtility.hasPermission("advancedhunt.admin.place_items")) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
            return;
        }

        new ConfirmationMenu(playerMenuUtility, plugin,
                plugin.getMessageManager().getMessage("gui.place_items.actions.delete.confirm_title", false,
                        "%name%", preset.getName()),
                confirmEvent -> plugin.getPlaceItemManager().deleteItem(preset.getId()).thenRun(() ->
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_items.deleted",
                                    "%group%", preset.getGroup(),
                                    "%name%", preset.getName()));
                            openPreviousMenu();
                        })
                ),
                cancelEvent -> open()
        ).setPreviousMenu(this).open();
    }
}
