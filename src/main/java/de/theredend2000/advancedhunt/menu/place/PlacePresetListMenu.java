package de.theredend2000.advancedhunt.menu.place;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.PagedMenu;
import de.theredend2000.advancedhunt.model.PlacePreset;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
            ItemStack none = new ItemBuilder(Material.BARRIER)
                    .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.list.none.name", false))
                    .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.list.none.lore", false))
                    .build();
            addStaticItem(22, none);
        } else {
            addPagedButtons(presets.size());
            int startIndex = page * maxItemsPerPage;
            int endIndex = Math.min(startIndex + maxItemsPerPage, presets.size());
            this.hasNextPage = endIndex < presets.size();

            for (int i = startIndex; i < endIndex; i++) {
                PlacePreset preset = presets.get(i);
                ItemStack icon = ItemSerializer.deserialize(preset.getItemData());
                if (icon == null || icon.getType() == Material.AIR) {
                    icon = new ItemStack(Material.BARRIER);
                }

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

        // Add preset to this group (captures held item)
        addButton(52, new ItemBuilder(Material.EMERALD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.create_in_group.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.create_in_group.lore", false,
                        "%group%", group).toArray(new String[0]))
                .build(), click -> {
            if (!playerMenuUtility.hasPermission("advancedhunt.admin.place_presets")) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                return;
            }

            ItemStack held = playerMenuUtility.getInventory().getItemInMainHand();
            if (held == null || held.getType() == Material.AIR) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.no_item"));
                Bukkit.getScheduler().runTask(plugin, this::open);
                return;
            }

            ItemStack heldSnapshot = held.clone();

            String itemData = ItemSerializer.serialize(heldSnapshot);
            if (itemData == null || itemData.isBlank()) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.serialize_failed"));
                Bukkit.getScheduler().runTask(plugin, this::open);
                return;
            }

            String name = generateUniqueName(group, heldSnapshot);

            plugin.getPlacePresetManager().createPreset(group, name, itemData).whenComplete((ok, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (ex != null || !Boolean.TRUE.equals(ok)) {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.create_failed"));
                    open();
                    return;
                }
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_presets.created",
                        "%group%", group,
                        "%name%", name));
                open();
            }));
        }, "advancedhunt.admin.place_presets");
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
        if (item == null || item.getType() == Material.AIR) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.deserialize_failed"));
            return;
        }

        ItemStack toGive = item.clone();
        var leftovers = playerMenuUtility.getInventory().addItem(toGive);
        for (ItemStack leftover : leftovers.values()) {
            playerMenuUtility.getWorld().dropItemNaturally(playerMenuUtility.getLocation(), leftover);
        }

        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_presets.given",
                "%group%", preset.getGroup(),
                "%name%", preset.getName()));
    }
}
