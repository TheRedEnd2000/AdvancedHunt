package de.theredend2000.advancedHunt.menu.cron;

import de.theredend2000.advancedHunt.Main;
import de.theredend2000.advancedHunt.menu.Menu;
import de.theredend2000.advancedHunt.model.ActRule;
import de.theredend2000.advancedHunt.model.Collection;
import de.theredend2000.advancedHunt.util.CronUtils;
import de.theredend2000.advancedHunt.util.ItemBuilder;
import de.theredend2000.advancedHunt.util.ValidationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CronFieldMenu extends Menu {

    private final Collection collection;
    private final ActRule actRule;
    private final boolean isActRuleContext;
    private final Map<CronField, String> fieldValues;
    private CronField selectedField;

    // Constructor for Collection context
    public CronFieldMenu(Player playerMenuUtility, Main plugin, Collection collection, String initialExpression) {
        super(playerMenuUtility, plugin);
        this.collection = collection;
        this.actRule = null;
        this.isActRuleContext = false;
        this.fieldValues = new LinkedHashMap<>();
        this.selectedField = CronField.SECOND;
        parseExpression(initialExpression);
    }

    // Constructor for ActRule context
    public CronFieldMenu(Player playerMenuUtility, Main plugin, Collection collection, ActRule actRule, String initialExpression) {
        super(playerMenuUtility, plugin);
        this.collection = collection;
        this.actRule = actRule;
        this.isActRuleContext = true;
        this.fieldValues = new LinkedHashMap<>();
        this.selectedField = CronField.SECOND;
        parseExpression(initialExpression);
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.cron.builder.title");
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
        fillBorders(super.FILLER_GLASS);

        // Preview Section (Top)
        List<String> previewLore = new ArrayList<>();
        previewLore.add(plugin.getMessageManager().getMessage("gui.cron.builder.preview.current_expression"));
        previewLore.add("§f" + buildExpression());
        previewLore.add("");
        
        List<String> nextRuns = CronUtils.getNextExecutions(buildExpression(), 2);
        if (!nextRuns.isEmpty()) {
            previewLore.add(plugin.getMessageManager().getMessage("gui.cron.builder.preview.next_runs"));
            for (int i = 0; i < nextRuns.size(); i++) {
                previewLore.add("§7" + (i + 1) + ". §f" + nextRuns.get(i));
            }
        } else {
            previewLore.add(plugin.getMessageManager().getMessage("gui.cron.builder.preview.invalid"));
        }

        addStaticItem(4, new ItemBuilder(Material.ENDER_EYE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.builder.preview.name"))
                .setLore(previewLore.toArray(new String[0]))
                .build());

        // Field Selection Row (slots 19-25)
        int slot = 19;
        for (CronField field : CronField.values()) {
            boolean isSelected = selectedField == field;
            String value = fieldValues.getOrDefault(field, field.defaultValue);
            
            addButton(slot++, new ItemBuilder(isSelected ? Material.LIME_DYE : Material.GRAY_DYE)
                    .setDisplayName(plugin.getMessageManager().getMessage(
                            isSelected ? "gui.cron.builder.field.selected_name" : "gui.cron.builder.field.unselected_name",
                            "%name%", field.getDisplayName(plugin)
                    ))
                    .setLore(plugin.getMessageManager().getMessageList("gui.cron.builder.field.lore", 
                            "%value%", value,
                            "%description%", field.getDescription(plugin)
                    ).toArray(new String[0]))
                    .build(), (e) -> {
                selectedField = field;
                refresh();
            });
        }

        // Value Modification Section (Center)
        String currentValue = fieldValues.getOrDefault(selectedField, selectedField.defaultValue);
        
        // Preset Values (Row 1 - slots 28-34)
        List<String> presets = selectedField.getPresetValues();
        for (int i = 0; i < 7 && i < presets.size(); i++) {
            String preset = presets.get(i);
            boolean isCurrent = currentValue.equals(preset);
            
            addButton(28 + i, new ItemBuilder(isCurrent ? Material.LIME_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE)
                    .setDisplayName(plugin.getMessageManager().getMessage(
                            isCurrent ? "gui.cron.builder.preset.current_name" : "gui.cron.builder.preset.set_name",
                            "%value%", preset
                    ))
                    .setLore(plugin.getMessageManager().getMessageList(
                            isCurrent ? "gui.cron.builder.preset.current_lore" : "gui.cron.builder.preset.set_lore",
                            "%description%", selectedField.getValueDescription(plugin, preset)
                    ).toArray(new String[0]))
                    .build(), (e) -> {
                fieldValues.put(selectedField, preset);
                refresh();
            });
        }

        // Custom Input Button
        addButton(31, new ItemBuilder(Material.ANVIL)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.builder.custom_input.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.builder.custom_input.lore",
                        "%field%", selectedField.getDisplayName(plugin)
                ).toArray(new String[0]))
                .build(), (e) -> {
            plugin.getChatInputListener().requestInput(playerMenuUtility, (input) -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        reopenMenu(buildExpression());
                        return;
                    }
                    
                    fieldValues.put(selectedField, input);
                    reopenMenu(buildExpression());
                });
            });
        });

        // Quick Modify Buttons
        addButton(37, new ItemBuilder(Material.RED_DYE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.builder.decrease.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.builder.decrease.lore").toArray(new String[0]))
                .build(), (e) -> {
            modifyNumericValue(-1);
        });

        addButton(43, new ItemBuilder(Material.LIME_DYE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.builder.increase.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.builder.increase.lore").toArray(new String[0]))
                .build(), (e) -> {
            modifyNumericValue(1);
        });

        // Action Buttons
        addButton(45, new ItemBuilder(Material.BARRIER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.builder.cancel.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.builder.cancel.lore").toArray(new String[0]))
                .build(), (e) -> {
            navigateBack();
        });

        addButton(53, new ItemBuilder(Material.EMERALD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.builder.save.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.builder.save.lore",
                        "%expression%", buildExpression()
                ).toArray(new String[0]))
                .build(), (e) -> {
            String expression = buildExpression();
            if (ValidationUtil.validateCron(expression)) {
                applyCron(expression);
            } else {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.cron.error.invalid"));
            }
        });
    }

    private void parseExpression(String expression) {
        try {
            String[] parts = expression.split(" ");
            CronField[] fields = CronField.values();
            
            for (int i = 0; i < Math.min(parts.length, fields.length); i++) {
                fieldValues.put(fields[i], parts[i]);
            }
            
            // Fill missing fields with defaults
            for (CronField field : fields) {
                fieldValues.putIfAbsent(field, field.defaultValue);
            }
        } catch (Exception e) {
            // Use defaults if parsing fails
            for (CronField field : CronField.values()) {
                fieldValues.put(field, field.defaultValue);
            }
        }
    }

    private String buildExpression() {
        String[] parts = new String[CronField.values().length];
        int i = 0;
        for (CronField field : CronField.values()) {
            parts[i++] = fieldValues.getOrDefault(field, field.defaultValue);
        }
        return String.join(" ", parts);
    }

    private void modifyNumericValue(int delta) {
        String current = fieldValues.getOrDefault(selectedField, selectedField.defaultValue);
        try {
            int value = Integer.parseInt(current);
            int newValue = Math.max(selectedField.minValue, Math.min(selectedField.maxValue, value + delta));
            fieldValues.put(selectedField, String.valueOf(newValue));
            refresh();
        } catch (NumberFormatException e) {
            // Not a numeric value, ignore
        }
    }

    // Context-aware helper methods
    
    private void applyCron(String expression) {
        if (isActRuleContext) {
            actRule.setCronExpression(expression);
        } else {
            collection.setResetCron(expression);
        }
        
        plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.cron.success.applied"));
                navigateBack();
            });
        });
    }
    
    private void reopenMenu(String expression) {
        if (isActRuleContext) {
            new CronFieldMenu(playerMenuUtility, plugin, collection, actRule, expression).open();
        } else {
            new CronFieldMenu(playerMenuUtility, plugin, collection, expression).open();
        }
    }
    
    private void navigateBack() {
        if (isActRuleContext) {
            new CronEditorMenu(playerMenuUtility, plugin, collection, actRule).open();
        } else {
            if (previousMenu != null) {
                previousMenu.open();
            } else {
                new CronEditorMenu(playerMenuUtility, plugin, collection).open();
            }
        }
    }

    private enum CronField {
        SECOND("second", "0", 0, 59,
                List.of("0", "*/5", "*/10", "*/15", "*/30", "*")),
        MINUTE("minute", "0", 0, 59,
                List.of("0", "*/5", "*/10", "*/15", "*/30", "*")),
        HOUR("hour", "0", 0, 23,
                List.of("0", "6", "12", "18", "*/6", "*/12", "*")),
        DAY("day", "*", 1, 31,
                List.of("1", "15", "L", "*", "?")),
        MONTH("month", "*", 1, 12,
                List.of("*", "1", "6", "12", "1,4,7,10")),
        DAY_OF_WEEK("day_of_week", "?", 1, 7,
                List.of("?", "*", "MON", "FRI", "SAT", "SUN", "MON-FRI")),
        YEAR("year", "*", 1970, 2099,
                List.of("*", "2025", "2026", "2027", "2030"));

        final String key;
        final String defaultValue;
        final int minValue;
        final int maxValue;
        final List<String> presetValues;

        CronField(String key, String defaultValue, int minValue, int maxValue, List<String> presetValues) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.presetValues = presetValues;
        }

        String getDisplayName(Main plugin) {
            return plugin.getMessageManager().getMessage("gui.cron.builder.fields." + key + ".name");
        }

        String getDescription(Main plugin) {
            return plugin.getMessageManager().getMessage("gui.cron.builder.fields." + key + ".description");
        }

        List<String> getPresetValues() {
            return presetValues;
        }

        String getValueDescription(Main plugin, String value) {
            String fieldKey = "gui.cron.builder.fields." + key + ".values.";
            
            // Handle wildcard patterns (*/N)
            if (value.startsWith("*/")) {
                String interval = value.substring(2);
                return plugin.getMessageManager().getMessage(fieldKey + "every_interval", "%interval%", interval);
            }
            
            // Handle specific numeric values for year
            if (this == YEAR && !value.equals("*")) {
                try {
                    Integer.parseInt(value);
                    return plugin.getMessageManager().getMessage(fieldKey + "specific_year", "%year%", value);
                } catch (NumberFormatException e) {
                    // Not a number, continue to specific lookup
                }
            }
            
            // Try specific value lookup
            String messageKey = fieldKey + sanitizeKey(value);
            String description = plugin.getMessageManager().getMessage(messageKey);
            
            // If not found, return custom value message
            if (description.equals(messageKey)) {
                return plugin.getMessageManager().getMessage("gui.cron.builder.fields.custom_value");
            }
            return description;
        }

        private String sanitizeKey(String value) {
            return value.replace("*", "wildcard")
                       .replace("/", "_")
                       .replace(",", "_")
                       .replace("-", "_")
                       .replace("?", "none")
                       .replace("#", "nth")
                       .replace("L", "last")
                       .toLowerCase();
        }
    }
}
