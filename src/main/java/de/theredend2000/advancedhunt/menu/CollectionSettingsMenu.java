package de.theredend2000.advancedhunt.menu;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.cron.CronEditorMenu;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.model.CollectionRewardHolder;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class CollectionSettingsMenu extends Menu {

    private final Collection collection;

    public CollectionSettingsMenu(Player playerMenuUtility, Main plugin, Collection collection) {
        super(playerMenuUtility, plugin);
        this.collection = collection;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.settings.title", "%collection%", collection.getName());
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        // Handled by buttons
    }

    @Override
    public void setMenuItems() {
        fillBorders(super.FILLER_GLASS);

        // Rename
        addButton(10, new ItemBuilder(Material.NAME_TAG)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.rename.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.rename.lore", "%name%", collection.getName()).toArray(new String[0]))
                .build(), (e) -> {
            plugin.getChatInputListener().requestInput(playerMenuUtility, (input) -> {
                plugin.getCollectionManager().renameCollection(collection.getName(), input).thenAccept(success -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (success) {
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("rename.success", "%old_name%", collection.getName(), "%new_name%", input));
                            collection.setName(input); // Update local object
                            new CollectionSettingsMenu(playerMenuUtility, plugin, collection).open();
                        } else {
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("rename.failed"));
                            new CollectionSettingsMenu(playerMenuUtility, plugin, collection).open();
                        }
                    });
                });
            });
        }, "advancedhunt.admin.collection.rename");

        // Toggle Enabled
        String status = collection.isEnabled() ? 
            plugin.getMessageManager().getMessage("gui.common.enabled") : 
            plugin.getMessageManager().getMessage("gui.common.disabled");
            
        addButton(11, new ItemBuilder(collection.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.status.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.status.lore", "%status%", status).toArray(new String[0]))
                .build(), (e) -> {
            collection.setEnabled(!collection.isEnabled());
            plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    refresh();
                });
            });
        });

        // Completion Rewards
        addButton(12, new ItemBuilder(Material.CHEST)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.rewards.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.rewards.lore").toArray(new String[0]))
                .build(), (e) -> {
            new RewardsMenu(playerMenuUtility, plugin, new CollectionRewardHolder(plugin, collection))
                    .setTitleKey("gui.rewards.collection_title")
                    .setPreviousMenu(this)
                    .open();
        });

        // Toggle Single Player Find
        String spfStatus = collection.isSinglePlayerFind() ? 
            plugin.getMessageManager().getMessage("gui.common.enabled") : 
            plugin.getMessageManager().getMessage("gui.common.disabled");
            
        addButton(13, new ItemBuilder(collection.isSinglePlayerFind() ? Material.ENDER_PEARL : Material.ENDER_EYE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.single_player_find.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.single_player_find.lore", "%status%", spfStatus).toArray(new String[0]))
                .build(), (e) -> {
            collection.setSinglePlayerFind(!collection.isSinglePlayerFind());
            plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    refresh();
                });
            });
        });

        // ACT Schedule Rules
        int ruleCount = collection.getActRules().size();
        addButton(14, new ItemBuilder(Material.REPEATING_COMMAND_BLOCK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.act_rules.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.act_rules.lore", false,
                    "%count%", String.valueOf(ruleCount)
                ).toArray(new String[0]))
                .build(), (e) -> {
            new ActRulesMenu(playerMenuUtility, plugin, collection).setPreviousMenu(this).open();
        });

        // Progress Reset Cron
        String cron = collection.getProgressResetCron() != null ? 
            "§f" + collection.getProgressResetCron() : 
            plugin.getMessageManager().getMessage("gui.common.none", false);

        addButton(15, new ItemBuilder(Material.CLOCK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.progress_reset_cron.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.progress_reset_cron.lore", false, "%cron%", cron).toArray(new String[0]))
                .build(), (e) -> {
            new ProgressResetCronMenu(playerMenuUtility, plugin, collection).setPreviousMenu(this).open();
        });

        // Delete Collection
        addButton(16, new ItemBuilder(Material.TNT)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.delete.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.delete.lore").toArray(new String[0]))
                .build(), (e) -> {
            new ConfirmationMenu(playerMenuUtility, plugin, 
                plugin.getMessageManager().getMessage("gui.settings.delete.confirm_title"), 
                (confirmEvent) -> {
                    plugin.getCollectionManager().deleteCollection(collection.getId()).thenRun(() -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.settings.delete.success", "%collection%", collection.getName()));
                            new CollectionEditorMenu(playerMenuUtility, plugin).open();
                        });
                    });
                },
                (cancelEvent) -> {
                    playerMenuUtility.closeInventory();
                }
            ).setPreviousMenu(this).open();
        }, "advancedhunt.admin.collection.delete");
    }

    /**
     * Wrapper class for editing progress reset cron (reuses CronEditorMenu)
     */
    private static class ProgressResetCronMenu extends CronEditorMenu {
        public ProgressResetCronMenu(Player player, Main plugin, Collection collection) {
            super(player, plugin, collection);
        }

        @Override
        public String getMenuName() {
            return plugin.getMessageManager().getMessage("gui.progress_reset_cron.title", false);
        }
    }
}
