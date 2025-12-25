package de.theredend2000.advancedHunt.menu;

import de.theredend2000.advancedHunt.Main;
import de.theredend2000.advancedHunt.model.Reward;
import de.theredend2000.advancedHunt.model.RewardType;
import de.theredend2000.advancedHunt.util.ItemBuilder;
import de.theredend2000.advancedHunt.util.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

/**
 * A menu for adding an item reward to a treasure.
 * Player places an item in the center slot, then confirms to add it as a reward.
 */
public class AddItemRewardMenu extends Menu {

    private final RewardsMenu parentMenu;
    private static final int ITEM_SLOT = 22; // Center slot
    private boolean confirmed = false;

    public AddItemRewardMenu(Player player, Main plugin, RewardsMenu parentMenu) {
        super(player, plugin);
        this.parentMenu = parentMenu;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.add_item_reward.title", false);
    }

    @Override
    public int getSlots() {
        return 45; // 5 rows
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        int slot = e.getRawSlot();
        
        // Allow interaction with the item slot
        if (slot == ITEM_SLOT) {
            e.setCancelled(false);
        }
        
        // Allow shift-clicking from player inventory
        if (slot >= 45 && e.isShiftClick()) {
            e.setCancelled(false);
        }
    }

    @Override
    public void setMenuItems() {
        fillBorders(FILLER_GLASS);
        
        // Item placement indicator (shows before item is placed)
        ItemStack placeholder = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.add_item_reward.placeholder.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.add_item_reward.placeholder.lore", false))
            .build();
        addStaticItem(ITEM_SLOT, placeholder);
        
        // Instructions
        ItemStack info = new ItemBuilder(Material.BOOK)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.add_item_reward.info.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.add_item_reward.info.lore", false))
            .build();
        addStaticItem(4, info);
        
        // Confirm button
        ItemStack confirm = new ItemBuilder(Material.LIME_CONCRETE)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.add_item_reward.confirm.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.add_item_reward.confirm.lore", false))
            .build();
        addButton(39, confirm, e -> confirmAdd());
        
        // Cancel button
        ItemStack cancel = new ItemBuilder(Material.RED_CONCRETE)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.add_item_reward.cancel.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.add_item_reward.cancel.lore", false))
            .build();
        addButton(41, cancel, e -> cancelAndReturn());
        
        fillBackground(FILLER_GLASS);
        
        // Clear the item slot so player can place items
        inventory.setItem(ITEM_SLOT, null);
        buttons[ITEM_SLOT] = null;
    }

    /**
     * Confirms adding the item as a reward.
     */
    private void confirmAdd() {
        ItemStack item = inventory.getItem(ITEM_SLOT);
        
        if (item == null || item.getType() == Material.AIR) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.add_item_reward.no_item"));
            return;
        }
        
        confirmed = true;
        
        // Prompt for chance
        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.rewards.enter_chance"));
        plugin.getChatInputListener().requestInput(playerMenuUtility, chanceStr -> {
            try {
                double chance = Double.parseDouble(chanceStr);
                if (chance < 0 || chance > 100) {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.rewards.invalid_chance"));
                    // Return item to player
                    giveItemBack(item);
                    parentMenu.open();
                    return;
                }
                
                // Serialize item and create reward
                String serialized = ItemSerializer.serialize(item);
                Reward reward = new Reward(RewardType.ITEM, chance, serialized);
                
                parentMenu.addReward(reward);
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.rewards.reward_added"));
                parentMenu.open();
                
            } catch (NumberFormatException ex) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.rewards.invalid_chance"));
                // Return item to player
                giveItemBack(item);
                parentMenu.open();
            }
        });
    }

    /**
     * Cancels and returns to the parent menu.
     */
    private void cancelAndReturn() {
        confirmed = true; // Prevent onClose from triggering
        
        // Return any item in the slot
        ItemStack item = inventory.getItem(ITEM_SLOT);
        if (item != null && item.getType() != Material.AIR) {
            giveItemBack(item);
        }
        
        parentMenu.open();
    }

    /**
     * Returns an item to the player's inventory or drops it.
     */
    private void giveItemBack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        
        var leftover = playerMenuUtility.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            // Drop items that don't fit
            leftover.values().forEach(i -> 
                playerMenuUtility.getWorld().dropItemNaturally(playerMenuUtility.getLocation(), i));
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (!confirmed) {
            // Return any item in the slot
            ItemStack item = inventory.getItem(ITEM_SLOT);
            if (item != null && item.getType() != Material.AIR) {
                giveItemBack(item);
            }
            
            // Reopen parent menu after a tick (to avoid issues with close event)
            Bukkit.getScheduler().runTaskLater(plugin, () -> parentMenu.open(), 1L);
        }
    }
}
