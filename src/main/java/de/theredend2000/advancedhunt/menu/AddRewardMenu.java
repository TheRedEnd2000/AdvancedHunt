package de.theredend2000.advancedhunt.menu;

import de.theredend2000.advancedhunt.Main;
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
        return plugin.getMessageManager().getMessage("gui.rewards.add_menu.title", false);
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
        addButton(10, addItem, e -> parentMenu.openAddItemRewardMenu());

        ItemStack addCommand = new ItemBuilder(Material.COMMAND_BLOCK)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.add_command.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.add_command.lore", false))
            .build();
        addButton(12, addCommand, e -> parentMenu.promptAddCommandReward());

        ItemStack addChatMessage = new ItemBuilder(Material.WRITABLE_BOOK)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.add_chat_message.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.add_chat_message.lore", false))
            .build();
        addButton(14, addChatMessage, e -> parentMenu.promptAddChatMessageReward());

        ItemStack addBroadcast = new ItemBuilder(Material.BELL)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.add_broadcast_message.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.add_broadcast_message.lore", false))
            .build();
        addButton(16, addBroadcast, e -> parentMenu.promptAddBroadcastMessageReward());

        fillBackground(FILLER_GLASS);
    }
}
