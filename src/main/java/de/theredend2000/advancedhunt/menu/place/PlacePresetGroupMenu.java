package de.theredend2000.advancedhunt.menu.place;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.PagedMenu;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PlacePresetGroupMenu extends PagedMenu {

    public PlacePresetGroupMenu(Player player, Main plugin) {
        super(player, plugin);
        this.maxItemsPerPage = 28;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.place_presets.groups.title", false);
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

        List<String> groups = plugin.getPlacePresetManager().getGroups();

        if (groups.isEmpty()) {
            addStaticItem(22, getWarningIcon(plugin.getMessageManager().getMessage("gui.place_presets.groups.none.name", false),plugin.getMessageManager().getMessageList("gui.place_presets.groups.none.lore", false)));
        } else {
            addPagedButtons(groups.size());
            int startIndex = page * maxItemsPerPage;
            int endIndex = Math.min(startIndex + maxItemsPerPage, groups.size());
            this.hasNextPage = endIndex < groups.size();

            for (int i = startIndex; i < endIndex; i++) {
                String group = groups.get(i);

                ItemStack icon = new ItemBuilder(Material.CHEST)
                        .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.groups.group.name", false,
                                "%group%", group))
                        .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.groups.group.lore", false,
                                "%count%", String.valueOf(plugin.getPlacePresetManager().getPresetsInGroup(group).size()),
                        "%action%", plugin.getMessageManager().getMessage("gui.place_presets.groups.group.action.open", false),
                        "%action2%", plugin.getMessageManager().getMessage("gui.place_presets.groups.group.action.manage", false)))
                        .build();

                addPagedItem(index++, icon, click -> {
                    if (click.isRightClick()) {
                    new PlacePresetGroupActionsMenu(playerMenuUtility, plugin, group)
                        .setPreviousMenu(this)
                        .open();
                    return;
                    }
                    new PlacePresetListMenu(playerMenuUtility, plugin, group)
                        .setPreviousMenu(this)
                        .open();
                });
            }
        }

        // Create group
        addButton(52, new ItemBuilder(Material.EMERALD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.create.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.create.lore", false).toArray(new String[0]))
                .build(), click -> {
            if (!playerMenuUtility.hasPermission("advancedhunt.admin.place_presets")) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                return;
            }

            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_presets.prompt_group"));
            plugin.getChatInputListener().requestInput(playerMenuUtility, groupInput -> {
                String group = groupInput == null ? null : groupInput.trim();
                if (group == null || group.isEmpty()) {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.invalid_group"));
                    Bukkit.getScheduler().runTask(plugin, this::open);
                    return;
                }

                plugin.getPlacePresetManager().createGroup(group).whenComplete((ok, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (ex != null || !Boolean.TRUE.equals(ok)) {
                        if (plugin.getPlacePresetManager().hasGroup(group)) {
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.duplicate_group"));
                        } else {
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.create_group_failed"));
                        }
                        open();
                        return;
                    }

                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_presets.group_created",
                            "%group%", group));
                    open();
                }));
            });
        }, "advancedhunt.admin.place_presets");
    }
}
