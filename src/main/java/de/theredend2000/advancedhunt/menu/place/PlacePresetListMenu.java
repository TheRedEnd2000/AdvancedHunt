package de.theredend2000.advancedhunt.menu.place;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.PagedMenu;
import de.theredend2000.advancedhunt.model.PlacePreset;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemSerializer;
import de.theredend2000.advancedhunt.util.ItemsAdderAdapter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PlacePresetListMenu extends PagedMenu {

    private final String group;

    public PlacePresetListMenu(Player player, Main plugin, String group) {
        super(player, plugin);
        this.group = group;
        this.maxItemsPerPage = 28;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.place_presets.list.title", false,
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

        List<PlacePreset> presets = plugin.getPlacePresetManager().getPresetsInGroup(group);

        if (presets.isEmpty()) {
            addMenuBorder();
            addStaticItem(22, getWarningIcon(plugin.getMessageManager().getMessage("gui.place_presets.list.none.name", false),plugin.getMessageManager().getMessageList("gui.place_presets.list.none.lore", false)));
        } else {
            addPagedButtons(presets.size());
            int startIndex = page * maxItemsPerPage;
            int endIndex = Math.min(startIndex + maxItemsPerPage, presets.size());
            this.hasNextPage = endIndex < presets.size();

            for (int i = startIndex; i < endIndex; i++) {
                PlacePreset preset = presets.get(i);
                ItemStack icon = ItemSerializer.deserialize(preset.getItemData());
                if (icon == null || icon.getType() == XMaterial.AIR.get() || (!icon.getType().isBlock() && !ItemsAdderAdapter.isCustomBlockItem(icon) && !ItemsAdderAdapter.isCustomFurniture(icon))) {
                    icon = new ItemStack(XMaterial.BARRIER.get());
                }
                icon.setAmount(1);

                icon = new ItemBuilder(icon.clone())
                        .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.list.preset.name", false,
                                "%name%", preset.getName()))
                        .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.list.preset.lore", false,
                                "%action%", plugin.getMessageManager().getMessage("gui.place_presets.list.preset.action.give", false),
                                "%action2%", plugin.getMessageManager().getMessage("gui.place_presets.list.preset.action.manage", false))
                                .toArray(new String[0]))
                    .build();

                ItemStack finalIcon = icon;
                addPagedItem(index++, finalIcon, click -> {
                    if (click.isRightClick()) {
                        new PlacePresetActionsMenu(playerMenuUtility, plugin, preset).setPreviousMenu(this).open();
                        return;
                    }
                    givePresetItem(preset);
                });
            }

            addMenuBorder();
        }

        // Add block preset to this group
        addButton(52, new ItemBuilder(XMaterial.EMERALD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.create_in_group.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.create_in_group.lore", false,
                        "%group%", group).toArray(new String[0]))
                .build(), click -> {
            if (!playerMenuUtility.hasPermission("advancedhunt.admin.place_presets")) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                return;
            }

            new AddPlacePresetMenu(playerMenuUtility, plugin, group, this).open();
        }, "advancedhunt.admin.place_presets");
    }

    String generateUniqueNameForCreate(String group, ItemStack item) {
        return generateUniqueName(group, item);
    }

    private String generateUniqueName(String group, ItemStack item) {
        String base = "Preset";
        if (item != null && item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            String display = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            if (display != null && !display.isBlank()) {
                base = display.trim();
            }
        } else if (item != null && item.getType() != null) {
            base = item.getType().name();
        }

        String candidate = base;
        int counter = 2;
        while (plugin.getPlacePresetManager().hasPresetNameInGroup(group, candidate)) {
            candidate = base + " " + counter;
            counter++;
        }
        return candidate;
    }

    private void givePresetItem(PlacePreset preset) {
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
}
