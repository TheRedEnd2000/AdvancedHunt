package de.theredend2000.advancedhunt.menu.place;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.menu.common.ConfirmationMenu;
import de.theredend2000.advancedhunt.model.PlacePreset;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

        addButton(10, new ItemBuilder(Material.CHEST)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.actions.give.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.actions.give.lore", false).toArray(new String[0]))
                .build(), e -> {
            give();
            openPreviousMenu();
        });

        addButton(12, new ItemBuilder(Material.NAME_TAG)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.actions.rename.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.actions.rename.lore", false,
                        "%current%", preset.getName()).toArray(new String[0]))
                .build(), e -> handleRename());

        addButton(14, new ItemBuilder(Material.BOOK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.actions.move_group.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.actions.move_group.lore", false,
                        "%current%", preset.getGroup()).toArray(new String[0]))
                .build(), e -> handleMoveGroup());

        addButton(16, new ItemBuilder(Material.TNT)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.actions.delete.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.actions.delete.lore", false).toArray(new String[0]))
                .build(), e -> handleDelete());

        fillBackground(FILLER_GLASS);
    }

    private void give() {
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

    private void handleRename() {
        if (!playerMenuUtility.hasPermission("advancedhunt.admin.place_presets")) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
            return;
        }

        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_presets.prompt_rename",
                "%name%", preset.getName()));
        plugin.getChatInputListener().requestInput(playerMenuUtility, input -> {
            String trimmed = input == null ? null : input.trim();
            if (trimmed == null || trimmed.isEmpty()) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.invalid_name"));
                Bukkit.getScheduler().runTask(plugin, this::open);
                return;
            }

            if (plugin.getPlacePresetManager().hasPresetNameInGroup(preset.getGroup(), trimmed)) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.duplicate_name"));
                Bukkit.getScheduler().runTask(plugin, this::open);
                return;
            }

            preset.setName(trimmed);
            plugin.getPlacePresetManager().savePreset(preset).whenComplete((v, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (ex != null) {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.save_failed"));
                    open();
                    return;
                }
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_presets.renamed",
                        "%group%", preset.getGroup(),
                        "%name%", preset.getName()));
                openPreviousMenu();
            }));
        });
    }

    private void handleMoveGroup() {
        if (!playerMenuUtility.hasPermission("advancedhunt.admin.place_presets")) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
            return;
        }

        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_presets.prompt_move_group",
                "%group%", preset.getGroup()));
        plugin.getChatInputListener().requestInput(playerMenuUtility, input -> {
            String trimmed = input == null ? null : input.trim();
            if (trimmed == null || trimmed.isEmpty()) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.invalid_group"));
                Bukkit.getScheduler().runTask(plugin, this::open);
                return;
            }

            // Prevent duplicate names within target group
            if (plugin.getPlacePresetManager().hasPresetNameInGroup(trimmed, preset.getName())) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.duplicate_name"));
                Bukkit.getScheduler().runTask(plugin, this::open);
                return;
            }

            preset.setGroup(trimmed);
            plugin.getPlacePresetManager().savePreset(preset).whenComplete((v, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (ex != null) {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.save_failed"));
                    open();
                    return;
                }
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_presets.moved",
                        "%group%", preset.getGroup(),
                        "%name%", preset.getName()));
                openPreviousMenu();
            }));
        });
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
