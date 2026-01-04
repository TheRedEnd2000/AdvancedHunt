package de.theredend2000.advancedhunt.menu.collection;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.PagedMenu;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class CollectionEditorMenu extends PagedMenu {

    public CollectionEditorMenu(Player playerMenuUtility, Main plugin) {
        super(playerMenuUtility, plugin);
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.editor.title");
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        // Handled by buttons
    }

    @Override
    public void setMenuItems() {
        addMenuBorder();

        List<String> collectionNames = plugin.getCollectionManager().getAllCollectionNames();

        if (collectionNames != null && !collectionNames.isEmpty()) {
            addPagedButtons(collectionNames.size());
            for (int i = 0; i < getMaxItemsPerPage(); i++) {
                index = getMaxItemsPerPage() * page + i;
                if (index >= collectionNames.size()) break;
                if (collectionNames.get(index) != null) {
                    String collectionName = collectionNames.get(index);
                    int finalI = i;
                    plugin.getCollectionManager().getCollectionByName(collectionName).ifPresent(collection -> {
                        String status = collection.isEnabled() ? 
                            plugin.getMessageManager().getMessage("gui.common.enabled") :
                            plugin.getMessageManager().getMessage("gui.common.disabled");
                        
                        // Show availability status
                        boolean isAvailable = plugin.getCollectionManager().isCollectionAvailable(collection);
                        String availabilityStatus = isAvailable ? 
                            plugin.getMessageManager().getMessage("collection.available", false) :
                            plugin.getMessageManager().getMessage("collection.not_available", false);
                        
                        String cron = collection.getProgressResetCron() != null ? 
                            "§e" + collection.getProgressResetCron() : 
                            plugin.getMessageManager().getMessage("gui.common.none");

                        int treasureCount = plugin.getTreasureManager().getTreasureCoresInCollection(collection.getId()).size();

                        addPagedItem(finalI, new ItemBuilder(XMaterial.CHEST.get())
                                .setDisplayName(plugin.getMessageManager().getMessage("gui.editor.collection_item.name", "%collection%", collection.getName()))
                                .setLore(plugin.getMessageManager().getMessageList("gui.editor.collection_item.lore", 
                                    "%status%", status,
                                    "%availability%", availabilityStatus,
                                    "%treasures%", String.valueOf(treasureCount),
                                    "%cron%", cron
                                ).toArray(new String[0]))
                                .build(), (e) -> {
                            new CollectionSettingsMenu(playerMenuUtility, plugin, collection).setPreviousMenu(this).open();
                        });
                    });
                }
            }
        }

        // Create Collection Button
        addButton(52, new ItemBuilder(XMaterial.EMERALD.get())
                .setDisplayName(plugin.getMessageManager().getMessage("gui.editor.create.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.editor.create.lore").toArray(new String[0]))
                .build(), (e) -> {
            plugin.getChatInputListener().requestInput(playerMenuUtility, (input) -> {
                plugin.getCollectionManager().createCollection(input).thenAccept(success -> {
                    if (success) {
                        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("command.create.success", "%name%", input));
                    } else {
                        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("command.create.failed"));
                    }
                    this.open();
                    //TODO FIX THE OPEN HERE!
                });
            });
        }, "advancedhunt.admin.collection.create");
    }
}
