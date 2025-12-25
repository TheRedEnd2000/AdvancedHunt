package de.theredend2000.advancedhunt.menu;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.ActRule;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.util.ActFormatParser;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Menu for managing ACT (Availability-Cycle-Timing) rules for a collection
 */
public class ActRulesMenu extends PagedMenu {

    private final Collection collection;

    public ActRulesMenu(Player player, Main plugin, Collection collection) {
        super(player, plugin);
        this.collection = collection;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.act_rules.title", false, "%collection%", collection.getName());
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        // All click handling is done via buttons
    }

    @Override
    public void setMenuItems() {
        List<ActRule> rules = collection.getActRules();
        this.hasNextPage = rules != null && (page + 1) * getMaxItemsPerPage() < rules.size();
        
        addMenuBorder();

        if (rules != null && !rules.isEmpty()) {
            for (int i = 0; i < getMaxItemsPerPage(); i++) {
                index = getMaxItemsPerPage() * page + i;
                if (index >= rules.size()) break;
                
                ActRule rule = rules.get(index);
                if (rule != null) {
                    int finalI = i;
                    
                    // Determine material based on rule status
                    Material material = rule.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE;
                    
                    // Parse ACT format for display
                    String actFormat = rule.getActFormat();
                    Optional<ActFormatParser.ActSchedule> scheduleOpt = ActFormatParser.parse(actFormat);
                    
                    String dateRange = rule.getDateRange();
                    String duration = rule.getDuration();
                    String cron = rule.getCronExpression();
                    
                    // Check if rule is currently active
                    String status = rule.isEnabled() ? "§aEnabled" : "§cDisabled";
                    
                    // Show next trigger if available
                    String nextTrigger = "§7N/A";
                    if (scheduleOpt.isPresent() && rule.isEnabled()) {
                        scheduleOpt.get().getNextTrigger(ZonedDateTime.now()).ifPresent(next -> {
                            // This will be set in the actual implementation
                        });
                    }

                    addPagedItem(finalI, new ItemBuilder(material)
                            .setDisplayName(plugin.getMessageManager().getMessage("gui.act_rules.rule_item.name", false, 
                                "%name%", rule.getName()))
                            .setLore(plugin.getMessageManager().getMessageList("gui.act_rules.rule_item.lore", false,
                                "%status%", status,
                                "%date_range%", dateRange,
                                "%duration%", duration,
                                "%cron%", cron
                            ).toArray(new String[0]))
                            .build(), (e) -> {
                        if (e.isLeftClick()) {
                            //Delete Rule
                            if(e.isShiftClick()){
                                collection.removeActRule(rule.getId());
                                plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                                        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.act_rules.rule_deleted",
                                                "%name%", rule.getName()));
                                        refresh();
                                    });
                                });
                                return;
                            }
                            // Edit rule
                            new ActRuleEditorMenu(playerMenuUtility, plugin, collection, rule).open();
                        } else if (e.isRightClick()) {
                            // Toggle enabled
                            rule.setEnabled(!rule.isEnabled());
                            plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    refresh();
                                });
                            });
                        }
                    });
                }
            }
        }

        // Add New Rule Button
        addButton(52, new ItemBuilder(Material.EMERALD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act_rules.add_rule.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act_rules.add_rule.lore", false).toArray(new String[0]))
                .build(), (e) -> {
            // Create new rule with default name
            ActRule newRule = new ActRule(collection.getId(), "New Rule");
            collection.addActRule(newRule);
            plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    // Open GUI editor immediately
                    new ActRuleEditorMenu(playerMenuUtility, plugin, collection, newRule).open();
                });
            });
        });

        // Back to Settings Button
        addButton(49, new ItemBuilder(Material.BARRIER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.common.back", false))
                .build(), (e) -> {
            new CollectionSettingsMenu(playerMenuUtility, plugin, collection).open();
        });
    }
}
