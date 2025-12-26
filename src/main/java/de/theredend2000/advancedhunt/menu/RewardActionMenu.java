package de.theredend2000.advancedhunt.menu;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.Reward;
import de.theredend2000.advancedhunt.model.RewardType;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Action menu for a specific reward - allows editing chance or deleting.
 * Bedrock-compatible alternative to shift-click/right-click actions.
 */
public class RewardActionMenu extends Menu {

    private final RewardsMenu parentMenu;
    private final Reward reward;
    private final int rewardIndex;

    public RewardActionMenu(Player player, Main plugin, RewardsMenu parentMenu, Reward reward, int rewardIndex) {
        super(player, plugin);
        this.parentMenu = parentMenu;
        this.reward = reward;
        this.rewardIndex = rewardIndex;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.rewards.editor.title", false,
            "%number%", String.valueOf(rewardIndex + 1));
    }

    @Override
    public int getSlots() {
        return 27; // 3 rows - compact menu
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        // All handled by buttons
    }

    @Override
    public void setMenuItems() {
        fillBorders(FILLER_GLASS);
        
        // Display the reward in center top
        ItemStack rewardDisplay = createRewardPreview();
        addStaticItem(4, rewardDisplay);
        
        // Edit Chance button
        ItemStack editChance = new ItemBuilder(Material.CLOCK)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.editor.edit_chance.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.editor.edit_chance.lore", false,
                "%chance%", formatChance(reward.getChance())))
            .build();
        addButton(11, editChance, e -> editChance());
        
        // Delete button
        ItemStack delete = new ItemBuilder(Material.RED_CONCRETE)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.editor.delete.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.editor.delete.lore", false))
            .build();
        addButton(15, delete, e -> confirmDelete());
        
        fillBackground(FILLER_GLASS);
    }

    /**
     * Creates a preview display of the reward.
     */
    private ItemStack createRewardPreview() {
        if (reward.getType() == RewardType.ITEM) {
            ItemStack item = ItemSerializer.deserialize(reward.getValue());
            if (item != null && item.getType() != Material.AIR) {
                ItemBuilder builder = new ItemBuilder(item.clone());
                List<String> lore = new ArrayList<>();
                if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                    lore.addAll(item.getItemMeta().getLore());
                }
                lore.add("");
                lore.add(ChatColor.DARK_GRAY + "─────────────");
                lore.add(plugin.getMessageManager().getMessage("gui.rewards.chance_lore", false,
                    "%chance%", formatChance(reward.getChance())));
                return builder.setLore(lore).build();
            }
        }
        
        // Command or Chat Message reward
        if (reward.getType() == RewardType.COMMAND) {
            String displayValue = "/" + (reward.getValue().length() > 30 ? reward.getValue().substring(0, 27) + "..." : reward.getValue());
            
            return new ItemBuilder(Material.COMMAND_BLOCK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.command_name", false))
                .setLore(
                    "",
                    plugin.getMessageManager().getMessage("gui.rewards.command_label", false),
                    ChatColor.WHITE + displayValue,
                    "",
                    ChatColor.DARK_GRAY + "─────────────",
                    plugin.getMessageManager().getMessage("gui.rewards.chance_lore", false,
                        "%chance%", formatChance(reward.getChance()))
                )
                .build();
        } else if (reward.getType() == RewardType.CHAT_MESSAGE) {
            String displayValue = reward.getValue().length() > 30 ? reward.getValue().substring(0, 27) + "..." : reward.getValue();
            
            return new ItemBuilder(Material.WRITABLE_BOOK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.chat_message_name", false))
                .setLore(
                    "",
                    plugin.getMessageManager().getMessage("gui.rewards.chat_message_label", false),
                    ChatColor.WHITE + displayValue,
                    "",
                    ChatColor.DARK_GRAY + "─────────────",
                    plugin.getMessageManager().getMessage("gui.rewards.chance_lore", false,
                        "%chance%", formatChance(reward.getChance()))
                )
                .build();
        } else if (reward.getType() == RewardType.CHAT_MESSAGE_BROADCAST) {
            String displayValue = reward.getValue().length() > 30 ? reward.getValue().substring(0, 27) + "..." : reward.getValue();
            
            return new ItemBuilder(Material.BELL)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.broadcast_message_name", false))
                .setLore(
                    "",
                    plugin.getMessageManager().getMessage("gui.rewards.broadcast_message_label", false),
                    ChatColor.WHITE + displayValue,
                    "",
                    ChatColor.DARK_GRAY + "─────────────",
                    plugin.getMessageManager().getMessage("gui.rewards.chance_lore", false,
                        "%chance%", formatChance(reward.getChance()))
                )
                .build();
        }
        
        // Invalid item
        return new ItemBuilder(Material.BARRIER)
            .setDisplayName(ChatColor.RED + "Invalid Item")
            .setLore(
                "",
                ChatColor.GRAY + "This reward could not be loaded",
                "",
                ChatColor.DARK_GRAY + "─────────────",
                plugin.getMessageManager().getMessage("gui.rewards.chance_lore", false,
                    "%chance%", formatChance(reward.getChance()))
            )
            .build();
    }

    /**
     * Opens chat prompt to edit the chance.
     */
    private void editChance() {
        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.prompt.new_chance",
            "%current%", formatChance(reward.getChance())));
        
        plugin.getChatInputListener().requestInput(playerMenuUtility, chanceStr -> {
            try {
                double chance = Double.parseDouble(chanceStr);
                if (chance < 0 || chance > 100) {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.rewards.invalid_chance"));
                    parentMenu.open();
                    return;
                }
                
                parentMenu.updateRewardChance(rewardIndex, chance);
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.rewards.chance_updated"));
                parentMenu.open();
            } catch (NumberFormatException ex) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.rewards.invalid_chance"));
                parentMenu.open();
            }
        });
    }

    /**
     * Opens a confirmation menu before deleting.
     */
    private void confirmDelete() {
        new ConfirmDeleteMenu(playerMenuUtility, plugin, this).open();
    }

    /**
     * Actually deletes the reward (called from confirmation menu).
     */
    public void executeDelete() {
        parentMenu.deleteReward(rewardIndex);
        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.deleted"));
        parentMenu.open();
    }

    /**
     * Returns to this menu (from confirmation menu on cancel).
     */
    public void returnToActionMenu() {
        open();
    }

    private String formatChance(double chance) {
        if (chance == (int) chance) {
            return String.valueOf((int) chance);
        }
        return String.format("%.1f", chance);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Inner class for delete confirmation
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Simple confirmation menu for deleting a reward.
     */
    public static class ConfirmDeleteMenu extends Menu {

        private final RewardActionMenu parentMenu;

        public ConfirmDeleteMenu(Player player, Main plugin, RewardActionMenu parentMenu) {
            super(player, plugin);
            this.parentMenu = parentMenu;
        }

        @Override
        public String getMenuName() {
            return plugin.getMessageManager().getMessage("gui.rewards.editor.confirm_delete.title", false);
        }

        @Override
        public int getSlots() {
            return 27;
        }

        @Override
        public void handleMenu(InventoryClickEvent e) {
            // All handled by buttons
        }

        @Override
        public void setMenuItems() {
            fillBackground(FILLER_GLASS);
            
            // Warning message
            ItemStack warning = new ItemBuilder(Material.CLOCK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.editor.confirm_delete.warning.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.rewards.editor.confirm_delete.warning.lore", false))
                .build();
            addStaticItem(4, warning);
            
            // Confirm delete
            ItemStack confirm = new ItemBuilder(Material.RED_CONCRETE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.editor.confirm_delete.confirm.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.rewards.editor.confirm_delete.confirm.lore", false))
                .build();
            addButton(11, confirm, e -> parentMenu.executeDelete());
            
            // Cancel
            ItemStack cancel = new ItemBuilder(Material.LIME_CONCRETE)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.editor.confirm_delete.cancel.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.rewards.editor.confirm_delete.cancel.lore", false))
                .build();
            addButton(15, cancel, e -> parentMenu.returnToActionMenu());
        }
    }
}
