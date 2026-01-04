package de.theredend2000.advancedhunt.menu.place;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.menu.common.ConfirmationMenu;
import de.theredend2000.advancedhunt.model.PlacePreset;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemSerializer;
import de.theredend2000.advancedhunt.util.ItemsAdderAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class PlacePresetActionsMenu extends Menu {

    private final PlacePreset preset;

    public PlacePresetActionsMenu(Player player, Main plugin, PlacePreset preset) {
        super(player, plugin);
        this.preset = preset;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.place_presets.actions.title", false,
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

        addButton(12, new ItemBuilder(XMaterial.CHEST.get())
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.actions.give.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.actions.give.lore", false).toArray(new String[0]))
                .build(), e -> {
            give();
            openPreviousMenu();
        });

        addButton(14, new ItemBuilder(XMaterial.TNT.get())
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.actions.delete.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.actions.delete.lore", false).toArray(new String[0]))
                .build(), e -> handleDelete());

        fillBackground(FILLER_GLASS);
    }

    private void give() {
        ItemStack item = ItemSerializer.deserialize(preset.getItemData());
        if (item == null || item.getType() == XMaterial.AIR.get()) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.deserialize_failed"));
            return;
        }

        if (!item.getType().isBlock() && !ItemsAdderAdapter.isCustomBlockItem(item) && !ItemsAdderAdapter.isCustomFurniture(item)) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.not_a_block"));
            return;
        }

        ItemStack toGive = item.clone();
        toGive.setAmount(1);
        var leftovers = playerMenuUtility.getInventory().addItem(toGive);
        for (ItemStack leftover : leftovers.values()) {
            playerMenuUtility.getWorld().dropItemNaturally(playerMenuUtility.getLocation(), leftover);
        }

        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_presets.given",
                "%group%", preset.getGroup(),
                "%name%", preset.getName()));
    }

    private void handleDelete() {
        if (!playerMenuUtility.hasPermission("advancedhunt.admin.place_presets")) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
            return;
        }

        new ConfirmationMenu(playerMenuUtility, plugin,
                plugin.getMessageManager().getMessage("gui.place_presets.actions.delete.confirm_title", false,
                        "%name%", preset.getName()),
                confirmEvent -> plugin.getPlacePresetManager().deletePreset(preset.getId()).thenRun(() ->
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_presets.deleted",
                                    "%group%", preset.getGroup(),
                                    "%name%", preset.getName()));
                            openPreviousMenu();
                        })
                ),
                cancelEvent -> open()
        ).setPreviousMenu(this).open();
    }
}
