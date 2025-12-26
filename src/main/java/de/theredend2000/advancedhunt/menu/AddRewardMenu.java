package de.theredend2000.advancedhunt.menu;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.Reward;
import de.theredend2000.advancedhunt.model.RewardType;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Sub menu for choosing which type of reward to add.
 * Kept as a simple button-only menu for Bedrock compatibility.
 */
public class AddRewardMenu extends Menu {

    private final RewardsMenu parentMenu;

    public AddRewardMenu(Player player, Main plugin, RewardsMenu parentMenu) {
        super(player, plugin);
        this.parentMenu = parentMenu;
        setPreviousMenu(parentMenu);
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.rewards.open_reward_option_menu.title", false);
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
        fillBorders(FILLER_GLASS);

        ItemStack addItem = new ItemBuilder(Material.CHEST)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.add_item.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.add_item.lore", false))
            .build();
        addButton(10, addItem, e -> openAddItemRewardMenu());

        ItemStack addCommand = new ItemBuilder(Material.COMMAND_BLOCK)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.add_command.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.add_command.lore", false))
            .build();
        addButton(12, addCommand, e -> promptAddCommandReward());

        ItemStack addChatMessage = new ItemBuilder(Material.WRITABLE_BOOK)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.add_chat_message.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.add_chat_message.lore", false))
            .build();
        addButton(14, addChatMessage, e -> promptAddChatMessageReward());

        ItemStack addBroadcast = new ItemBuilder(Material.BELL)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.add_broadcast_message.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.add_broadcast_message.lore", false))
            .build();
        addButton(16, addBroadcast, e -> promptAddBroadcastMessageReward());

        fillBackground(FILLER_GLASS);
    }

    public void openAddItemRewardMenu() {
        new AddItemRewardMenu(playerMenuUtility, plugin, parentMenu,this).open();
    }

    /**
     * Prompts for chat message input to add a chat message reward.
     * Defaults to 100% chance since chat messages are typically always shown.
     */
    public void promptAddChatMessageReward() {
        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.prompt.chat_message"));
        plugin.getChatInputListener().requestInput(playerMenuUtility, message -> {
            if (message == null || message.isEmpty()) {
                open();
                return;
            }

            // Default to 100% chance for chat messages
            parentMenu.addReward(new Reward(RewardType.CHAT_MESSAGE, 100.0, message));
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.added"));
            parentMenu.open();
        });
    }

    /**
     * Prompts for broadcast message input to add a broadcast message reward.
     * Defaults to 100% chance since broadcast messages are typically always shown.
     */
    public void promptAddBroadcastMessageReward() {
        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.prompt.broadcast_message"));
        plugin.getChatInputListener().requestInput(playerMenuUtility, message -> {
            if (message == null || message.isEmpty()) {
                open();
                return;
            }

            // Default to 100% chance for broadcast messages
            parentMenu.addReward(new Reward(RewardType.CHAT_MESSAGE_BROADCAST, 100.0, message));
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.added"));
            parentMenu.open();
        });
    }

    /**
     * Prompts for command input to add a command reward.
     */
    public void promptAddCommandReward() {
        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.prompt.command"));
        plugin.getChatInputListener().requestInput(playerMenuUtility, command -> {
            if (command == null || command.isEmpty()) {
                open();
                return;
            }

            if (plugin.getRewardManager().isCommandBlacklisted(command)) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.rewards.command_blacklisted"));
                open();
                return;
            }

            // Now ask for chance
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.prompt.chance"));
            plugin.getChatInputListener().requestInput(playerMenuUtility, chanceStr -> {
                try {
                    double chance = Double.parseDouble(chanceStr);
                    if (chance < 0 || chance > 100) {
                        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.rewards.invalid_chance"));
                        open();
                        return;
                    }

                    parentMenu.addReward(new Reward(RewardType.COMMAND, chance, command));
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.added"));
                    parentMenu.open();
                } catch (NumberFormatException ex) {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.rewards.invalid_chance"));
                    open();
                }
            });
        });
    }
}
