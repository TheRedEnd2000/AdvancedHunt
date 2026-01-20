package de.theredend2000.advancedhunt.menu.place;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.PagedMenu;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PlaceItemGroupMenu extends PagedMenu {

    public PlaceItemGroupMenu(Player player, Main plugin) {
        super(player, plugin);
        this.maxItemsPerPage = 28;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.place_items.groups.title", false);
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        // handled by buttons
    }

    @Override
    public void setMenuItems() {
        index = 0;

        addMenuBorder();

        List<String> groups = plugin.getPlaceItemManager().getGroups();

        if (groups.isEmpty()) {
            addStaticItem(22, getWarningIcon(plugin.getMessageManager().getMessage("gui.place_items.groups.none.name", false),plugin.getMessageManager().getMessageList("gui.place_items.groups.none.lore", false)));
        } else {
            addPagedButtons(groups.size());
            int startIndex = page * maxItemsPerPage;
            int endIndex = Math.min(startIndex + maxItemsPerPage, groups.size());
            this.hasNextPage = endIndex < groups.size();

            for (int i = startIndex; i < endIndex; i++) {
                String group = groups.get(i);

                ItemStack icon = new ItemBuilder(XMaterial.CHEST)
                        .setDisplayName(plugin.getMessageManager().getMessage("gui.place_items.groups.group.name", false,
                                "%group%", group))
                        .setLore(plugin.getMessageManager().getMessageList("gui.place_items.groups.group.lore", false,
                                "%count%", String.valueOf(plugin.getPlaceItemManager().getItemsInGroup(group).size()),
                        "%action%", plugin.getMessageManager().getMessage("gui.place_items.groups.group.action.open", false),
                        "%action2%", plugin.getMessageManager().getMessage("gui.place_items.groups.group.action.manage", false)))
                        .build();

                addPagedItem(index++, icon, click -> {
                    if (click.isRightClick()) {
                    new PlaceItemGroupActionsMenu(playerMenuUtility, plugin, group)
                        .setPreviousMenu(this)
                        .open();
                    return;
                    }
                    new PlaceItemListMenu(playerMenuUtility, plugin, group)
                        .setPreviousMenu(this)
                        .open();
                });
            }
        }

        // Create group
        addButton(52, new ItemBuilder(XMaterial.EMERALD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_items.create.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_items.create.lore", false).toArray(new String[0]))
                .build(), click -> {
            if (!playerMenuUtility.hasPermission("advancedhunt.admin.place_items")) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                return;
            }

            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_items.prompt_group"));
            plugin.getChatInputListener().requestInput(playerMenuUtility, groupInput -> {
                String group = groupInput == null ? null : groupInput.trim();
                if (group == null || group.isEmpty()) {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_items.invalid_group"));
                    Bukkit.getScheduler().runTask(plugin, this::open);
                    return;
                }

                plugin.getPlaceItemManager().createGroup(group).whenComplete((ok, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (ex != null || !Boolean.TRUE.equals(ok)) {
                        if (plugin.getPlaceItemManager().hasGroup(group)) {
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_items.duplicate_group"));
                        } else {
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_items.create_group_failed"));
                        }
                        open();
                        return;
                    }

                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_items.group_created",
                            "%group%", group));
                    open();
                }));
            });
        }, "advancedhunt.admin.place_items");
    }
}
