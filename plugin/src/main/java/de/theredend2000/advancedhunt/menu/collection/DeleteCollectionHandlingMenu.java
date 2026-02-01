package de.theredend2000.advancedhunt.menu.collection;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.TreasureDeletionType;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.menu.common.ConfirmationMenu;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class DeleteCollectionHandlingMenu extends Menu {

    private final Collection collection;
    private boolean processing;

    public DeleteCollectionHandlingMenu(org.bukkit.entity.Player playerMenuUtility, Main plugin, Collection collection) {
        super(playerMenuUtility, plugin);
        this.collection = collection;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.settings.delete_handling.title", false);
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
        fillBorders(super.FILLER_GLASS);

        addHandlingButton(11,
            XMaterial.CHEST,
            "gui.settings.delete_handling.keep.name",
            "gui.settings.delete_handling.keep.lore",
            "gui.settings.delete_handling.keep.confirm_title",
            TreasureDeletionType.KEEP_ALL
        );

        addHandlingButton(13,
            XMaterial.PLAYER_HEAD,
            "gui.settings.delete_handling.remove_heads.name",
            "gui.settings.delete_handling.remove_heads.lore",
            "gui.settings.delete_handling.remove_heads.confirm_title",
            TreasureDeletionType.REMOVE_HEADS
        );

        addHandlingButton(15,
            XMaterial.TNT,
            "gui.settings.delete_handling.remove_blocks.name",
            "gui.settings.delete_handling.remove_blocks.lore",
            "gui.settings.delete_handling.remove_blocks.confirm_title",
            TreasureDeletionType.REMOVE_BLOCKS_AND_FURNITURE
        );
    }

    private void addHandlingButton(
        int slot,
        XMaterial material,
        String nameKey,
        String loreKey,
        String confirmTitleKey,
        TreasureDeletionType handling
    ) {
        ItemStack item = new ItemBuilder(material)
            .setDisplayName(plugin.getMessageManager().getMessage(nameKey, false))
            .setLore(plugin.getMessageManager().getMessageList(loreKey, false).toArray(new String[0]))
            .build();

        addButton(slot, item, (e) -> {
            if (processing) return;
            if (collection == null) return;

            new ConfirmationMenu(playerMenuUtility, plugin,
                plugin.getMessageManager().getMessage(confirmTitleKey, false, "%collection%", collection.getName()),
                (confirmEvent) -> runDelete(confirmEvent, handling),
                (cancelEvent) -> openPreviousMenu()
            ).setPreviousMenu(this).open();
        });
    }

    private void runDelete(InventoryClickEvent e, TreasureDeletionType handling) {
        if (processing) return;
        processing = true;

        plugin.getCollectionManager().deleteCollection(collection.getId(), handling).whenComplete((v, ex) -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                processing = false;
                if (ex != null) {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.generic"));
                    openPreviousMenu();
                    return;
                }

                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                    "feedback.settings.delete.success",
                    "%collection%",
                    collection.getName()
                ));
                new CollectionEditorMenu(playerMenuUtility, plugin).open();
            });
        });
    }
}
