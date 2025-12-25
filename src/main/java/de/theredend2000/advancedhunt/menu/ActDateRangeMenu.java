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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Menu for editing the date range component of an ACT rule.
 * Provides presets and manual input options.
 */
public class ActDateRangeMenu extends Menu {

    private final Collection collection;
    private final ActRule rule;
    private final Menu previousMenu;

    public ActDateRangeMenu(Player player, Main plugin, Collection collection, ActRule rule, Menu previousMenu) {
        super(player, plugin);
        this.collection = collection;
        this.rule = rule;
        this.previousMenu = previousMenu;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.act.date_range.editor.title", false);
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
        // Current date range display at top
        String currentValue = rule.getDateRange();
        String description = ActFormatParser.getHumanReadableDateRange(currentValue);
        boolean isValid = ActFormatParser.isValidDateRange(currentValue);

        addButton(13, new ItemBuilder(Material.PAPER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.date_range.editor.current.name", false))
                .setLore(
                    plugin.getMessageManager().getMessage("gui.act.date_range.editor.current.value", false, "%date_range%", currentValue),
                    plugin.getMessageManager().getMessage("gui.act.date_range.editor.current.description", false, "%description%", description),
                    "",
                    isValid ? "§a✓ Valid" : "§c✗ Invalid"
                )
                .build(), (e) -> {});

        // Preset: Always Active (*)
        addButton(19, new ItemBuilder(Material.LIME_DYE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.date_range.preset.always.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.date_range.preset.always.lore", false).toArray(new String[0]))
                .build(), (e) -> applyDateRange("*"));

        // Preset: This Year
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        String thisYearRange = currentYear + "-01-01:" + currentYear + "-12-31";
        addButton(20, new ItemBuilder(Material.YELLOW_DYE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.date_range.preset.this_year.name", false, "%year%", String.valueOf(currentYear)))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.date_range.preset.this_year.lore", false, 
                    "%year%", String.valueOf(currentYear)).toArray(new String[0]))
                .build(), (e) -> applyDateRange(thisYearRange));

        // Preset: This Month
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        String thisMonthRange = monthStart.format(formatter) + ":" + monthEnd.format(formatter);
        addButton(21, new ItemBuilder(Material.ORANGE_DYE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.date_range.preset.this_month.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.date_range.preset.this_month.lore", false,
                    "%start%", monthStart.format(formatter),
                    "%end%", monthEnd.format(formatter)).toArray(new String[0]))
                .build(), (e) -> applyDateRange(thisMonthRange));

        // Preset: Next Month
        LocalDate nextMonthStart = now.plusMonths(1).withDayOfMonth(1);
        LocalDate nextMonthEnd = nextMonthStart.withDayOfMonth(nextMonthStart.lengthOfMonth());
        String nextMonthRange = nextMonthStart.format(formatter) + ":" + nextMonthEnd.format(formatter);
        addButton(22, new ItemBuilder(Material.PINK_DYE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.date_range.preset.next_month.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.date_range.preset.next_month.lore", false,
                    "%start%", nextMonthStart.format(formatter),
                    "%end%", nextMonthEnd.format(formatter)).toArray(new String[0]))
                .build(), (e) -> applyDateRange(nextMonthRange));

        // Manual Input
        addButton(31, new ItemBuilder(Material.ANVIL)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.date_range.manual.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.date_range.manual.lore", false).toArray(new String[0]))
                .build(), (e) -> {
            Player player = (Player) e.getWhoClicked();
            player.closeInventory();
            
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.act.date_range.prompt"));
            
            plugin.getChatInputListener().requestInput(playerMenuUtility, (input) -> {
                if (ActFormatParser.isValidDateRange(input)) {
                    applyDateRange(input);
                } else {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.common.error.validation.date_range"));
                    Bukkit.getScheduler().runTask(plugin, this::open);
                }
            });
        });

        // Back Button
        addButton(49, new ItemBuilder(Material.BARRIER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.common.back_to", false, "%menu%", "Rule Editor"))
                .build(), (e) -> {
            if (previousMenu != null) {
                previousMenu.open();
            }
        });
    }

    private void applyDateRange(String dateRange) {
        rule.setDateRange(dateRange);
        plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.act.date_range.success"));
                if (previousMenu != null) {
                    previousMenu.open();
                } else {
                    new ActRuleEditorMenu(playerMenuUtility, plugin, collection, rule).open();
                }
            });
        });
    }
}
