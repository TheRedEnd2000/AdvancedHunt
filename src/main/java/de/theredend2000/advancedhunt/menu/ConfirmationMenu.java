package de.theredend2000.advancedHunt.menu;

import de.theredend2000.advancedHunt.Main;
import de.theredend2000.advancedHunt.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.Consumer;

public class ConfirmationMenu extends Menu {

    private final String title;
    private final Consumer<InventoryClickEvent> onConfirm;
    private final Consumer<InventoryClickEvent> onCancel;

    public ConfirmationMenu(Player player, Main plugin, String title, Consumer<InventoryClickEvent> onConfirm, Consumer<InventoryClickEvent> onCancel) {
        super(player, plugin);
        this.title = title;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    public String getMenuName() {
        return title;
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        // Handled by buttons
    }

    @Override
    public void setMenuItems() {
        fillBorders(super.FILLER_GLASS);

        // Confirm Button
        addButton(11, new ItemBuilder(Material.LIME_TERRACOTTA)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.common.confirm"))
                .build(), (e) -> {
            if (onConfirm != null) {
                onConfirm.accept(e);
            }
        });

        // Cancel Button
        addButton(15, new ItemBuilder(Material.RED_TERRACOTTA)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.common.cancel"))
                .build(), (e) -> {
            if (onCancel != null) {
                onCancel.accept(e);
            }
        });
    }
}
