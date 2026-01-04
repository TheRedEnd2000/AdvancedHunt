package de.theredend2000.advancedhunt.menu.place;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.menu.common.ConfirmationMenu;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class PlacePresetGroupActionsMenu extends Menu {

    private final String group;

    public PlacePresetGroupActionsMenu(Player player, Main plugin, String group) {
        super(player, plugin);
        this.group = group;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.place_presets.group_actions.title", false,
                "%group%", group);
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

        addButton(11, new ItemBuilder(XMaterial.NAME_TAG.get())
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.group_actions.rename.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.group_actions.rename.lore", false,
                        "%current%", group).toArray(new String[0]))
                .build(), e -> handleRename());

        addButton(15, new ItemBuilder(XMaterial.TNT.get())
                .setDisplayName(plugin.getMessageManager().getMessage("gui.place_presets.group_actions.delete.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.place_presets.group_actions.delete.lore", false,
                        "%count%", String.valueOf(plugin.getPlacePresetManager().getPresetsInGroup(group).size())).toArray(new String[0]))
                .build(), e -> handleDelete());

        fillBackground(FILLER_GLASS);
    }

    private void handleRename() {
        if (!playerMenuUtility.hasPermission("advancedhunt.admin.place_presets")) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
            return;
        }

        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_presets.prompt_rename_group",
                "%group%", group));
        plugin.getChatInputListener().requestInput(playerMenuUtility, input -> {
            String trimmed = input == null ? null : input.trim();
            if (trimmed == null || trimmed.isEmpty()) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.invalid_group"));
                Bukkit.getScheduler().runTask(plugin, this::open);
                return;
            }

            if (plugin.getPlacePresetManager().hasGroup(trimmed)) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.duplicate_group"));
                Bukkit.getScheduler().runTask(plugin, this::open);
                return;
            }

            plugin.getPlacePresetManager().renameGroup(group, trimmed).whenComplete((ok, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (ex != null || !Boolean.TRUE.equals(ok)) {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.place_presets.rename_group_failed"));
                    open();
                    return;
                }

                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_presets.group_renamed",
                        "%old%", group,
                        "%group%", trimmed));
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
                plugin.getMessageManager().getMessage("gui.place_presets.group_actions.delete.confirm_title", false,
                        "%group%", group),
                confirmEvent -> plugin.getPlacePresetManager().deleteGroup(group).thenRun(() ->
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.place_presets.group_deleted",
                                    "%group%", group));
                            openPreviousMenu();
                        })
                ),
                cancelEvent -> open()
        ).setPreviousMenu(this).open();
    }
}
