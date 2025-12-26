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
import java.util.List;
import java.util.Optional;

/**
 * Paginated menu showing complete ACT rule presets.
 * Each preset applies all three components (date range, duration, cron) at once.
 */
public class ActPresetMenu extends PagedMenu {

    private final Collection collection;
    private final ActRule rule;
    
    // Static preset list - initialized once, shared across all menu instances
    private static final List<ActPreset> PRESETS = List.of(
            new ActPreset("always_available", "[*] [*] [NONE]", Material.NETHER_STAR),
            new ActPreset("daily_2h", "[*] [2h] [0 0 9 * * ?]", Material.CLOCK),
            new ActPreset("daily_4h", "[*] [4h] [0 0 14 * * ?]", Material.GOLDEN_CARROT),
            new ActPreset("weekend_8h", "[*] [8h] [0 0 10 ? * SAT,SUN]", Material.CAKE),
            new ActPreset("weekly_24h", "[*] [24h] [0 0 0 ? * MON]", Material.PAPER),
            new ActPreset("manual_6h", "[*] [6h] [MANUAL]", Material.LEVER),
            new ActPreset("holiday_special", "[" + getChristmasDateRange() + "] [*] [0 0 0 * * ?]", Material.PLAYER_HEAD),
            new ActPreset("seasonal_summer", "[" + getSummerDateRange() + "] [*] [NONE]", Material.SUNFLOWER),
            new ActPreset("hourly_30m", "[*] [30m] [0 0 * * * ?]", Material.IRON_NUGGET),
            new ActPreset("twice_daily_3h", "[*] [3h] [0 0 9,18 * * ?]", Material.GOLDEN_APPLE)
    );

    public ActPresetMenu(Player player, Main plugin, Collection collection, ActRule rule) {
        super(player, plugin);
        this.collection = collection;
        this.rule = rule;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.act.presets.title", false);
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        // Handled by buttons
    }

    @Override
    public void setMenuItems() {
        addMenuBorder();
        addPagedButtons(PRESETS.size());

        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, PRESETS.size());

        for (int i = startIndex; i < endIndex; i++) {
            ActPreset preset = PRESETS.get(i);
            int relativeIndex = i - startIndex;
            
            addPagedItem(relativeIndex, new ItemBuilder(preset.material)
                    .setDisplayName(preset.getName(plugin))
                    .setLore(preset.getDescription(plugin))
                    .build(), (e) -> applyPreset(preset));
        }
    }

    private void applyPreset(ActPreset preset) {
        Optional<ActFormatParser.ActSchedule> parsed = ActFormatParser.parse(preset.actFormat);
        
        if (parsed.isPresent()) {
            ActFormatParser.ActSchedule schedule = parsed.get();
            rule.setDateRange(schedule.getDateRange());
            rule.setDuration(schedule.getDuration());
            rule.setCronExpression(schedule.getCron());
            
            plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.act.presets.applied", 
                        "%preset%", preset.getName(plugin)));
                    
                    if (previousMenu != null) {
                        previousMenu.open();
                    } else {
                        new ActRuleEditorMenu(playerMenuUtility, plugin, collection, rule).open();
                    }
                });
            });
        } else {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.act_format.error.invalid_format"));
            if (previousMenu != null) {
                previousMenu.open();
            }
        }
    }

    // Helper methods to generate dynamic date ranges
    private static String getChristmasDateRange() {
        int year = LocalDate.now().getYear();
        return year + "-12-20:" + year + "-12-26";
    }

    private static String getSummerDateRange() {
        int year = LocalDate.now().getYear();
        return year + "-06-01:" + year + "-08-31";
    }

    private static final class ActPreset {
        final String key;
        final String actFormat;
        final Material material;

        ActPreset(String key, String actFormat, Material material) {
            this.key = key;
            this.actFormat = actFormat;
            this.material = material;
        }

        String getName(Main plugin) {
            return plugin.getMessageManager().getMessage("gui.act.presets.list." + key + ".name", false);
        }

        String[] getDescription(Main plugin) {
            return plugin.getMessageManager().getMessageList("gui.act.presets.list." + key + ".lore", false).toArray(new String[0]);
        }
    }
}
