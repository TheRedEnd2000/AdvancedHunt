package de.theredend2000.advancedhunt.menu.cron;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.ActRuleEditorMenu;
import de.theredend2000.advancedhunt.menu.CollectionSettingsMenu;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.model.ActRule;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.util.CronUtils;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ValidationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class CronEditorMenu extends Menu {

    private final Collection collection;
    private final ActRule actRule;
    private final boolean isActRuleContext;

    // Constructor for Collection context (progress reset cron)
    public CronEditorMenu(Player playerMenuUtility, Main plugin, Collection collection) {
        super(playerMenuUtility, plugin);
        this.collection = collection;
        this.actRule = null;
        this.isActRuleContext = false;
    }

    // Constructor for ActRule context (ACT rule cron component)
    public CronEditorMenu(Player playerMenuUtility, Main plugin, Collection collection, ActRule actRule) {
        super(playerMenuUtility, plugin);
        this.collection = collection;
        this.actRule = actRule;
        this.isActRuleContext = true;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.cron.editor.title");
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

        // Current Cron Display
        List<String> currentLore = new ArrayList<>();
        String currentCron = getCurrentCron();
        
        if (currentCron != null && !currentCron.isEmpty()) {
            currentLore.add(plugin.getMessageManager().getMessage("gui.cron.editor.current.expression") + " §f" + currentCron);
            currentLore.add("");
            
            // Calculate next executions
            List<String> nextRuns = CronUtils.getNextExecutions(currentCron, 3);
            if (!nextRuns.isEmpty()) {
                currentLore.add(plugin.getMessageManager().getMessage("gui.cron.editor.current.next_runs"));
                for (int i = 0; i < nextRuns.size(); i++) {
                    currentLore.add("§7" + (i + 1) + ". §f" + nextRuns.get(i));
                }
            } else {
                currentLore.add(plugin.getMessageManager().getMessage("gui.cron.editor.current.invalid"));
            }
        } else {
            currentLore.add(plugin.getMessageManager().getMessage("gui.cron.editor.current.none"));
        }

        addStaticItem(4, new ItemBuilder(Material.CLOCK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.editor.current.name"))
                .setLore(currentLore.toArray(new String[0]))
                .build());

        // Quick Presets Row
        addStaticItem(10,new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.preset.information.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.preset.information.lore"))
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGJmOGI2Mjc3Y2QzNjI2NjI4M2NiNWE5ZTY5NDM5NTNjNzgzZTZmZjdkNmEyZDU5ZDE1YWQwNjk3ZTkxZDQzYyJ9fX0=")
                .build());

        addButton(11, new ItemBuilder(Material.SUNFLOWER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.preset.daily.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.preset.daily.lore").toArray(new String[0]))
                .build(), (e) -> applyPreset("0 0 0 * * ? *", "gui.cron.preset.daily.name"));

        addButton(12, new ItemBuilder(Material.PAPER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.preset.weekly.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.preset.weekly.lore").toArray(new String[0]))
                .build(), (e) -> applyPreset("0 0 0 ? * MON *", "gui.cron.preset.weekly.name"));

        addButton(13, new ItemBuilder(Material.WRITABLE_BOOK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.preset.monthly.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.preset.monthly.lore").toArray(new String[0]))
                .build(), (e) -> applyPreset("0 0 0 1 * ? *", "gui.cron.preset.monthly.name"));

        addButton(14, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.preset.yearly.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.preset.yearly.lore").toArray(new String[0]))
                .build(), (e) -> applyPreset("0 0 0 1 1 ? *", "gui.cron.preset.yearly.name"));

        // More Presets Button
        addButton(15, new ItemBuilder(Material.BOOKSHELF)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.preset.more.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.preset.more.lore").toArray(new String[0]))
                .build(), (e) -> {
            CronPresetMenu presetMenu = new CronPresetMenu(playerMenuUtility, plugin, collection);
            presetMenu.setPreviousMenu(this);
            presetMenu.open();
        });

        // Advanced Options Row
        addStaticItem(19,new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.custom.information.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.custom.information.lore"))
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGJmOGI2Mjc3Y2QzNjI2NjI4M2NiNWE5ZTY5NDM5NTNjNzgzZTZmZjdkNmEyZDU5ZDE1YWQwNjk3ZTkxZDQzYyJ9fX0=")
                .build());

        addButton(20, new ItemBuilder(Material.REDSTONE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.custom.builder.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.custom.builder.lore").toArray(new String[0]))
                .build(), (e) -> {
            CronFieldMenu fieldMenu = isActRuleContext ? 
                new CronFieldMenu(playerMenuUtility, plugin, collection, actRule, currentCron != null ? currentCron : "0 0 0 * * ? *") :
                new CronFieldMenu(playerMenuUtility, plugin, collection, currentCron != null ? currentCron : "0 0 0 * * ? *");
            fieldMenu.setPreviousMenu(this);
            fieldMenu.open();
        });

        addButton(21, new ItemBuilder(Material.PAPER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.custom.manual.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.custom.manual.lore").toArray(new String[0]))
                .build(), (e) -> {
            plugin.getChatInputListener().requestInput(playerMenuUtility, (input) -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        new CronEditorMenu(playerMenuUtility, plugin, collection).open();
                        return;
                    }
                    
                    if (ValidationUtil.validateCron(input)) {
                        applyCron(input);
                    } else {
                        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.cron.error.invalid"));
                        reopenMenu();
                    }
                });
            });
        });

        // Clear Button
        addButton(43, new ItemBuilder(Material.BARRIER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.clear.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.clear.lore").toArray(new String[0]))
                .build(), (e) -> {
            clearCron();
        });
    }

    private void applyPreset(String cronExpression, String presetNameKey) {
        applyCron(cronExpression);
        String presetName = plugin.getMessageManager().getMessage(presetNameKey);
        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.cron.success.preset", "%preset%", presetName));
    }

    // Context-aware helper methods
    
    private String getCurrentCron() {
        if (isActRuleContext) {
            return actRule.getCronExpression();
        } else {
            return collection.getProgressResetCron();
        }
    }
    
    private void applyCron(String cronExpression) {
        if (isActRuleContext) {
            actRule.setCronExpression(cronExpression);
        } else {
            collection.setProgressResetCron(cronExpression);
        }
        
        plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.cron.success.applied"));
                reopenMenu();
            });
        });
    }
    
    private void clearCron() {
        if (isActRuleContext) {
            actRule.setCronExpression("NONE");
        } else {
            collection.setResetCron(null);
        }
        
        plugin.getCollectionManager().saveCollection(collection).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.cron.success.cleared"));
                reopenMenu();
            });
        });
    }
    
    private void reopenMenu() {
        if (isActRuleContext) {
            new CronEditorMenu(playerMenuUtility, plugin, collection, actRule).open();
        } else {
            new CronEditorMenu(playerMenuUtility, plugin, collection).open();
        }
    }
    
    private void navigateBack() {
        if (isActRuleContext) {
            new ActRuleEditorMenu(playerMenuUtility, plugin, collection, actRule).open();
        } else {
            new CollectionSettingsMenu(playerMenuUtility, plugin, collection).open();
        }
    }

}
