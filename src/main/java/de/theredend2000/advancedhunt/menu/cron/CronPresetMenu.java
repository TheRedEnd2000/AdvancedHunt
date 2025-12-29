package de.theredend2000.advancedhunt.menu.cron;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.PagedMenu;
import de.theredend2000.advancedhunt.model.ActRule;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class CronPresetMenu extends PagedMenu {

    private final CronExpressionHolder holder;
    private final CronEditPolicy policy;
    
    // Static preset list - initialized once, shared across all menu instances
    private static final List<CronPreset> PRESETS = List.of(
            new CronPreset("hourly", "0 0 * * * ? *", Material.CLOCK),
            new CronPreset("every_6_hours", "0 0 */6 * * ? *", Material.CLOCK),
            new CronPreset("every_12_hours", "0 0 */12 * * ? *", Material.CLOCK),
            new CronPreset("daily_midnight", "0 0 0 * * ? *", Material.SUNFLOWER),
            new CronPreset("daily_noon", "0 0 12 * * ? *", Material.GOLDEN_CARROT),
            new CronPreset("daily_6am", "0 0 6 * * ? *", Material.EMERALD),
            new CronPreset("weekdays_9am", "0 0 9 ? * MON-FRI *", Material.DIAMOND),
            new CronPreset("monday_midnight", "0 0 0 ? * MON *", Material.PAPER),
            new CronPreset("friday_6pm", "0 0 18 ? * FRI *", Material.CAKE),
            new CronPreset("sunday_midnight", "0 0 0 ? * SUN *", Material.BELL),
            new CronPreset("first_day_month", "0 0 0 1 * ? *", Material.WRITABLE_BOOK),
            new CronPreset("last_day_month", "0 0 0 L * ? *", Material.BOOK),
            new CronPreset("first_monday_month", "0 0 0 ? * MON#1 *", Material.ENCHANTED_BOOK),
            new CronPreset("new_year", "0 0 0 1 1 ? *", Material.EXPERIENCE_BOTTLE),
            new CronPreset("christmas", "0 0 0 25 12 ? *", Material.PLAYER_HEAD),
            new CronPreset("quarterly", "0 0 0 1 1,4,7,10 ? *", Material.COMPASS)
    );

    // Constructor for Collection context
    public CronPresetMenu(Player playerMenuUtility, Main plugin, Collection collection) {
        super(playerMenuUtility, plugin);
        this.holder = new CollectionProgressResetCronHolder(plugin, collection);
        this.policy = CronEditPolicy.progressReset();
    }

    // Constructor for ActRule context
    public CronPresetMenu(Player playerMenuUtility, Main plugin, Collection collection, ActRule actRule) {
        super(playerMenuUtility, plugin);
        this.holder = new ActRuleCronHolder(plugin, collection, actRule);
        this.policy = CronEditPolicy.actSchedule();
    }

    public CronPresetMenu(Player playerMenuUtility, Main plugin, CronExpressionHolder holder, CronEditPolicy policy) {
        super(playerMenuUtility, plugin);
        this.holder = holder;
        this.policy = policy;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.cron.presets.title");
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
            CronPreset preset = PRESETS.get(i);
            int relativeIndex = i - startIndex;
            
            addPagedItem(relativeIndex, new ItemBuilder(preset.material)
                    .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.presets.item.name", "%name%", preset.getName(plugin)))
                    .setLore(preset.getDescription(plugin))
                    .build(), (e) -> {
                    holder.setExpression(preset.expression);
                    holder.save().thenRun(() -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.cron.preset_applied", "%preset%", preset.getName(plugin)));
                            if (previousMenu != null) {
                                previousMenu.open();
                            } else {
                                new CronEditorMenu(playerMenuUtility, plugin, holder, policy).open();
                            }
                        });
                    });
                });
        }
    }

    private static final class CronPreset {
        final String key;
        final String expression;
        final Material material;

        CronPreset(String key, String expression, Material material) {
            this.key = key;
            this.expression = expression;
            this.material = material;
        }

        String getName(Main plugin) {
            return plugin.getMessageManager().getMessage("gui.cron.presets.list." + key + ".name");
        }

        String[] getDescription(Main plugin) {
            return plugin.getMessageManager().getMessageList("gui.cron.presets.list." + key + ".lore").toArray(new String[0]);
        }
    }
}
