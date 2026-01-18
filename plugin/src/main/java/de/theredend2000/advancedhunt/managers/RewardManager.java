package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.model.PlayerData;
import de.theredend2000.advancedhunt.model.Reward;
import de.theredend2000.advancedhunt.model.RewardType;
import de.theredend2000.advancedhunt.util.HexColor;
import de.theredend2000.advancedhunt.util.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Manages reward distribution to players, including various reward types
 * and placeholder replacement for messages.
 */
public class RewardManager {

    private final Main plugin;
    private final Random random = new Random();

    // Cache für deserialisierte Items um wiederholte Deserialisierung zu vermeiden
    private final Map<String, ItemStack> itemCache = new ConcurrentHashMap<>();

    // Cache compiled command blacklist patterns to avoid recompiling on every reward.
    private volatile List<String> cachedCommandBlacklist = Collections.emptyList();
    private volatile List<Pattern> cachedCommandBlacklistPatterns = Collections.emptyList();

    public RewardManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Gives rewards to a player with collection context for placeholders.
     *
     * @param player The player receiving rewards
     * @param rewards The list of rewards to give
     * @param collection The collection context (can be null)
     */
    public void giveRewards(Player player, List<Reward> rewards, Collection collection) {
        if (rewards == null || rewards.isEmpty()) return;

        // Pre-calculate collection statistics once for all rewards
        CollectionStats stats = calculateCollectionStats(player, collection);

        for (Reward reward : rewards) {
            // Skip reward based on chance
            if (random.nextDouble() * 100 > reward.getChance()) continue;

            // Process the reward and get item name if applicable
            String itemName = processReward(player, reward, stats);

            // Send custom messages for COMMAND and ITEM rewards
            sendCustomMessages(player, reward, stats, itemName);
        }
    }

    /**
     * Processes a single reward based on its type.
     *
     * @return The item display name if it's an ITEM reward, null otherwise
     */
    private String processReward(Player player, Reward reward, CollectionStats stats) {
        switch (reward.getType()) {
            case COMMAND:
                executeCommand(player, reward, stats);
                return null;

            case ITEM:
                return giveItem(player, reward);

            case CHAT_MESSAGE:
                sendChatMessage(player, reward, stats);
                return null;

            case CHAT_MESSAGE_BROADCAST:
                broadcastMessage(player, reward, stats);
                return null;

            default:
                plugin.getLogger().warning("Unknown reward type: " + reward.getType());
                return null;
        }
    }

    /**
     * Executes a command reward with placeholder replacement.
     */
    private void executeCommand(Player player, Reward reward, CollectionStats stats) {
        String cmd = replacePlaceholders(reward.getValue(), player.getName(), stats, 0, null);

        if (plugin.getConfig().getBoolean("rewards.block-execution", true) && isCommandBlacklisted(cmd)) {
            plugin.getLogger().warning("Blocked execution of blacklisted command reward: " + cmd);
            return;
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    /**
     * Gives an item reward to the player.
     * Uses caching to avoid repeated deserialization of complex items.
     *
     * @return The display name of the item
     */
    private String giveItem(Player player, Reward reward) {
        ItemStack cachedItem = itemCache.computeIfAbsent(
                reward.getValue(),
                value -> {
                    ItemStack item = ItemSerializer.deserialize(value);
                    if (item == null) {
                        plugin.getLogger().warning("Failed to deserialize item reward: " + value);
                    }
                    return item;
                }
        );

        if (cachedItem == null) return null;
        ItemStack item = cachedItem.clone();

        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);

        if (!leftover.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.instance_dropped"));
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        return getItemDisplayName(cachedItem);
    }

    /**
     * Sends a chat message reward to the player.
     */
    private void sendChatMessage(Player player, Reward reward, CollectionStats stats) {
        String message = replacePlaceholders(
                reward.getValue(),
                player.getName(),
                stats,
                reward.getChance(),
                null // No item for chat messages
        );
        player.sendMessage(HexColor.color(message, '&'));
    }

    /**
     * Broadcasts a message reward to all players.
     */
    private void broadcastMessage(Player player, Reward reward, CollectionStats stats) {
        String message = replacePlaceholders(
                reward.getValue(),
                player.getName(),
                stats,
                reward.getChance(),
                null
        );
        Bukkit.broadcastMessage(HexColor.color(message, '&'));
    }

    /**
     * Sends custom message and broadcast for COMMAND and ITEM rewards.
     */
    private void sendCustomMessages(Player player, Reward reward, CollectionStats stats, String itemName) {
        // Only COMMAND and ITEM rewards support custom messages/broadcasts
        if (reward.getType() != RewardType.COMMAND && reward.getType() != RewardType.ITEM) {
            return;
        }

        // Only allow %item% placeholder for ITEM rewards
        String finalItemName = (reward.getType() == RewardType.ITEM) ? itemName : null;

        // Send custom message if configured
        if (reward.getMessage() != null && !reward.getMessage().isEmpty()) {
            String message = replacePlaceholders(
                    reward.getMessage(),
                    player.getName(),
                    stats,
                    reward.getChance(),
                    finalItemName
            );
            player.sendMessage(HexColor.color(message, '&'));
        }

        // Send custom broadcast if configured
        if (reward.getBroadcast() != null && !reward.getBroadcast().isEmpty()) {
            String broadcast = replacePlaceholders(
                    reward.getBroadcast(),
                    player.getName(),
                    stats,
                    reward.getChance(),
                    finalItemName
            );
            Bukkit.broadcastMessage(HexColor.color(broadcast, '&'));
        }
    }

    /**
     * Calculates collection statistics for placeholder replacement.
     */
    private CollectionStats calculateCollectionStats(Player player, Collection collection) {
        if (collection == null) {
            return new CollectionStats(0, 0, 0);
        }

        try {
            int maxTreasures = plugin.getTreasureManager().getTreasureCoresInCollection(collection.getId()).size();
            PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());

            int foundTreasures = plugin.getTreasureManager()
                    .countFoundInCollection(playerData.getFoundTreasures(), collection.getId());

            int remainingTreasures = Math.max(0, maxTreasures - foundTreasures);

            return new CollectionStats(foundTreasures, maxTreasures, remainingTreasures);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to calculate collection stats: " + e.getMessage());
            return new CollectionStats(0, 0, 0);
        }
    }

    private List<Pattern> getCommandBlacklistPatterns() {
        List<String> blacklist = plugin.getConfig().getStringList("rewards.command-blacklist");
        if (blacklist == null || blacklist.isEmpty()) {
            cachedCommandBlacklist = Collections.emptyList();
            cachedCommandBlacklistPatterns = Collections.emptyList();
            return cachedCommandBlacklistPatterns;
        }

        // Fast path: unchanged config.
        if (blacklist.equals(cachedCommandBlacklist)) {
            return cachedCommandBlacklistPatterns;
        }

        List<Pattern> compiled = new ArrayList<>(blacklist.size());
        for (String regex : blacklist) {
            if (regex == null || regex.trim().isEmpty()) continue;
            try {
                compiled.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid regex pattern in command-blacklist: " + regex);
            }
        }

        cachedCommandBlacklist = Collections.unmodifiableList(new ArrayList<>(blacklist));
        cachedCommandBlacklistPatterns = Collections.unmodifiableList(new ArrayList<>(compiled));
        return cachedCommandBlacklistPatterns;
    }

    /**
     * Replaces all placeholders in a message string.
     */
    private String replacePlaceholders(String message, String playerName, CollectionStats stats,
                                       double chance, String itemName) {
        String prefix = plugin.getConfig().getString("prefix", "[AdvancedHunt]");
        return message
                .replace("%player%", playerName)
                .replace("%prefix%", prefix)
                .replace("%found_treasures%", String.valueOf(stats.foundTreasures))
                .replace("%max_treasures%", String.valueOf(stats.maxTreasures))
                .replace("%remaining_treasures%", String.valueOf(stats.remainingTreasures))
                .replace("%chance%", formatChance(chance))
                .replace("%item%", itemName != null ? itemName : "")
                .replace("\\n", System.lineSeparator());
    }

    /**
     * Gets the display name of an item, falling back to formatted material name.
     */
    private String getItemDisplayName(ItemStack item) {
        if (item == null) return "Unknown Item";

        // Check for custom display name
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return ChatColor.stripColor(meta.getDisplayName());
            }
        }

        // Format material name: DIAMOND_SWORD -> Diamond sword
        String materialName = item.getType().name()
                .toLowerCase()
                .replace("_", " ");

        // Capitalize first letter
        return Character.toUpperCase(materialName.charAt(0)) + materialName.substring(1);
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

    /**
     * Checks if a command matches any of the regex patterns in the blacklist.
     *
     * @param command The command to check
     * @return true if the command is blacklisted, false otherwise
     */
    public boolean isCommandBlacklisted(String command) {
        List<Pattern> patterns = getCommandBlacklistPatterns();
        if (patterns.isEmpty()) return false;

        // Remove leading slash if present for consistent matching
        String checkCmd = command.startsWith("/") ? command.substring(1) : command;

        for (Pattern pattern : patterns) {
            if (pattern.matcher(checkCmd).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears the item cache. Useful for reloading or memory management.
     */
    public void clearItemCache() {
        itemCache.clear();
        plugin.getLogger().info("Item reward cache cleared (" + itemCache.size() + " items)");
    }

    /**
     * Gets the current size of the item cache.
     */
    public int getCacheSize() {
        return itemCache.size();
    }

    /**
     * Inner class to hold collection statistics for placeholder replacement.
     */
    private static class CollectionStats {
        final int foundTreasures;
        final int maxTreasures;
        final int remainingTreasures;

        CollectionStats(int foundTreasures, int maxTreasures, int remainingTreasures) {
            this.foundTreasures = foundTreasures;
            this.maxTreasures = maxTreasures;
            this.remainingTreasures = remainingTreasures;
        }
    }
}