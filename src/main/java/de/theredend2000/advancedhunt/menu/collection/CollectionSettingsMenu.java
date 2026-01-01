package de.theredend2000.advancedhunt.menu.collection;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.menu.act.ActRulesMenu;
import de.theredend2000.advancedhunt.menu.common.ConfirmationMenu;
import de.theredend2000.advancedhunt.menu.cron.CronEditorMenu;
import de.theredend2000.advancedhunt.menu.reward.RewardPresetListMenu;
import de.theredend2000.advancedhunt.menu.reward.RewardsMenu;
import de.theredend2000.advancedhunt.model.*;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

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
    public void handleMenu(InventoryClickEvent e) {
        // Handled by buttons
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void setMenuItems() {
        fillBorders(super.FILLER_GLASS);

        // ==================== BASIC SETTINGS ====================
        addStaticItem(10, new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.categories.basic.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.categories.basic.lore", false))
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGJmOGI2Mjc3Y2QzNjI2NjI4M2NiNWE5ZTY5NDM5NTNjNzgzZTZmZjdkNmEyZDU5ZDE1YWQwNjk3ZTkxZDQzYyJ9fX0=")
                .build());

        addStaticItem(11, super.EXTRA_GLASS);

        // Rename
        addButton(12, new ItemBuilder(Material.NAME_TAG)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.rename.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.rename.lore", "%name%", collection.getName()).toArray(new String[0]))
                .build(), (e) -> {
            plugin.getChatInputListener().requestInput(playerMenuUtility, (input) -> {
                plugin.getCollectionManager().renameCollection(collection.getName(), input).thenAccept(success -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (success) {
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("command.rename.success", "%old_name%", collection.getName(), "%new_name%", input));
                            collection.setName(input);
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

        addButton(13, new ItemBuilder(collection.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.status.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.status.lore", "%status%", status).toArray(new String[0]))
                .build(), (e) -> {
            collection.setEnabled(!collection.isEnabled());
            plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, this::refresh);
            });
        });

        // Toggle Single Player Find
        String spfStatus = collection.isSinglePlayerFind() ?
                plugin.getMessageManager().getMessage("gui.common.enabled") :
                plugin.getMessageManager().getMessage("gui.common.disabled");

        addButton(14, new ItemBuilder(collection.isSinglePlayerFind() ? Material.ENDER_PEARL : Material.ENDER_EYE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.single_player_find.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.single_player_find.lore", "%status%", spfStatus).toArray(new String[0]))
                .build(), (e) -> {
            collection.setSinglePlayerFind(!collection.isSinglePlayerFind());
            plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, this::refresh);
            });
        });

        // Delete Collection
        addButton(15, new ItemBuilder(Material.TNT)
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
                    (cancelEvent) -> playerMenuUtility.closeInventory()
            ).setPreviousMenu(this).open();
        }, "advancedhunt.admin.collection.delete");

        // ==================== ACT CONFIGURATION ====================
        addStaticItem(19, new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.categories.rewards.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.categories.rewards.lore", false))
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGJmOGI2Mjc3Y2QzNjI2NjI4M2NiNWE5ZTY5NDM5NTNjNzgzZTZmZjdkNmEyZDU5ZDE1YWQwNjk3ZTkxZDQzYyJ9fX0=")
                .build());

        addStaticItem(20, super.EXTRA_GLASS);

        // Completion Rewards
        addButton(21, new ItemBuilder(Material.CHEST)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.rewards.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.rewards.lore").toArray(new String[0]))
                .build(), (e) -> {
            new RewardsMenu(playerMenuUtility, plugin, new CollectionRewardHolder(plugin, collection))
                    .setTitleKey("gui.rewards.collection_title")
                    .setPreviousMenu(this)
                    .open();
        });

        // ACT Schedule Rules
        int ruleCount = collection.getActRules().size();
        addButton(22, new ItemBuilder(Material.REPEATING_COMMAND_BLOCK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.act_rules.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.act_rules.lore", false,
                        "%count%", String.valueOf(ruleCount)
                ).toArray(new String[0]))
                .build(), (e) -> {
            new ActRulesMenu(playerMenuUtility, plugin, collection).setPreviousMenu(this).open();
        });

        // Progress Reset Cron
        String cron = collection.getProgressResetCron() != null ?
                "Â§f" + collection.getProgressResetCron() :
                plugin.getMessageManager().getMessage("gui.common.none", false);

        addButton(23, new ItemBuilder(Material.CLOCK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.progress_reset_cron.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.progress_reset_cron.lore", false, "%cron%", cron).toArray(new String[0]))
                .build(), (e) -> {
            new ProgressResetCronMenu(playerMenuUtility, plugin, collection).setPreviousMenu(this).open();
        });

        // ==================== PRESETS ====================
        addStaticItem(28, new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.categories.presets.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.categories.presets.lore", false))
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGJmOGI2Mjc3Y2QzNjI2NjI4M2NiNWE5ZTY5NDM5NTNjNzgzZTZmZjdkNmEyZDU5ZDE1YWQwNjk3ZTkxZDQzYyJ9fX0=")
                .build());

        addStaticItem(29, super.EXTRA_GLASS);

        // Default Treasure Reward Preset
        String defaultPresetName = plugin.getMessageManager().getMessage("gui.common.none", false);
        UUID defaultPresetId = collection.getDefaultTreasureRewardPresetId();
        if (defaultPresetId != null) {
            defaultPresetName = plugin.getRewardPresetManager()
                    .getPreset(RewardPresetType.TREASURE, defaultPresetId)
                    .map(RewardPreset::getName)
                    .orElse(plugin.getMessageManager().getMessage("gui.presets.unknown", false));
        }

        addButton(30, new ItemBuilder(Material.WRITABLE_BOOK)
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
                plugin.getCollectionManager().saveCollection(collection).thenRun(() -> Bukkit.getScheduler().runTask(plugin, this::open));
            }, collection,true).setPreviousMenu(this).open();
        });

        // Override all treasures rewards
        addButton(31, new ItemBuilder(Material.ANVIL)
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

        // ==================== VISIBILITY OPTIONS ====================
        addStaticItem(37, new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.categories.visibility.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.categories.visibility.lore", false))
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGJmOGI2Mjc3Y2QzNjI2NjI4M2NiNWE5ZTY5NDM5NTNjNzgzZTZmZjdkNmEyZDU5ZDE1YWQwNjk3ZTkxZDQzYyJ9fX0=")
                .build());

        addStaticItem(38, super.EXTRA_GLASS);

        // Hide After Found
        String enabled = plugin.getMessageManager().getMessage("gui.settings.hide_status.enabled", false);
        String disabled = plugin.getMessageManager().getMessage("gui.settings.hide_status.disabled", false);
        addButton(39, new ItemBuilder(Material.GLASS_BOTTLE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.hide_after_found.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.hide_after_found.lore", false,
                        "%status%", disabled).toArray(new String[0]))
                .build(), (e) -> {
            playerMenuUtility.sendMessage("Â§4Â§lCOMING SOON");
        });

        // Hide Collection Disabled
        addButton(40, new ItemBuilder(Material.GLASS)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.hide_collection_disabled.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.hide_collection_disabled.lore", false,
                        "%status%", disabled).toArray(new String[0]))
                .build(), (e) -> {
            playerMenuUtility.sendMessage("Â§4Â§lCOMING SOON");
        });
    }

    private void runBulkOverride(RewardPreset preset) {
        List<TreasureCore> cores = plugin.getTreasureManager().getTreasureCoresInCollection(collection.getId());
        if (cores.isEmpty()) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.override.none"));
            open();
            return;
        }
        playerMenuUtility.closeInventory();

        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.override.started",
                "%count%", String.valueOf(cores.size()),
                "%name%", preset.getName()));

        final int total = cores.size();
        final AtomicInteger completed = new AtomicInteger(0);
        final AtomicInteger nextPercentToReport = new AtomicInteger(10);

        final int batchSize = 50;
        final List<de.theredend2000.advancedhunt.model.Reward> rewardSnapshot =
                preset.getRewards() == null ? java.util.Collections.emptyList() : java.util.List.copyOf(preset.getRewards());

        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (int i = 0; i < cores.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, cores.size());
            chain = chain.thenCompose(v -> {
                List<CompletableFuture<Void>> batch = new ArrayList<>(end - start);
                for (int j = start; j < end; j++) {
                    batch.add(plugin.getTreasureManager().updateTreasureRewards(cores.get(j).getId(), rewardSnapshot));
                }
                return CompletableFuture.allOf(batch.toArray(new CompletableFuture[0])).thenRun(() -> {
                    int done = completed.addAndGet(end - start);
                    if (total <= 0) return;
                    int percent = (int) Math.floor((done * 100.0) / total);

                    int next = nextPercentToReport.get();
                    if (percent < next) return;

                    while (percent >= next && next < 100) {
                        if (!nextPercentToReport.compareAndSet(next, next + 10)) {
                            next = nextPercentToReport.get();
                            continue;
                        }
                        int percentToSend = next;
                        Bukkit.getScheduler().runTask(plugin, () -> MessageUtils.sendActionBar(playerMenuUtility,
                                plugin.getMessageManager().getMessage("feedback.preset.override.progress",
                                        "%percent%", String.valueOf(percentToSend),
                                        "%current%", String.valueOf(done),
                                        "%total%", String.valueOf(total)))
                        );
                        next += 10;
                    }
                });
            });
        }

        chain.whenComplete((v, ex) -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (ex != null) {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.override.failed"));
                } else {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.override.done",
                            "%count%", String.valueOf(cores.size()),
                            "%name%", preset.getName()));
                }
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