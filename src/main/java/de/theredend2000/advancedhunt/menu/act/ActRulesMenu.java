package de.theredend2000.advancedhunt.menu.act;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.PagedMenu;
import de.theredend2000.advancedhunt.model.ActRule;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.util.ActFormatParser;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Menu for managing ACT (Availability-Cycle-Timing) rules for a collection
 */
public class ActRulesMenu extends PagedMenu {

    private final Collection collection;
    private boolean processing = false;

    public ActRulesMenu(Player player, Main plugin, Collection collection) {
        super(player, plugin);
        this.collection = collection;
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
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.act.rules.list.title", false, "%collection%", collection.getName());
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
    public void performClick(InventoryClickEvent event) {
        if (processing) return;
        super.performClick(event);
    }

    @Override
    public void setMenuItems() {
        List<ActRule> rules = collection.getActRules();
        this.hasNextPage = rules != null && (page + 1) * getMaxItemsPerPage() < rules.size();

        addMenuBorder();

        if (rules != null && !rules.isEmpty()) {
            addPagedButtons(rules.size());
            for (int i = 0; i < getMaxItemsPerPage(); i++) {
                index = getMaxItemsPerPage() * page + i;
                if (index >= rules.size()) break;
                
                ActRule rule = rules.get(index);
                if (rule != null) {

                    // Determine material based on rule status
                    Material material = rule.isEnabled() ? XMaterial.LIME_DYE.get() : XMaterial.GRAY_DYE.get();
                    
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
                        Optional<ZonedDateTime> nextOpt = scheduleOpt.get().getNextTrigger(ZonedDateTime.now());
                        if (nextOpt.isPresent()) {
                            nextTrigger = "§f" + plugin.getMessageManager().formatDateTime(nextOpt.get());
                        }
                    }

                    addPagedItem(i, new ItemBuilder(material)
                            .setDisplayName(plugin.getMessageManager().getMessage("gui.act.rules.list.rule_item.name", false, 
                                "%name%", rule.getName()))
                            .setLore(plugin.getMessageManager().getMessageList("gui.act.rules.list.rule_item.lore", false,
                                "%status%", status,
                                "%date_range%", dateRange,
                                "%duration%", duration,
                                "%cron%", cron,
                                "%next_trigger%", nextTrigger
                            ).toArray(new String[0]))
                            .build(), (e) -> {
                        if (e.getClick() == ClickType.SHIFT_LEFT) {
                            processing = true;
                            //Delete Rule
                            collection.removeActRule(rule.getId());
                            plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.act.rules.rule_deleted",
                                            "%name%", rule.getName()));
                                    refresh();
                                    processing = false;
                                });
                            }).exceptionally(ex -> {
                                processing = false;
                                return null;
                            });
                            return;
                        }
                        
                        if (e.isLeftClick()) {
                            // Edit rule
                            new ActRuleEditorMenu(playerMenuUtility, plugin, collection, rule).setPreviousMenu(this).open();
                        } else if (e.isRightClick()) {
                            processing = true;
                            // Toggle enabled
                            rule.setEnabled(!rule.isEnabled());
                            plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    refresh();
                                    processing = false;
                                });
                            }).exceptionally(ex -> {
                                processing = false;
                                return null;
                            });
                        }
                    });
                }
            }
        }

        // Add New Rule Button
        addButton(52, new ItemBuilder(XMaterial.EMERALD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.rules.list.add_rule.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.rules.list.add_rule.lore", false).toArray(new String[0]))
                .build(), (e) -> {
            processing = true;
            // Create new rule with default name
            ActRule newRule = new ActRule(plugin.getCollectionManager().generateUniqueActRuleId(), collection.getId(), "New Rule");
            collection.addActRule(newRule);
            plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    new ActSetupMenu(playerMenuUtility, plugin, collection, newRule)
                            .setPreviousMenu(this)
                            .open();
                    processing = false;
                });
            }).exceptionally(ex -> {
                processing = false;
                return null;
            });
        });
    }
}
