package de.theredend2000.advancedhunt.menu;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.Reward;
import de.theredend2000.advancedhunt.model.RewardType;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
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
        addButton(canHaveMessage(reward.getType()) ? 11 : 12, editChance, e -> editChance());

        // Truncate long values for display
        String displayValue;
        if(reward.getType() != RewardType.ITEM)
            displayValue = truncate(reward.getValue(), 30);
        else
            displayValue = ItemSerializer.deserialize(reward.getValue()).getType().name();
        String displayMessage = truncate(formatMessage(reward.getMessage()), 30);
        String displayBroadcast = truncate(formatMessage(reward.getBroadcast()), 30);

        // Edit Message button (item/cmd)
        ItemStack message = new ItemBuilder(Material.WRITABLE_BOOK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.editor.edit_message.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.rewards.editor.edit_message.lore", false,
                        "%message%", displayMessage))
                .build();
        addButton(canHaveMessage(reward.getType()) ? 14 : 26, message, this::editMessage);

        // Edit Broadcast button (item/cmd)
        ItemStack broadcast = new ItemBuilder(Material.BELL)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.editor.edit_broadcast.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.rewards.editor.edit_broadcast.lore", false,
                        "%broadcast%", displayBroadcast))
                .build();
        addButton(canHaveMessage(reward.getType()) ? 15 : 26, broadcast, this::editBroadcast);

        // Edit value button
        ItemStack value = new ItemBuilder(Material.CAULDRON)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.editor.edit_value.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.rewards.editor.edit_value.lore", false,
                        "%value%", displayValue))
                .build();
        addButton(canHaveMessage(reward.getType()) ? 12 : 14, value, e -> editValue());
        
        // Delete button
        ItemStack delete = new ItemBuilder(Material.RED_CONCRETE)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.editor.delete.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.editor.delete.lore", false))
            .build();
        addButton(26, delete, e -> confirmDelete());
        
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

                // Add message info
                String messageValue = null;
                if(reward.getMessage() != null) messageValue = reward.getMessage().length() > 30 ? reward.getMessage().substring(0, 27) + "..." : reward.getMessage();
                lore.add(plugin.getMessageManager().getMessage("gui.rewards.message_lore", false,
                        "%message%", formatMessage(messageValue)));

                // Add broadcast info
                String broadcastValue = null;
                if(reward.getBroadcast() != null) broadcastValue = reward.getBroadcast().length() > 30 ? reward.getBroadcast().substring(0, 27) + "..." : reward.getBroadcast();
                lore.add(plugin.getMessageManager().getMessage("gui.rewards.broadcast_lore", false,
                        "%broadcast%", formatMessage(broadcastValue)));

                return builder.setLore(lore).build();
            }
        }

        // Command or Chat Message reward
        if (reward.getType() == RewardType.COMMAND) {
            String displayValue = "/" + (reward.getValue().length() > 30 ? reward.getValue().substring(0, 27) + "..." : reward.getValue());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(plugin.getMessageManager().getMessage("gui.rewards.command_label", false));
            lore.add(ChatColor.WHITE + displayValue);
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "─────────────");
            lore.add(plugin.getMessageManager().getMessage("gui.rewards.chance_lore", false,
                    "%chance%", formatChance(reward.getChance())));

            // Add message info
            String messageValue = null;
            if(reward.getMessage() != null) messageValue = reward.getMessage().length() > 30 ? reward.getMessage().substring(0, 27) + "..." : reward.getMessage();
            lore.add(plugin.getMessageManager().getMessage("gui.rewards.message_lore", false,
                    "%message%", formatMessage(messageValue)));

            // Add broadcast info
            String broadcastValue = null;
            if(reward.getBroadcast() != null) broadcastValue = reward.getBroadcast().length() > 30 ? reward.getBroadcast().substring(0, 27) + "..." : reward.getBroadcast();
            lore.add(plugin.getMessageManager().getMessage("gui.rewards.broadcast_lore", false,
                    "%broadcast%", formatMessage(broadcastValue)));

            return new ItemBuilder(Material.COMMAND_BLOCK)
                    .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.command_name", false))
                    .setLore(lore)
                    .build();
        }else if (reward.getType() == RewardType.CHAT_MESSAGE) {
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

    private void editMessage(InventoryClickEvent e) {
        ClickType clickType = e.getClick();
        if(clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT){
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.prompt.new_message",
                    "%current%", formatMessage(reward.getMessage())));

            plugin.getChatInputListener().requestInput(playerMenuUtility, message -> {
                parentMenu.updateRewardMessage(rewardIndex,message);
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.rewards.message_updated"));
                parentMenu.open();
            });
        }else if(clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT){
            parentMenu.updateRewardMessage(rewardIndex,null);
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.rewards.message_reset"));
            parentMenu.open();
        }
    }

    private void editBroadcast(InventoryClickEvent e) {
        ClickType clickType = e.getClick();
        if(clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT){
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.prompt.new_broadcast",
                    "%current%", formatMessage(reward.getBroadcast())));

            plugin.getChatInputListener().requestInput(playerMenuUtility, message -> {
                parentMenu.updateRewardBroadcast(rewardIndex,message);
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.rewards.broadcast_updated"));
                parentMenu.open();
            });
        }else if(clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT){
            parentMenu.updateRewardBroadcast(rewardIndex,null);
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.rewards.broadcast_reset"));
            parentMenu.open();
        }
    }

    private void editValue() {
        if(reward.getType() != RewardType.ITEM) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.prompt.new_value",
                    "%current%", reward.getValue()));

            plugin.getChatInputListener().requestInput(playerMenuUtility, value -> {
                parentMenu.updateRewardValue(rewardIndex, value);
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.rewards.value_updated"));
                parentMenu.open();
            });
        }else{
            new AddItemRewardMenu(playerMenuUtility, plugin, parentMenu,this,true,rewardIndex).open();
        }
    }

    /**
     * Opens a confirmation menu before deleting.
     */
    private void confirmDelete() {
        String title = plugin.getMessageManager().getMessage("gui.rewards.editor.confirm_delete.title", false);
        ConfirmationMenu confirmMenu = new ConfirmationMenu(
            playerMenuUtility,
            plugin,
            title,
            e -> {
                parentMenu.deleteReward(rewardIndex);
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.deleted"));
                parentMenu.open();
            },
            e -> open()
        );
        confirmMenu.open();
    }

    private String formatChance(double chance) {
        if (chance == (int) chance) {
            return String.valueOf((int) chance);
        }
        return String.format("%.1f", chance);
    }

    private String formatMessage(String message){
        if(message == null) return "None";
        return message;
    }

    private boolean canHaveMessage(RewardType rewardType){
        return rewardType == RewardType.COMMAND || rewardType == RewardType.ITEM;
    }

    // Helper method
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
