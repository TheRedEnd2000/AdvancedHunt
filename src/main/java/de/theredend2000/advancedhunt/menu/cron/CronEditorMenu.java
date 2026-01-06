package de.theredend2000.advancedhunt.menu.cron;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.model.ActRule;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.util.CronUtils;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ValidationUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class CronEditorMenu extends Menu {

    private final CronExpressionHolder holder;
    private final CronEditPolicy policy;
    private Menu afterApplyMenu;

    // Constructor for Collection context (progress reset cron)
    public CronEditorMenu(Player playerMenuUtility, Main plugin, Collection collection) {
        super(playerMenuUtility, plugin);
        this.holder = new CollectionProgressResetCronHolder(plugin, collection);
        this.policy = CronEditPolicy.progressReset();
    }

    // Constructor for ActRule context (ACT rule cron component)
    public CronEditorMenu(Player playerMenuUtility, Main plugin, Collection collection, ActRule actRule) {
        super(playerMenuUtility, plugin);
        this.holder = new ActRuleCronHolder(plugin, collection, actRule);
        this.policy = CronEditPolicy.actSchedule();
    }

    public CronEditorMenu(Player playerMenuUtility, Main plugin, CronExpressionHolder holder, CronEditPolicy policy) {
        super(playerMenuUtility, plugin);
        this.holder = holder;
        this.policy = policy;
    }

    /**
     * Optional navigation target used by guided flows.
     * If set, applying or clearing a cron will open this menu instead of staying in the cron editor.
     */
    public CronEditorMenu setAfterApplyMenu(Menu afterApplyMenu) {
        this.afterApplyMenu = afterApplyMenu;
        return this;
    }

    public Menu getAfterApplyMenu() {
        return afterApplyMenu;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage(
                "gui.cron.editor.title",
                false,
                "%cron_type%",
                plugin.getMessageManager().getMessage(policy.cronTypeMessageKey(), false)
        );
    }

    @Override
    public int getSlots() {
        return 36;
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
        String currentCron = holder.getExpression();
        
        if (currentCron == null || currentCron.isEmpty() || policy.isNone(currentCron)) {
            currentLore.add(plugin.getMessageManager().getMessage("gui.cron.editor.current.none"));
        } else {
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
                currentLore.add(plugin.getMessageManager().getMessage("gui.cron.common.invalid_expression"));
            }
        }

        addStaticItem(4, new ItemBuilder(XMaterial.CLOCK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.editor.current.name"))
                .setLore(currentLore.toArray(new String[0]))
                .build());

        // Quick Presets Row
        addStaticItem(10,new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.preset.information.name"))
            .setLore(plugin.getMessageManager().getMessageList(
                "gui.cron.preset.information.lore",
                false,
                "%cron_type%",
                plugin.getMessageManager().getMessage(policy.cronTypeMessageKey(), false)
            ))
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGJmOGI2Mjc3Y2QzNjI2NjI4M2NiNWE5ZTY5NDM5NTNjNzgzZTZmZjdkNmEyZDU5ZDE1YWQwNjk3ZTkxZDQzYyJ9fX0=")
                .build());

        addButton(11, new ItemBuilder(XMaterial.SUNFLOWER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.preset.daily.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.preset.daily.lore").toArray(new String[0]))
            .build(), (e) -> applyPreset("0 0 0 * * ? *", "feedback.cron.preset.daily.name"));

        addButton(12, new ItemBuilder(XMaterial.PAPER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.preset.weekly.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.preset.weekly.lore").toArray(new String[0]))
            .build(), (e) -> applyPreset("0 0 0 ? * MON *", "feedback.cron.preset.weekly.name"));

        addButton(13, new ItemBuilder(XMaterial.WRITABLE_BOOK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.preset.monthly.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.preset.monthly.lore").toArray(new String[0]))
            .build(), (e) -> applyPreset("0 0 0 1 * ? *", "feedback.cron.preset.monthly.name"));

        addButton(14, new ItemBuilder(XMaterial.EXPERIENCE_BOTTLE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.preset.yearly.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.preset.yearly.lore").toArray(new String[0]))
            .build(), (e) -> applyPreset("0 0 0 1 1 ? *", "feedback.cron.preset.yearly.name"));

        // More Presets Button
        addButton(15, new ItemBuilder(XMaterial.BOOKSHELF)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.preset.more.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.preset.more.lore").toArray(new String[0]))
                .build(), (e) -> {
            CronPresetMenu presetMenu = new CronPresetMenu(playerMenuUtility, plugin, holder, policy);
            presetMenu.setPreviousMenu(this);
            presetMenu.open();
        });

        // Advanced Options Row
        addStaticItem(19,new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.custom.information.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.custom.information.lore"))
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGJmOGI2Mjc3Y2QzNjI2NjI4M2NiNWE5ZTY5NDM5NTNjNzgzZTZmZjdkNmEyZDU5ZDE1YWQwNjk3ZTkxZDQzYyJ9fX0=")
                .build());

        addButton(20, new ItemBuilder(XMaterial.REDSTONE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.custom.builder.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.custom.builder.lore").toArray(new String[0]))
                .build(), (e) -> {
            String builderStart = getBuilderStartExpression(currentCron);
            CronFieldMenu fieldMenu = new CronFieldMenu(playerMenuUtility, plugin, holder, policy, builderStart);
            fieldMenu.setPreviousMenu(this);
            fieldMenu.open();
        });

        addButton(21, new ItemBuilder(XMaterial.PAPER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.custom.manual.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.custom.manual.lore").toArray(new String[0]))
                .build(), (e) -> {
            plugin.getChatInputListener().requestInput(playerMenuUtility, (input) -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        this.open();
                        return;
                    }

                    if (policy.allowSpecialValues()) {
                        if (policy.allowNone() && input.equalsIgnoreCase(CronEditPolicy.SPECIAL_NONE)) {
                            applyCron(CronEditPolicy.SPECIAL_NONE);
                            return;
                        }
                    }
                    
                    if (ValidationUtil.validateCron(input)) {
                        applyCron(input);
                    } else {
                        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.cron.invalid"));
                        this.open();
                    }
                });
            });
        });

        // Clear Button
        addButton(35, new ItemBuilder(XMaterial.BARRIER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.cron.clear.name"))
                .setLore(plugin.getMessageManager().getMessageList("gui.cron.clear.lore").toArray(new String[0]))
                .build(), (e) -> {
            clearCron();
        });
    }

    private void applyPreset(String cronExpression, String presetNameKey) {
        applyCron(cronExpression);
        String presetName = plugin.getMessageManager().getMessage(presetNameKey);
        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.cron.preset_applied", "%preset%", presetName));
    }
    
    private void applyCron(String cronExpression) {
        holder.setExpression(cronExpression);
        holder.save().thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.cron.applied"));
                if (afterApplyMenu != null) {
                    afterApplyMenu.open();
                } else {
                    refresh();
                }
            });
        });
    }
    
    private void clearCron() {
        holder.clear();
        holder.save().thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.cron.cleared"));
                if (afterApplyMenu != null) {
                    afterApplyMenu.open();
                } else {
                    refresh();
                }
            });
        });
    }

    private String getBuilderStartExpression(String currentCron) {
        if (currentCron == null || currentCron.isEmpty() || policy.isSpecial(currentCron)) {
            return policy.defaultBuilderExpression();
        }
        return currentCron;
    }

}
