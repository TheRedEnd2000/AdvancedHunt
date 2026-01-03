package de.theredend2000.advancedhunt.menu.act;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.menu.cron.CronEditorMenu;
import de.theredend2000.advancedhunt.model.ActRule;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Simple, operator-friendly ACT setup menu.
 *
 * Active always between dates X and Y (date range + NONE + permanent)
 * Active for X hours every Y (duration + cron schedule)
 * Advanced (full editor)
 */
public class ActSetupMenu extends Menu {

    private final Collection collection;
    private final ActRule rule;

    public ActSetupMenu(Player player, Main plugin, Collection collection, ActRule rule) {
        super(player, plugin);
        this.collection = collection;
        this.rule = rule;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.act.setup.title", false);
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        // All click handling is done via buttons
    }

    @Override
    public void setMenuItems() {
        fillBorders(super.FILLER_GLASS);

        ActRulesMenu rulesMenu = previousMenu instanceof ActRulesMenu ? (ActRulesMenu) previousMenu : null;

        ActRuleEditorMenu editorMenu;
        if (previousMenu instanceof ActRuleEditorMenu) {
            // If we came from the editor already, return to that exact instance.
            editorMenu = (ActRuleEditorMenu) previousMenu;
        } else {
            // Otherwise create an editor that returns to whatever opened this setup menu.
            editorMenu = new ActRuleEditorMenu(playerMenuUtility, plugin, collection, rule);
            if (rulesMenu != null) {
                editorMenu.setPreviousMenu(rulesMenu);
            } else if (previousMenu != null) {
                editorMenu.setPreviousMenu(previousMenu);
            }
        }

        // Date window (always on)
        addButton(11, new ItemBuilder(Material.MAP)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.act.setup.date_window.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.act.setup.date_window.lore", false).toArray(new String[0]))
                .build(), (e) -> {
            // Ensure this behaves like "always available during date window"
            rule.setDuration("*");
            rule.setCronExpression("NONE");

            plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    new ActDateRangeMenu(playerMenuUtility, plugin, collection, rule, editorMenu)
                            .setPreviousMenu(this)
                            .open();
                });
            });
        });

        // Repeating schedule (duration + fixed time cron)
        addButton(13, new ItemBuilder(Material.CLOCK)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.act.setup.repeating_schedule.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.act.setup.repeating_schedule.lore", false).toArray(new String[0]))
                .build(), (e) -> {
            // Defaults; user will pick duration, then pick schedule.
            rule.setDateRange("*");

            CronEditorMenu cronMenu = new CronEditorMenu(playerMenuUtility, plugin, collection, rule);
            cronMenu.setPreviousMenu(editorMenu);

            ActDurationMenu durationMenu = new ActDurationMenu(playerMenuUtility, plugin, collection, rule, this, cronMenu);
            durationMenu.open();
        });

        // Advanced
        addButton(15, new ItemBuilder(Material.COMPARATOR)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.act.setup.advanced.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.act.setup.advanced.lore", false).toArray(new String[0]))
                .build(), (e) -> editorMenu.open());
    }
}
