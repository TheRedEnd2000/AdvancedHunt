package de.theredend2000.advancedhunt.menu;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.cron.CronEditorMenu;
import de.theredend2000.advancedhunt.model.*;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
        return 36;
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
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("command.rename.success", "%old_name%", collection.getName(), "%new_name%", input));
                            collection.setName(input); // Update local object
                            new CollectionSettingsMenu(playerMenuUtility, plugin, collection).open();
                        } else {
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("command.rename.failed"));
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

        // Default Treasure Reward Preset (applies to newly created treasures in this collection)
        String defaultPresetName = plugin.getMessageManager().getMessage("gui.common.none", false);
        UUID defaultPresetId = collection.getDefaultTreasureRewardPresetId();
        if (defaultPresetId != null) {
            defaultPresetName = plugin.getRewardPresetManager()
                    .getPreset(RewardPresetType.TREASURE, defaultPresetId)
                    .map(RewardPreset::getName)
                    .orElse(plugin.getMessageManager().getMessage("gui.presets.unknown", false));
        }

        addButton(19, new ItemBuilder(Material.WRITABLE_BOOK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.default_treasure_preset.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.default_treasure_preset.lore", false,
                        "%preset%", "§f" + defaultPresetName).toArray(new String[0]))
                .build(), (e) -> {
            if (!playerMenuUtility.hasPermission("advancedhunt.admin")) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                return;
            }

            if (e.isRightClick()) {
                collection.setDefaultTreasureRewardPresetId(null);
                plugin.getCollectionManager().saveCollection(collection).thenRun(() -> Bukkit.getScheduler().runTask(plugin, this::refresh));
                return;
            }

            new RewardPresetListMenu(playerMenuUtility, plugin, RewardPresetType.TREASURE, selected -> {
                collection.setDefaultTreasureRewardPresetId(selected.getId());
                plugin.getCollectionManager().saveCollection(collection).thenRun(() -> Bukkit.getScheduler().runTask(plugin, this::refresh));
            }, collection).setPreviousMenu(this).open();
        });

        // Override all treasures rewards with a preset
        addButton(20, new ItemBuilder(Material.ANVIL)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.override_treasure_rewards.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.override_treasure_rewards.lore", false).toArray(new String[0]))
                .build(), (e) -> {
            if (!playerMenuUtility.hasPermission("advancedhunt.admin")) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                return;
            }

                new RewardPresetListMenu(playerMenuUtility, plugin, RewardPresetType.TREASURE, selected -> {
                new ConfirmationMenu(playerMenuUtility, plugin,
                        plugin.getMessageManager().getMessage("gui.settings.override_treasure_rewards.confirm_title", false,
                                "%name%", selected.getName()),
                        (confirmEvent) -> runBulkOverride(selected),
                        (cancelEvent) -> open()
                ).setPreviousMenu(this).open();
                }, collection).setPreviousMenu(this).open();
        });
    }

    private void runBulkOverride(RewardPreset preset) {
        List<TreasureCore> cores = plugin.getTreasureManager().getTreasureCoresInCollection(collection.getId());
        if (cores.isEmpty()) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.override.none"));
            open();
            return;
        }

        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.override.started",
                "%count%", String.valueOf(cores.size()),
                "%name%", preset.getName()));

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (TreasureCore core : cores) {
            CompletableFuture<Void> f = plugin.getTreasureManager().getFullTreasureAsync(core.getId()).thenCompose(oldTreasure -> {
                if (oldTreasure == null) {
                    return CompletableFuture.completedFuture(null);
                }
                Treasure newTreasure = new Treasure(
                        oldTreasure.getId(),
                        oldTreasure.getCollectionId(),
                        oldTreasure.getLocation(),
                        new ArrayList<>(preset.getRewards()),
                        oldTreasure.getNbtData(),
                        oldTreasure.getMaterial(),
                        oldTreasure.getBlockState()
                );
                return plugin.getTreasureManager().updateTreasure(oldTreasure, newTreasure);
            });
            futures.add(f);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete((v, ex) -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (ex != null) {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.override.failed"));
                } else {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.override.done",
                            "%count%", String.valueOf(cores.size()),
                            "%name%", preset.getName()));
                }
                open();
            });
        });
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
            return plugin.getMessageManager().getMessage("gui.settings.reset_schedule.title", false);
        }
    }
}
