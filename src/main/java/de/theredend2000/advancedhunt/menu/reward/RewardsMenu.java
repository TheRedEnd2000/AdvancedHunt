package de.theredend2000.advancedhunt.menu.reward;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.PagedMenu;
import de.theredend2000.advancedhunt.model.*;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A GUI menu that displays the rewards configured for a RewardHolder (Treasure or Collection).
 * Supports pagination for holders with many rewards.
 */
public class RewardsMenu extends PagedMenu {

    private enum QuickActionMode {
        DELETE,
        GET_INSTANCE
    }

    private QuickActionMode quickActionMode = QuickActionMode.DELETE;

    private final RewardHolder holder;
    private final List<Reward> rewards;
    private String titleKey = "gui.rewards.title";
    
    // Optional context for switching between treasure and collection rewards
    private RewardHolder alternateHolder;
    private String alternateTitleKey;

    /**
     * Creates a reward menu.
     */
    public RewardsMenu(Player player, Main plugin, RewardHolder holder) {
        super(player, plugin);
        this.holder = holder;
        // Create a mutable copy for editing
        this.rewards = holder.getRewards() != null ? new ArrayList<>(holder.getRewards()) : new ArrayList<>();
        this.maxItemsPerPage = 28; // 4 rows × 7 columns
    }

    public RewardsMenu setTitleKey(String titleKey) {
        this.titleKey = titleKey;
        return this;
    }
    
    /**
     * Sets an alternate holder and title for switching contexts.
     * For example, allows switching between treasure and collection rewards.
     */
    public RewardsMenu setAlternateContext(RewardHolder alternateHolder, String alternateTitleKey) {
        this.alternateHolder = alternateHolder;
        this.alternateTitleKey = alternateTitleKey;
        return this;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage(titleKey, false);
    }

    @Override
    public int getSlots() {
        return 54; // 6 rows for PagedMenu layout
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        // All click handling is done via buttons
    }

    @Override
    public void setMenuItems() {
        // Reset index for this page
        index = 0;
        
        if (rewards.isEmpty()) {
            // Show "no rewards" indicator in center
            addMenuBorder();
            ItemStack noRewards = new ItemBuilder(Material.BARRIER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.no_rewards.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.rewards.no_rewards.lore", false))
                .build();
            addStaticItem(22, noRewards);
        } else {

            addPagedButtons(rewards.size());
            // Calculate pagination
            int startIndex = page * maxItemsPerPage;
            int endIndex = Math.min(startIndex + maxItemsPerPage, rewards.size());
            
            this.hasNextPage = endIndex < rewards.size();
            
            // Display rewards for current page
            for (int i = startIndex; i < endIndex; i++) {
                Reward reward = rewards.get(i);
                final int rewardIndex = i;
                ItemStack displayItem = createRewardDisplay(reward, i + 1);

                addPagedItem(index++, displayItem, e -> handleRewardClick(e, rewardIndex));
            }
            
            addMenuBorder();
        }
    }
    
    /**
     * Returns the total number of pages needed to display all rewards.
     */
    private int getTotalPages() {
        if (rewards.isEmpty()) return 1;
        return (int) Math.ceil((double) rewards.size() / maxItemsPerPage);
    }
    
    /**
     * Override to fix the next page check - uses actual reward count instead of index.
     */
    @Override
    public void addMenuBorder() {
        super.addMenuBorder();

        // Reward controls
        addButton(53, new ItemBuilder(Material.HOPPER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.open_reward_option_menu.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.rewards.open_reward_option_menu.lore", false))
                .build(), e -> new AddRewardMenu(playerMenuUtility, plugin, this).open());

        // Quick action mode toggle
        QuickActionMode mode = getQuickActionMode();
        addButton(52, new ItemBuilder(Material.LEVER)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.quick_mode.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.quick_mode.lore", false,
                "%mode%", getQuickActionModeDisplay(mode),
                "%action%", getQuickActionActionDisplay(mode)))
            .build(), e -> {
                setQuickActionMode(getNextQuickActionMode(getQuickActionMode()));
                updateQuickActionDisplay();
            });

        // Determine if we're in treasure or collection context
        RewardPresetType presetType = titleKey.contains("collection") ? RewardPresetType.COLLECTION : RewardPresetType.TREASURE;
        boolean isCollection = presetType == RewardPresetType.COLLECTION;
        String presetContext = presetType == RewardPresetType.COLLECTION ? "collection" : "treasure";

        // Preset save/load buttons (not when already editing a preset)
        if (!(holder instanceof PresetRewardHolder)) {
            // Preset save button
            addButton(45, new ItemBuilder(Material.WRITABLE_BOOK)
                    .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.save_preset_" + presetContext + ".name", false))
                    .setLore(plugin.getMessageManager().getMessageList("gui.rewards.save_preset_" + presetContext + ".lore", false))
                    .build(), e -> {
                // Prompt for preset name
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.prompt_name_" + presetContext));
                plugin.getChatInputListener().requestInput(playerMenuUtility, presetName -> {
                    if (presetName == null || presetName.trim().isEmpty()) {
                        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.preset.invalid_name"));
                        open();
                        return;
                    }

                    String trimmed = presetName.trim();
                    if (plugin.getRewardPresetManager().hasPresetName(presetType, trimmed)) {
                        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.preset.duplicate_name"));
                        open();
                        return;
                    }

                    plugin.getRewardPresetManager().createPreset(presetType, trimmed, new ArrayList<>(rewards)).thenAccept(success -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (!success) {
                                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.preset.save_failed_" + presetContext));
                                open();
                                return;
                            }
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.saved_" + presetContext,
                                    "%name%", trimmed));
                            open();
                        });
                    });
                });
            });

            // Preset load button
            addButton(46, new ItemBuilder(Material.WRITTEN_BOOK)
                    .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.load_preset_" + presetContext + ".name", false))
                    .setLore(plugin.getMessageManager().getMessageList("gui.rewards.load_preset_" + presetContext + ".lore", false))
                    .build(), e -> {

                Collection collectionContext = null;
                if (holder instanceof CollectionRewardHolder collectionRewardHolder) {
                    collectionContext = collectionRewardHolder.getCollection();
                } else if (alternateHolder instanceof CollectionRewardHolder collectionRewardHolder) {
                    collectionContext = collectionRewardHolder.getCollection();
                }

                new RewardPresetListMenu(playerMenuUtility, plugin, presetType, selected -> {
                    rewards.clear();
                    rewards.addAll(selected.getRewards());
                    holder.saveRewards(new ArrayList<>(rewards));
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.loaded_" + presetContext,
                            "%name%", selected.getName()));
                    Bukkit.getScheduler().runTask(plugin, RewardsMenu.this::open);
                }, collectionContext).setPreviousMenu(this).open();
            });
        }

        // Switch context button (treasure <-> collection)
        if (alternateHolder != null) {
            Material icon = isCollection ? Material.ENDER_CHEST : Material.CHEST;
            String switchKey = isCollection ? "gui.rewards.switch_to_treasure" : "gui.rewards.switch_to_collection";

            addButton(8, new ItemBuilder(icon)
                    .setDisplayName(plugin.getMessageManager().getMessage(switchKey + ".name", false))
                    .setLore(plugin.getMessageManager().getMessageList(switchKey + ".lore", false))
                    .build(), e -> {
                // Switch to the alternate context
                RewardsMenu newMenu = new RewardsMenu(playerMenuUtility, plugin, alternateHolder)
                        .setTitleKey(alternateTitleKey)
                        .setAlternateContext(holder, titleKey);
                newMenu.open();
            });
        }

    }

    /**
     * Creates a display ItemStack for a reward, showing its type, value, and chance.
     */
    private ItemStack createRewardDisplay(Reward reward, int rewardNumber) {
        String chanceLore = plugin.getMessageManager().getMessage("gui.rewards.chance_lore", false,
            "%chance%", formatChance(reward.getChance()));
        
        if (reward.getType() == RewardType.ITEM) {
            return createItemRewardDisplay(reward, chanceLore, rewardNumber);
        } else if (reward.getType() == RewardType.COMMAND) {
            return createCommandRewardDisplay(reward, chanceLore, rewardNumber);
        } else if (reward.getType() == RewardType.CHAT_MESSAGE) {
            return createChatMessageRewardDisplay(reward, chanceLore, rewardNumber);
        } else {
            return createBroadcastMessageRewardDisplay(reward, chanceLore, rewardNumber);
        }
    }

    /**
     * Creates display for an ITEM type reward - shows the actual item with chance in lore.
     */
    private ItemStack createItemRewardDisplay(Reward reward, String chanceLore, int rewardNumber) {
        ItemStack item = ItemSerializer.deserialize(reward.getValue());

        if (item == null || item.getType() == Material.AIR) {
            // Fallback for invalid item data
            return new ItemBuilder(Material.BARRIER)
                    .setDisplayName(ChatColor.RED + "Invalid Item")
                    .setLore(
                            ChatColor.GRAY + "This item could not be loaded",
                            "",
                            chanceLore
                    )
                    .build();
        }

        // Clone the item and append chance to lore
        ItemBuilder builder = new ItemBuilder(item.clone());

        // Get existing lore or create new
        List<String> lore = new ArrayList<>();
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            lore.addAll(item.getItemMeta().getLore());
        }

        // Add separator and chance info
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "─────────────");
        lore.add(plugin.getMessageManager().getMessage("gui.rewards.reward_number", false,
                "%number%", String.valueOf(rewardNumber)));
        lore.add(chanceLore);
        if (item.getAmount() > 1) {
            lore.add(plugin.getMessageManager().getMessage("gui.rewards.amount_lore", false,
                    "%amount%", String.valueOf(item.getAmount())));
        }

        // Add message info
        if (reward.getMessage() != null && !reward.getMessage().isEmpty()) {
            String messageValue = reward.getMessage().length() > 30
                    ? reward.getMessage().substring(0, 27) + "..."
                    : reward.getMessage();
            lore.add(plugin.getMessageManager().getMessage("gui.rewards.message_lore", false,
                    "%message%", messageValue));
        }

        // Add broadcast info
        if (reward.getBroadcast() != null && !reward.getBroadcast().isEmpty()) {
            String broadcastValue = reward.getBroadcast().length() > 30
                    ? reward.getBroadcast().substring(0, 27) + "..."
                    : reward.getBroadcast();
            lore.add(plugin.getMessageManager().getMessage("gui.rewards.broadcast_lore", false,
                    "%broadcast%", broadcastValue));
        }

        lore.add("");
        lore.add(plugin.getMessageManager().getMessage("gui.rewards.click_to_edit", false,
            "%action%", getQuickActionActionDisplay(getQuickActionMode())));

        return builder.setLore(lore).build();
    }

    /**
     * Creates display for a COMMAND type reward - shows a command block with command info.
     */
    private ItemStack createCommandRewardDisplay(Reward reward, String chanceLore, int rewardNumber) {
        String command = reward.getValue();

        // Truncate long commands for display
        String displayCommand = command.length() > 40
                ? command.substring(0, 37) + "..."
                : command;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(plugin.getMessageManager().getMessage("gui.rewards.command_label", false));
        lore.add(ChatColor.WHITE + "/" + displayCommand);

        // Show full command if truncated
        if (command.length() > 40) {
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "(Command truncated)");
        }

        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "─────────────");
        lore.add(plugin.getMessageManager().getMessage("gui.rewards.reward_number", false,
                "%number%", String.valueOf(rewardNumber)));
        lore.add(chanceLore);

        // Add message info
        if (reward.getMessage() != null && !reward.getMessage().isEmpty()) {
            String messageValue = reward.getMessage().length() > 30
                    ? reward.getMessage().substring(0, 27) + "..."
                    : reward.getMessage();
            lore.add(plugin.getMessageManager().getMessage("gui.rewards.message_lore", false,
                    "%message%", messageValue));
        }

        // Add broadcast info
        if (reward.getBroadcast() != null && !reward.getBroadcast().isEmpty()) {
            String broadcastValue = reward.getBroadcast().length() > 30
                    ? reward.getBroadcast().substring(0, 27) + "..."
                    : reward.getBroadcast();
            lore.add(plugin.getMessageManager().getMessage("gui.rewards.broadcast_lore", false,
                    "%broadcast%", broadcastValue));
        }

        lore.add("");
        lore.add(plugin.getMessageManager().getMessage("gui.rewards.click_to_edit", false,
            "%action%", getQuickActionActionDisplay(getQuickActionMode())));

        return new ItemBuilder(Material.COMMAND_BLOCK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.command_name", false))
                .setLore(lore)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Reward editing methods
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Handles clicking on a reward in edit mode.
     * Opens the action menu for Bedrock compatibility.
     */
    private void handleRewardClick(InventoryClickEvent e, int rewardIndex) {
        if (rewardIndex >= rewards.size()) return;

        ClickType clickType = e.getClick();
        if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            e.setCancelled(true);

            Reward reward = rewards.get(rewardIndex);
            QuickActionMode mode = getQuickActionMode();

            if (mode == QuickActionMode.DELETE) {
                deleteReward(rewardIndex);
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.deleted"));

                // Clamp page after deletion (e.g., deleting last item on last page)
                int totalPages = getTotalPages();
                if (page > totalPages - 1) {
                    page = Math.max(0, totalPages - 1);
                }
                open();
                return;
            }

            if (mode == QuickActionMode.GET_INSTANCE) {
                giveRewardInstance(reward);
                return;
            }
        }
        
        Reward reward = rewards.get(rewardIndex);
        new RewardActionMenu(playerMenuUtility, plugin, this, reward, rewardIndex).setPreviousMenu(this).open();
    }

    private QuickActionMode getQuickActionMode() {
        return quickActionMode;
    }

    private void setQuickActionMode(QuickActionMode mode) {
        this.quickActionMode = mode;
    }

    private QuickActionMode getNextQuickActionMode(QuickActionMode current) {
        if (current == QuickActionMode.DELETE) return QuickActionMode.GET_INSTANCE;
        return QuickActionMode.DELETE;
    }

    private String getQuickActionModeDisplay(QuickActionMode mode) {
        if (mode == QuickActionMode.GET_INSTANCE) {
            return plugin.getMessageManager().getMessage("gui.rewards.quick_mode.mode_get_instance", false);
        }
        return plugin.getMessageManager().getMessage("gui.rewards.quick_mode.mode_delete", false);
    }

    private String getQuickActionActionDisplay(QuickActionMode mode) {
        if (mode == QuickActionMode.GET_INSTANCE) {
            return plugin.getMessageManager().getMessage("gui.rewards.quick_action.get_instance", false);
        }
        return plugin.getMessageManager().getMessage("gui.rewards.quick_action.delete", false);
    }

    /**
     * Updates the quick action toggle icon and all reward item lores in-place
     * without recreating the entire menu. This is a performance optimization.
     * Only updates icons if the mode has actually changed.
     */
    private void updateQuickActionDisplay() {
        QuickActionMode mode = getQuickActionMode();
        
        // Update the lever icon at slot 47
        ItemStack leverIcon = new ItemBuilder(Material.LEVER)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.quick_mode.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.rewards.quick_mode.lore", false,
                "%mode%", getQuickActionModeDisplay(mode),
                "%action%", getQuickActionActionDisplay(mode)))
            .build();
        updateSlot(52, leverIcon);
        
        // Update all reward items' lores to reflect the new action
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, rewards.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Reward reward = rewards.get(i);
            int displayIndex = i - startIndex;
            int slot = getSlotForPagedIndex(displayIndex);
            
            ItemStack updatedDisplay = createRewardDisplay(reward, i + 1);
            updateSlot(slot, updatedDisplay);
        }
    }

    private void giveRewardInstance(Reward reward) {
        if (reward.getType() == RewardType.ITEM) {
            ItemStack item = ItemSerializer.deserialize(reward.getValue());
            if (item == null || item.getType() == Material.AIR) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.instance_invalid"));
                return;
            }

            ItemStack toGive = item.clone();
            Map<Integer, ItemStack> leftover = playerMenuUtility.getInventory().addItem(toGive);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(stack -> playerMenuUtility.getWorld().dropItemNaturally(playerMenuUtility.getLocation(), stack));
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.instance_dropped"));
            } else {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.instance_given"));
            }
            return;
        }

        // COMMAND, CHAT_MESSAGE, and CHAT_MESSAGE_BROADCAST rewards don't have a physical instance; show the configured value.
        if (reward.getType() == RewardType.COMMAND) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                "gui.rewards.command_preview",
                "%command%", reward.getValue()
            ));
        } else if (reward.getType() == RewardType.CHAT_MESSAGE) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                "gui.rewards.chat_message_preview",
                "%message%", reward.getValue()
            ));
        } else if (reward.getType() == RewardType.CHAT_MESSAGE_BROADCAST) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                "gui.rewards.broadcast_message_preview",
                "%message%", reward.getValue()
            ));
        }
    }

    /**
     * Creates display for a CHAT_MESSAGE_BROADCAST type reward - shows a bell with message info.
     */
    private ItemStack createBroadcastMessageRewardDisplay(Reward reward, String chanceLore, int rewardNumber) {
        String message = reward.getValue();
        
        // Truncate long messages for display
        String displayMessage = message.length() > 40 
            ? message.substring(0, 37) + "..." 
            : message;
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(plugin.getMessageManager().getMessage("gui.rewards.broadcast_message_label", false));
        lore.add(ChatColor.WHITE + displayMessage);
        
        // Show full message if truncated
        if (message.length() > 40) {
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "(Message truncated)");
        }
        
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "─────────────");
        lore.add(plugin.getMessageManager().getMessage("gui.rewards.reward_number", false,
            "%number%", String.valueOf(rewardNumber)));
        lore.add(chanceLore);
        
        lore.add("");
        lore.add(plugin.getMessageManager().getMessage("gui.rewards.click_to_edit", false,
            "%action%", getQuickActionActionDisplay(getQuickActionMode())));
        
        return new ItemBuilder(Material.BELL)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.broadcast_message_name", false))
            .setLore(lore)
            .build();
    }

    /**
     * Creates display for a CHAT_MESSAGE type reward - shows a book with message info.
     */
    private ItemStack createChatMessageRewardDisplay(Reward reward, String chanceLore, int rewardNumber) {
        String message = reward.getValue();
        
        // Truncate long messages for display
        String displayMessage = message.length() > 40 
            ? message.substring(0, 37) + "..." 
            : message;
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(plugin.getMessageManager().getMessage("gui.rewards.chat_message_label", false));
        lore.add(ChatColor.WHITE + displayMessage);
        
        // Show full message if truncated
        if (message.length() > 40) {
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "(Message truncated)");
        }
        
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "─────────────");
        lore.add(plugin.getMessageManager().getMessage("gui.rewards.reward_number", false,
            "%number%", String.valueOf(rewardNumber)));
        lore.add(chanceLore);
        
        lore.add("");
        lore.add(plugin.getMessageManager().getMessage("gui.rewards.click_to_edit", false,
            "%action%", getQuickActionActionDisplay(getQuickActionMode())));
        
        return new ItemBuilder(Material.WRITABLE_BOOK)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.rewards.chat_message_name", false))
            .setLore(lore)
            .build();
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

            addReward(new Reward(RewardType.COMMAND, 100.0, null, null, command));
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.added"));
            open();
        });
    }

    /**
     * Updates the chance of a reward at the specified index.
     * Called from RewardActionMenu.
     */
    public void updateRewardChance(int rewardIndex, double newChance) {
        if (rewardIndex < 0 || rewardIndex >= rewards.size()) return;
        
        Reward oldReward = rewards.get(rewardIndex);
        Reward newReward = new Reward(oldReward.getType(), newChance,oldReward.getMessage(),oldReward.getBroadcast(), oldReward.getValue());
        rewards.set(rewardIndex, newReward);
        saveRewards();
    }

    public void updateRewardMessage(int rewardIndex, String message) {
        if (rewardIndex < 0 || rewardIndex >= rewards.size()) return;

        Reward oldReward = rewards.get(rewardIndex);
        Reward newReward = new Reward(oldReward.getType(), oldReward.getChance(), message, oldReward.getBroadcast(), oldReward.getValue());
        rewards.set(rewardIndex, newReward);
        saveRewards();
    }

    public void updateRewardBroadcast(int rewardIndex, String broadcast) {
        if (rewardIndex < 0 || rewardIndex >= rewards.size()) return;

        Reward oldReward = rewards.get(rewardIndex);
        Reward newReward = new Reward(oldReward.getType(), oldReward.getChance(), oldReward.getMessage(), broadcast, oldReward.getValue());
        rewards.set(rewardIndex, newReward);
        saveRewards();
    }

    public void updateRewardValue(int rewardIndex, String value) {
        if (rewardIndex < 0 || rewardIndex >= rewards.size()) return;

        Reward oldReward = rewards.get(rewardIndex);
        Reward newReward = new Reward(oldReward.getType(), oldReward.getChance(), oldReward.getMessage(), oldReward.getBroadcast(), value);
        rewards.set(rewardIndex, newReward);
        saveRewards();
    }

    /**
     * Deletes a reward at the specified index.
     * Public for access from RewardActionMenu.
     */
    public void deleteReward(int rewardIndex) {
        if (rewardIndex < 0 || rewardIndex >= rewards.size()) return;
        
        rewards.remove(rewardIndex);
        saveRewards();
    }

    /**
     * Adds a new reward and saves to the holder.
     */
    public void addReward(Reward reward) {
        rewards.add(reward);
        saveRewards();
    }

    /**
     * Saves the current rewards list to the holder.
     */
    private void saveRewards() {
        holder.saveRewards(new ArrayList<>(rewards));
    }

    /**
     * Formats the chance value for display (removes unnecessary decimals).
     */
    private String formatChance(double chance) {
        if (chance == (int) chance) {
            return String.valueOf((int) chance);
        }
        return String.format("%.1f", chance);
    }
}
