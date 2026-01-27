package de.theredend2000.advancedhunt.menu.reward;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.model.Reward;
import de.theredend2000.advancedhunt.model.RewardType;
import de.theredend2000.advancedhunt.util.ItemBuilder;
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

        ItemStack addItem = new ItemBuilder(XMaterial.CHEST)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.add_item.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.add_item.lore", false))
            .build();
        addButton(10, addItem, e -> openAddItemRewardMenu());

        ItemStack addCommand = new ItemBuilder(XMaterial.PLAYER_HEAD)
            .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWI2Y2VlOGZkYTdlZjBiM2FlMGViMDU3OWQ1Njc2Y2UzNmFmN2VmYzU3NGQ4ODcyOGYzODk0ZjZiMTY2NTM4In19fQ==")
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.add_command.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.add_command.lore", false))
            .build();
        addButton(12, addCommand, e -> promptAddCommandReward());

        ItemStack addChatMessage = new ItemBuilder(XMaterial.WRITABLE_BOOK)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.add_chat_message.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.add_chat_message.lore", false))
            .build();
        addButton(14, addChatMessage, e -> promptAddChatMessageReward());

        ItemStack addBroadcast = new ItemBuilder(XMaterial.BELL)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.add_broadcast_message.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.add_broadcast_message.lore", false))
            .build();
        addButton(16, addBroadcast, e -> promptAddBroadcastMessageReward());

        fillBackground(FILLER_GLASS);
    }

    public void openAddItemRewardMenu() {
        new AddItemRewardMenu(playerMenuUtility, plugin, parentMenu,this,false,-1).open();
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
            parentMenu.addReward(new Reward(RewardType.CHAT_MESSAGE, 100.0,null,null, message));
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
            parentMenu.addReward(new Reward(RewardType.CHAT_MESSAGE_BROADCAST, 100.0,null,null, message));
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

            parentMenu.addReward(new Reward(RewardType.COMMAND, 100.0, null, null, command));
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.added"));
            parentMenu.open();
        });
    }
}
