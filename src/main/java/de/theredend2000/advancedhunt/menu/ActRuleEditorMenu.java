package de.theredend2000.advancedHunt.menu;

import de.theredend2000.advancedHunt.Main;
import de.theredend2000.advancedHunt.model.ActRule;
import de.theredend2000.advancedHunt.model.Collection;
import de.theredend2000.advancedHunt.util.ActFormatParser;
import de.theredend2000.advancedHunt.util.CronUtils;
import de.theredend2000.advancedHunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Hub menu for ACT rule editing.
 * Displays current configuration with live preview and provides access to component editors.
 */
public class ActRuleEditorMenu extends Menu {

    private final Collection collection;
    private final ActRule rule;

    public ActRuleEditorMenu(Player player, Main plugin, Collection collection, ActRule rule) {
        super(player, plugin);
        this.collection = collection;
        this.rule = rule;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.act.editor.title", false, "%name%", rule.getName());
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(org.bukkit.event.inventory.InventoryClickEvent e) {
        // All click handling is done via buttons
    }

    @Override
    public void setMenuItems() {
        // Live Preview Section (Top)
        String dateRangeReadable = ActFormatParser.getHumanReadableDateRange(rule.getDateRange());
        String durationReadable = ActFormatParser.getHumanReadableDuration(rule.getDuration());
        String cronReadable = ActFormatParser.getHumanReadableCron(rule.getCronExpression());
        
        boolean isValid = ActFormatParser.isValidDateRange(rule.getDateRange()) &&
                         ActFormatParser.isValidDuration(rule.getDuration()) &&
                         ActFormatParser.isValidCron(rule.getCronExpression());
        
        List<String> previewLore = new ArrayList<>();
        previewLore.add(plugin.getMessageManager().getMessage("gui.act.editor.current.date_range", false, 
            "%date_range%", rule.getDateRange()));
        previewLore.add(plugin.getMessageManager().getMessage("gui.act.editor.current.duration", false, 
            "%duration%", rule.getDuration()));
        previewLore.add(plugin.getMessageManager().getMessage("gui.act.editor.current.cron", false, 
            "%cron%", rule.getCronExpression()));
        previewLore.add("");
        
        // Show next activation times if cron is valid
        if (!rule.getCronExpression().equalsIgnoreCase("MANUAL") && 
            !rule.getCronExpression().equalsIgnoreCase("NONE") &&
            ActFormatParser.isValidCron(rule.getCronExpression())) {
            try {
                List<String> nextRuns = CronUtils.getNextExecutions(rule.getCronExpression(), 3);
                if (!nextRuns.isEmpty()) {
                    previewLore.add(plugin.getMessageManager().getMessage("gui.act.editor.current.next_activation", false));
                    for (int i = 0; i < nextRuns.size(); i++) {
                        previewLore.add("§7" + (i + 1) + ". §f" + nextRuns.get(i));
                    }
                    previewLore.add("");
                }
            } catch (Exception e) {
                // Invalid cron, skip preview
            }
        }
        
        previewLore.add(plugin.getMessageManager().getMessage("gui.act.editor.current.validation", false, 
            "%status%", isValid ? plugin.getMessageManager().getMessage("gui.act.editor.current.valid", false) : 
                               plugin.getMessageManager().getMessage("gui.act.editor.current.invalid", false)));
        
        addButton(4, new ItemBuilder(Material.PAPER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.editor.current.name", false))
                .setLore(previewLore.toArray(new String[0]))
                .build(), (e) -> {});

        // Name Editor
        addButton(10, new ItemBuilder(Material.NAME_TAG)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act_rule_editor.name.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act_rule_editor.name.lore", false,
                    "%name%", rule.getName()
                ).toArray(new String[0]))
                .build(), (e) -> {
            plugin.getChatInputListener().requestInput(playerMenuUtility, (input) -> {
                rule.setName(input);
                plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        new ActRuleEditorMenu(playerMenuUtility, plugin, collection, rule).open();
                    });
                });
            });
        });

        // Component Editors (Middle Row)

        // Date Range Editor
        addButton(20, new ItemBuilder(Material.MAP)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.component.date_range.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.component.date_range.lore", false,
                    "%value%", rule.getDateRange(),
                    "%description%", dateRangeReadable
                ).toArray(new String[0]))
                .build(), (e) -> {
            new ActDateRangeMenu(playerMenuUtility, plugin, collection, rule, this).open();
        });

        // Duration Editor
        addButton(22, new ItemBuilder(Material.CLOCK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.component.duration.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.component.duration.lore", false,
                    "%value%", rule.getDuration(),
                    "%description%", durationReadable
                ).toArray(new String[0]))
                .build(), (e) -> {
            new ActDurationMenu(playerMenuUtility, plugin, collection, rule, this).open();
        });

        // Cron Editor
        addButton(24, new ItemBuilder(Material.REPEATER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.component.cron.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.component.cron.lore", false,
                    "%value%", rule.getCronExpression(),
                    "%description%", cronReadable
                ).toArray(new String[0]))
                .build(), (e) -> {
            new de.theredend2000.advancedHunt.menu.cron.CronEditorMenu(playerMenuUtility, plugin, collection, rule).open();
        });

        // Apply Preset Button
        addButton(31, new ItemBuilder(Material.CHEST)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.preset.apply.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.preset.apply.lore", false).toArray(new String[0]))
                .build(), (e) -> {
            ActPresetMenu presetMenu = new ActPresetMenu(playerMenuUtility, plugin, collection, rule);
            presetMenu.setPreviousMenu(this);
            presetMenu.open();
        });

        // Manual ACT Input Button
        addButton(32, new ItemBuilder(Material.WRITABLE_BOOK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.manual.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.manual.lore", false).toArray(new String[0]))
                .build(), (e) -> {
            Player player = (Player) e.getWhoClicked();
            player.closeInventory();
            
            for (String line : plugin.getMessageManager().getMessageList("gui.act.manual.prompt", false)) {
                playerMenuUtility.sendMessage(line);
            }
            
            plugin.getChatInputListener().requestInput(playerMenuUtility, (input) -> {
                java.util.Optional<ActFormatParser.ActSchedule> parsed = ActFormatParser.parse(input);
                if (parsed.isPresent()) {
                    ActFormatParser.ActSchedule schedule = parsed.get();
                    rule.setDateRange(schedule.getDateRange());
                    rule.setDuration(schedule.getDuration());
                    rule.setCronExpression(schedule.getCron());
                    
                    plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.act.success.applied"));
                            new ActRuleEditorMenu(playerMenuUtility, plugin, collection, rule).open();
                        });
                    });
                } else {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.act_format.error.invalid_format"));
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        new ActRuleEditorMenu(playerMenuUtility, plugin, collection, rule).open();
                    });
                }
            });
        });

        // Settings Row (Bottom)
        
        // Toggle Enabled
        String enabledStatus = rule.isEnabled() ? 
            plugin.getMessageManager().getMessage("gui.common.enabled", false) : 
            plugin.getMessageManager().getMessage("gui.common.disabled", false);
        Material statusMaterial = rule.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE;
        
        addButton(38, new ItemBuilder(statusMaterial)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act_rule_editor.enabled.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act_rule_editor.enabled.lore", false,
                    "%status%", enabledStatus
                ).toArray(new String[0]))
                .build(), (e) -> {
            rule.setEnabled(!rule.isEnabled());
            plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    new ActRuleEditorMenu(playerMenuUtility, plugin, collection, rule).open();
                });
            });
        });

        // Priority
        addButton(40, new ItemBuilder(Material.IRON_BARS)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act_rule_editor.priority.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act_rule_editor.priority.lore", false,
                    "%priority%", String.valueOf(rule.getPriority())
                ).toArray(new String[0]))
                .build(), (e) -> {
            plugin.getChatInputListener().requestInput(playerMenuUtility, (input) -> {
                try {
                    int priority = Integer.parseInt(input);
                    rule.setPriority(priority);
                    plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            new ActRuleEditorMenu(playerMenuUtility, plugin, collection, rule).open();
                        });
                    });
                } catch (NumberFormatException ex) {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.error.invalid_number"));
                    new ActRuleEditorMenu(playerMenuUtility, plugin, collection, rule).open();
                }
            });
        });

        // Delete Rule
        addButton(42, new ItemBuilder(Material.RED_DYE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act_rule_editor.delete.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act_rule_editor.delete.lore", false).toArray(new String[0]))
                .build(), (e) -> {
            collection.removeActRule(rule.getId());
            plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.act_rules.rule_deleted",
                        "%name%", rule.getName()));
                    new ActRulesMenu(playerMenuUtility, plugin, collection).open();
                });
            });
        });

        // Back Button
        addButton(49, new ItemBuilder(Material.BARRIER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.common.back_arrow", false))
                .build(), (e) -> {
            new ActRulesMenu(playerMenuUtility, plugin, collection).open();
        });
    }
}
