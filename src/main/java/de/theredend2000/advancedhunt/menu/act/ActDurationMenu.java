package de.theredend2000.advancedhunt.menu.act;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.model.ActRule;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.util.ActFormatParser;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Menu for editing the duration component of an ACT rule.
 * Provides presets, quick adjustment, and manual input options.
 */
public class ActDurationMenu extends Menu {

    private final Collection collection;
    private final ActRule rule;
    private final Menu afterApplyMenu;
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhd])");

    public ActDurationMenu(Player player, Main plugin, Collection collection, ActRule rule, Menu previousMenu) {
        super(player, plugin);
        this.collection = collection;
        this.rule = rule;
        this.afterApplyMenu = null;
        setPreviousMenu(previousMenu);
    }

    /**
     * When afterApplyMenu is provided, applying a duration will immediately open that menu.
     * Used for operator-friendly flows (e.g., pick duration, then pick schedule).
     */
    public ActDurationMenu(Player player, Main plugin, Collection collection, ActRule rule, Menu previousMenu, Menu afterApplyMenu) {
        super(player, plugin);
        this.collection = collection;
        this.rule = rule;
        this.afterApplyMenu = afterApplyMenu;
        setPreviousMenu(previousMenu);
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.act.components.duration.editor.title", false);
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
        fillBorders(super.FILLER_GLASS);
        // Current duration display at top
        String currentValue = rule.getDuration();
        String description = ActFormatParser.getHumanReadableDuration(currentValue);
        boolean isValid = ActFormatParser.isValidDuration(currentValue);

        addButton(13, new ItemBuilder(Material.CLOCK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.components.duration.editor.current.name", false))
                .setLore(
                    plugin.getMessageManager().getMessage("gui.act.components.duration.editor.current.value", false, "%duration%", currentValue),
                    plugin.getMessageManager().getMessage("gui.act.components.duration.editor.current.description", false, "%description%", description),
                    "",
                    isValid ? "§a✓ Valid" : "§c✗ Invalid"
                )
                .build(), (e) -> {});

        // Preset: Permanent (*)
        addButton(19, new ItemBuilder(Material.NETHER_STAR)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.components.duration.preset.permanent.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.components.duration.preset.permanent.lore", false).toArray(new String[0]))
                .build(), (e) -> applyDuration("*"));

        // Preset: 30 Minutes
        addButton(20, new ItemBuilder(Material.IRON_NUGGET)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.components.duration.preset.30_minutes.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.components.duration.preset.30_minutes.lore", false).toArray(new String[0]))
                .build(), (e) -> applyDuration("30m"));

        // Preset: 1 Hour
        addButton(21, new ItemBuilder(Material.GOLD_NUGGET)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.components.duration.preset.1_hour.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.components.duration.preset.1_hour.lore", false).toArray(new String[0]))
                .build(), (e) -> applyDuration("1h"));

        // Preset: 2 Hours
        addButton(22, new ItemBuilder(Material.GOLD_INGOT)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.components.duration.preset.2_hours.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.components.duration.preset.2_hours.lore", false).toArray(new String[0]))
                .build(), (e) -> applyDuration("2h"));

        // Preset: 4 Hours
        addButton(23, new ItemBuilder(Material.GOLDEN_CARROT)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.components.duration.preset.4_hours.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.components.duration.preset.4_hours.lore", false).toArray(new String[0]))
                .build(), (e) -> applyDuration("4h"));

        // Preset: 8 Hours
        addButton(24, new ItemBuilder(Material.GOLDEN_APPLE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.components.duration.preset.8_hours.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.components.duration.preset.8_hours.lore", false).toArray(new String[0]))
                .build(), (e) -> applyDuration("8h"));

        // Preset: 24 Hours
        addButton(25, new ItemBuilder(Material.ENCHANTED_GOLDEN_APPLE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.components.duration.preset.24_hours.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.components.duration.preset.24_hours.lore", false).toArray(new String[0]))
                .build(), (e) -> applyDuration("24h"));

        // Preset: 7 Days
        addButton(28, new ItemBuilder(Material.TOTEM_OF_UNDYING)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.components.duration.preset.7_days.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.components.duration.preset.7_days.lore", false).toArray(new String[0]))
                .build(), (e) -> applyDuration("7d"));

        // Decrease button (-1)
        addButton(37, new ItemBuilder(Material.RED_CONCRETE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.components.duration.decrease.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.components.duration.decrease.lore", false).toArray(new String[0]))
                .build(), (e) -> modifyNumericDuration(-1));

        // Increase button (+1)
        addButton(43, new ItemBuilder(Material.LIME_CONCRETE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.components.duration.increase.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.components.duration.increase.lore", false).toArray(new String[0]))
                .build(), (e) -> modifyNumericDuration(1));

        // Manual Input
        addButton(40, new ItemBuilder(Material.ANVIL)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.act.components.duration.custom.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.act.components.duration.custom.lore", false).toArray(new String[0]))
                .build(), (e) -> {
            Player player = (Player) e.getWhoClicked();
            player.closeInventory();
            
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.act.components.duration.prompt"));
            
            plugin.getChatInputListener().requestInput(playerMenuUtility, (input) -> {
                if (ActFormatParser.isValidDuration(input)) {
                    applyDuration(input);
                } else {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.validation.duration"));
                    Bukkit.getScheduler().runTask(plugin, this::open);
                }
            });
        });
    }

    private void modifyNumericDuration(int delta) {
        String current = rule.getDuration();
        
        // Only works for numeric durations
        Matcher matcher = DURATION_PATTERN.matcher(current);
        if (!matcher.matches()) {
            // Not a numeric duration, ignore
            return;
        }

        try {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            
            int newValue = Math.max(1, value + delta); // Minimum value is 1
            String newDuration = newValue + unit;
            
            applyDuration(newDuration);
        } catch (NumberFormatException e) {
            // Should not happen, but ignore if it does
        }
    }

    private void applyDuration(String duration) {
        rule.setDuration(duration);
        plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.act.duration.success"));
                if (afterApplyMenu != null) {
                    afterApplyMenu.open();
                } else {
                    refresh();
                }
            });
        });
    }
}
