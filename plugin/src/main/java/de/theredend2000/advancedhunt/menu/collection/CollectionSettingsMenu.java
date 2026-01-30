package de.theredend2000.advancedhunt.menu.collection;

import com.cryptomorin.xseries.XMaterial;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CollectionSettingsMenu extends Menu {

    private Collection collection;
    private boolean processing = false;

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
    public void open() {
        processing = false;
        super.open();
    }

    @Override
    public void refresh() {
        processing = false;
        super.refresh();
    }

    @Override
    public void setMenuItems() {
        fillBorders(super.FILLER_GLASS);

        // ==================== BASIC SETTINGS ====================
        addStaticItem(10, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.categories.basic.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.categories.basic.lore", false))
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGJmOGI2Mjc3Y2QzNjI2NjI4M2NiNWE5ZTY5NDM5NTNjNzgzZTZmZjdkNmEyZDU5ZDE1YWQwNjk3ZTkxZDQzYyJ9fX0=")
                .build());

        addStaticItem(11, super.EXTRA_GLASS);

        // Rename
        addButton(12, new ItemBuilder(XMaterial.NAME_TAG)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.rename.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.rename.lore", "%name%", collection.getName()).toArray(new String[0]))
                .build(), (e) -> {
            plugin.getChatInputListener().requestInput(playerMenuUtility, (input) -> {
                plugin.getCollectionManager().renameCollection(collection.getName(), input).thenAccept(success -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (success) {
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("command.rename.success", "%old_name%", collection.getName(), "%new_name%", input));
                            collection.setName(input);
                        } else {
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("command.rename.failed"));
                        }
                        this.open();
                    });
                });
            });
        }, "advancedhunt.admin.collection.rename");

        // Toggle Enabled
        String status = collection.isEnabled() ?
                plugin.getMessageManager().getMessage("gui.common.enabled") :
                plugin.getMessageManager().getMessage("gui.common.disabled");

        addButton(13, new ItemBuilder(collection.isEnabled() ? XMaterial.LIME_DYE.get() : XMaterial.GRAY_DYE.get())
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.status.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.status.lore", "%status%", status).toArray(new String[0]))
                .build(), (e) -> {
            if (processing) return;
            processing = true;
            
            collection.setEnabled(!collection.isEnabled());
            plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Refresh visibility with the already-updated collection
                    if (collection.isHideWhenNotAvailable()) {
                        plugin.getTreasureVisibilityManager().refreshCollectionVisibility(collection);
                    }
                    processing = false;
                    this.refresh();
                });
            });
        });

        // Toggle Single Player Find
        String spfStatus = collection.isSinglePlayerFind() ?
                plugin.getMessageManager().getMessage("gui.common.enabled") :
                plugin.getMessageManager().getMessage("gui.common.disabled");

        addButton(14, new ItemBuilder(collection.isSinglePlayerFind() ? XMaterial.ENDER_PEARL.get() : XMaterial.ENDER_EYE.get())
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.single_player_find.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.single_player_find.lore", "%status%", spfStatus).toArray(new String[0]))
                .build(), (e) -> {
            if (processing) return;
            processing = true;
            
            collection.setSinglePlayerFind(!collection.isSinglePlayerFind());
            plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    processing = false;
                    this.refresh();
                });
            });
        });

        // Delete Collection
        addButton(15, new ItemBuilder(XMaterial.TNT)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.delete.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.delete.lore").toArray(new String[0]))
                .build(), (e) -> {
            new DeleteCollectionHandlingMenu(playerMenuUtility, plugin, collection)
                    .setPreviousMenu(this)
                    .open();
        }, "advancedhunt.admin.collection.delete");

        addButton(17, new ItemBuilder(XMaterial.CAULDRON)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.deleting_type.delete.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.deleting_type.delete.lore").toArray(new String[0]))
                .build(), (e) -> {
            playerMenuUtility.sendMessage("Yes");
            //TODO ADD FUNCTION
        });

        // ==================== ACT CONFIGURATION ====================
        addStaticItem(19, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.categories.rewards.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.categories.rewards.lore", false))
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGJmOGI2Mjc3Y2QzNjI2NjI4M2NiNWE5ZTY5NDM5NTNjNzgzZTZmZjdkNmEyZDU5ZDE1YWQwNjk3ZTkxZDQzYyJ9fX0=")
                .build());

        addStaticItem(20, super.EXTRA_GLASS);

        // Completion Rewards
        addButton(21, new ItemBuilder(XMaterial.CHEST)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.rewards.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.rewards.lore").toArray(new String[0]))
                .build(), (e) -> {
            new RewardsMenu(playerMenuUtility, plugin, new CollectionRewardHolder(plugin, collection))
                    .setPreviousMenu(this)
                    .open();
        });

        // ACT Schedule Rules
        int ruleCount = collection.getActRules().size();
        addButton(22, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzY4N2EyYmNhZjgzZWE4YTMxNzRkMDNkMzkwMDk5MWI2N2U2NWM4Y2ExY2M0ZDk1YTBiMmNiNzE3OTY3YTYyNyJ9fX0=")
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

        addButton(23, new ItemBuilder(XMaterial.CLOCK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.progress_reset_cron.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.progress_reset_cron.lore", false, "%cron%", cron).toArray(new String[0]))
                .build(), (e) -> {
            new ProgressResetCronMenu(playerMenuUtility, plugin, collection).setPreviousMenu(this).open();
        });

        // ==================== PRESETS ====================
        addStaticItem(28, new ItemBuilder(XMaterial.PLAYER_HEAD)
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

        addButton(30, new ItemBuilder(XMaterial.WRITABLE_BOOK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.default_treasure_preset.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.default_treasure_preset.lore", false,
                        "%preset%", "§f" + defaultPresetName).toArray(new String[0]))
                .build(), (e) -> {
            if (!playerMenuUtility.hasPermission("advancedhunt.admin")) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                return;
            }

            if (e.isRightClick()) {
                if (processing) return;
                processing = true;
                
                collection.setDefaultTreasureRewardPresetId(null);
                plugin.getCollectionManager().saveCollection(collection).thenRun(() -> 
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        processing = false;
                        this.refresh();
                    }));
                return;
            }

            new RewardPresetListMenu(playerMenuUtility, plugin, RewardPresetType.TREASURE, selected -> {
                if (processing) return;
                processing = true;
                
                collection.setDefaultTreasureRewardPresetId(selected.getId());
                plugin.getCollectionManager().saveCollection(collection).thenRun(() -> 
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        processing = false;
                        this.open();
                    }));
            }, collection,true).setPreviousMenu(this).open();
        });

        // Override all treasures rewards
        addButton(31, new ItemBuilder(XMaterial.ANVIL)
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
        String enabled = plugin.getMessageManager().getMessage("gui.common.enabled", false);
        String disabled = plugin.getMessageManager().getMessage("gui.common.disabled", false);
        addButton(39, new ItemBuilder(Material.GLASS_BOTTLE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.hide_after_found.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.hide_after_found.lore", false,
                        "%status%", disabled).toArray(new String[0]))
                .build(), (e) -> {
            playerMenuUtility.sendMessage("§c§lThis feature is currently in development.");
        });

        // Hide When Not Available
        String hideStatus = collection.isHideWhenNotAvailable() ? enabled : disabled;
        addButton(40, new ItemBuilder(collection.isHideWhenNotAvailable() ? Material.GLASS : Material.GLASS_BOTTLE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.settings.hide_when_not_available.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.settings.hide_when_not_available.lore", false,
                        "%status%", hideStatus).toArray(new String[0]))
                .build(), (e) -> {
            if (processing) return;
            processing = true;
            
            collection.setHideWhenNotAvailable(!collection.isHideWhenNotAvailable());
            plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Refresh visibility with the already-updated collection
                    plugin.getTreasureVisibilityManager().refreshCollectionVisibility(collection);
                    processing = false;
                    this.refresh();
                });
            });
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

        final List<Reward> rewardSnapshot =
            preset.getRewards() == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(preset.getRewards()));

        plugin.getTreasureManager().overrideTreasureRewardsInCollection(
                collection.getId(),
                rewardSnapshot,
                50,
            (percentToSend, done) -> Bukkit.getScheduler().runTask(plugin, () -> MessageUtils.sendActionBar(playerMenuUtility,
                        plugin.getMessageManager().getMessage("feedback.preset.override.progress",
                                "%percent%", String.valueOf(percentToSend),
                    "%current%", String.valueOf(done),
                                "%total%", String.valueOf(cores.size())))
                )
        ).whenComplete((count, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (ex != null) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.override.failed"));
            } else {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.override.done",
                        "%count%", String.valueOf(count),
                        "%name%", preset.getName()));
            }
        }));
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