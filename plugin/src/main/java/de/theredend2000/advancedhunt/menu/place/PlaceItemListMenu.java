package de.theredend2000.advancedhunt.menu.place;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.PagedMenu;
import de.theredend2000.advancedhunt.model.PlaceItem;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemSerializer;
import de.theredend2000.advancedhunt.util.ItemsAdderAdapter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class PlaceItemListMenu extends PagedMenu {

    private final String group;

    public PlaceItemListMenu(Player player, Main plugin, String group) {
        super(player, plugin);
        this.group = group;
        this.maxItemsPerPage = 28;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.place_items.list.title", false,
                "%group%", group);
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

        List<PlaceItem> items = plugin.getPlaceItemManager().getItemsInGroup(group);

        if (items.isEmpty()) {
            addMenuBorder();
            addStaticItem(22, getWarningIcon(plugin.getMessageManager().getMessage("gui.place_items.list.none.name", false),plugin.getMessageManager().getMessageList("gui.place_items.list.none.lore", false)));
        } else {
            addPagedButtons(items.size());
            int startIndex = page * maxItemsPerPage;
            int endIndex = Math.min(startIndex + maxItemsPerPage, items.size());
            this.hasNextPage = endIndex < items.size();

            for (int i = startIndex; i < endIndex; i++) {
                PlaceItem preset = items.get(i);
                ItemStack icon = ItemSerializer.deserialize(preset.getItemData());
                if (icon == null || icon.getType() == XMaterial.AIR.get() || (!icon.getType().isBlock() && !ItemsAdderAdapter.isCustomBlockItem(icon) && !ItemsAdderAdapter.isCustomFurniture(icon))) {
                    icon = new ItemStack(XMaterial.BARRIER.get());
                }
                icon.setAmount(1);

                icon = new ItemBuilder(icon.clone())
                        .setDisplayName(plugin.getMessageManager().getMessage("gui.place_items.list.preset.name", false,
                                "%name%", preset.getName()))
                        .setLore(plugin.getMessageManager().getMessageList("gui.place_items.list.preset.lore", false,
                                "%action%", plugin.getMessageManager().getMessage("gui.place_items.list.preset.action.give", false),
                                "%action2%", plugin.getMessageManager().getMessage("gui.place_items.list.preset.action.manage", false))
                                .toArray(new String[0]))
                    .build();

                ItemStack finalIcon = icon;
                addPagedItem(index++, finalIcon, click -> {
                    if (click.isRightClick()) {
                        new PlaceItemActionsMenu(playerMenuUtility, plugin, preset).setPreviousMenu(this).open();
                        return;
                    }
                    givePresetItem(preset);
                });
            }

            addMenuBorder();
        }

        // Add block preset to this group
        addButton(52, new ItemBuilder(XMaterial.EMERALD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_items.create_in_group.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_items.create_in_group.lore", false,
                        "%group%", group).toArray(new String[0]))
                .build(), click -> {
            if (!playerMenuUtility.hasPermission("advancedhunt.admin.place_items")) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                return;
            }

            new AddPlaceItemMenu(playerMenuUtility, plugin, group, this).open();
        }, "advancedhunt.admin.place_items");
    }

    String generateUniqueNameForCreate(String group, ItemStack item) {
        return generateUniqueName(group, item);
    }

    private String generateUniqueName(String group, ItemStack item) {
        String base = "Preset";
        if (item != null && item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            String display = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            if (display != null && !display.trim().isEmpty()) {
                base = display.trim();
            }
        } else if (item != null && item.getType() != null) {
            base = item.getType().name();
        }

        String candidate = base;
        int counter = 2;
        while (plugin.getPlaceItemManager().hasPresetNameInGroup(group, candidate)) {
            candidate = base + " " + counter;
            counter++;
        }
        return candidate;
    }

    private void givePresetItem(PlaceItem preset) {
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
}
