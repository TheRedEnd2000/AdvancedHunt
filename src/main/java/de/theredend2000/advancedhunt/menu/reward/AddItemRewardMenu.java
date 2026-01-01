package de.theredend2000.advancedhunt.menu.reward;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.model.Reward;
import de.theredend2000.advancedhunt.model.RewardType;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * A menu for adding an item reward to a treasure.
 * Player places an item in the center slot, then confirms to add it as a reward.
 */
public class AddItemRewardMenu extends Menu {

    private final RewardsMenu parentMenu;
    private final Menu lastMenu;
    private static final int ITEM_SLOT = 22; // Center slot
    private boolean confirmed = false;
    private final boolean edit;
    private final int rewardIndex;

    public AddItemRewardMenu(Player player, Main plugin, RewardsMenu parentMenu, Menu lastMenu, boolean edit, int rewardIndex) {
        super(player, plugin);
        this.parentMenu = parentMenu;
        this.lastMenu = lastMenu;
        this.edit = edit;
        this.rewardIndex = rewardIndex;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.rewards.add.item.title", false);
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
        
        // Allow interaction with player inventory
        if (slot >= getSlots()) {
            e.setCancelled(false);
        }
    }

    @Override
    public void handleDrag(InventoryDragEvent event) {
        Set<Integer> rawSlots = event.getRawSlots();
        int topSize = event.getView().getTopInventory().getSize();

        for (int slot : rawSlots) {
            if (slot < topSize) { 
                // If the slot is NOT the item slot, cancel
                if (slot != ITEM_SLOT) {
                    event.setCancelled(true);
                    ((Player) event.getWhoClicked()).updateInventory();
                    return;
                }
            }
        }
    }

    @Override
    public void addCloseOrBack() {
    }

    @Override
    public void setMenuItems() {
        fillBorders(FILLER_GLASS);
        
        // Item placement indicator (shows before item is placed)
        ItemStack placeholder = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.add.item.placeholder.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.add.item.placeholder.lore", false))
            .build();
        addStaticItem(ITEM_SLOT, placeholder);
        
        // Instructions
        ItemStack info = new ItemBuilder(Material.BOOK)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.add.item.info.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.add.item.info.lore", false))
            .build();
        addStaticItem(4, info);
        
        // Confirm button
        ItemStack confirm = new ItemBuilder(Material.LIME_CONCRETE)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.add.item.confirm.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.add.item.confirm.lore", false))
            .build();
        addButton(39, confirm, e -> confirmAdd());
        
        // Cancel button
        ItemStack cancel = new ItemBuilder(Material.RED_CONCRETE)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.add.item.cancel.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.add.item.cancel.lore", false))
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
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.rewards.add.item.no_item"));
            return;
        }
        
        confirmed = true;

        if(edit){
            String serialized = ItemSerializer.serialize(item);
            parentMenu.updateRewardValue(rewardIndex, serialized);
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("gui.rewards.value_updated"));
            parentMenu.open();
            return;
        }

        String serialized = ItemSerializer.serialize(item);
        Reward reward = new Reward(RewardType.ITEM, 100.0, null, null, serialized);
        parentMenu.addReward(reward);
        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.added"));
        parentMenu.open();
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

        lastMenu.open();
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
            Bukkit.getScheduler().runTaskLater(plugin, lastMenu::open, 1L);
        }
    }
}
