package de.theredend2000.advancedhunt.menu.place;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.common.SingleItemInputMenu;
import de.theredend2000.advancedhunt.util.ItemSerializer;
import de.theredend2000.advancedhunt.util.ItemsAdderAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * A menu for creating a place preset by placing a block item in the center slot.
 * The block is not consumed; it is returned to the player on confirm/cancel/close.
 */
public class AddPlaceItemMenu extends SingleItemInputMenu {

    private final String group;
    private final PlaceItemListMenu parentMenu;

    public AddPlaceItemMenu(Player player, Main plugin, String group, PlaceItemListMenu parentMenu) {
        super(player, plugin, parentMenu, false);
        this.group = group;
        this.parentMenu = parentMenu;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.place_items.add.title", false,
                "%group%", group);
    }

    @Override
    protected String getGuiKeyBase() {
        return "gui.place_items.add";
    }

    @Override
    protected String getNoItemErrorKey() {
        return "error.place_items.no_item";
    }

    @Override
    protected boolean isValidItem(ItemStack item) {
        if (item == null || item.getType() == XMaterial.AIR.get()) return false;
        return item.getType().isBlock() || ItemsAdderAdapter.isCustomBlockItem(item) || ItemsAdderAdapter.isCustomFurniture(item);
    }

    @Override
    protected String getInvalidItemErrorKey() {
        return "error.place_items.not_a_block";
    }

    @Override
    protected void onConfirm(ItemStack itemSnapshot) {
        if (!playerMenuUtility.hasPermission("advancedhunt.admin.place_items")) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
            return;
        }

        // Save as a single block (amount 1) while preserving ItemAdder NBT/meta
        ItemStack blockSnapshot = itemSnapshot.clone();
        blockSnapshot.setAmount(1);

        String itemData = ItemSerializer.serialize(blockSnapshot);
        if (itemData == null || itemData.trim().isEmpty()) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_items.serialize_failed"));
            return;
        }

        String name = parentMenu.generateUniqueNameForCreate(group, blockSnapshot);

        plugin.getPlaceItemManager().createItem(group, name, itemData).whenComplete((ok, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (ex != null || !Boolean.TRUE.equals(ok)) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_items.create_failed"));
                parentMenu.open();
                return;
            }

            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_items.created",
                    "%group%", group,
                    "%name%", name));
            parentMenu.open();
        }));
    }
}
