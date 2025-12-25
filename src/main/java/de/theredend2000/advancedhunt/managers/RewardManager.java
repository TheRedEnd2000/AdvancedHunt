package de.theredend2000.advancedHunt.managers;

import de.theredend2000.advancedHunt.Main;
import de.theredend2000.advancedHunt.model.Reward;
import de.theredend2000.advancedHunt.model.RewardType;
import de.theredend2000.advancedHunt.util.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

public class RewardManager {

    private final Main plugin;
    private final Random random = new Random();

    public RewardManager(Main plugin) {
        this.plugin = plugin;
    }

    public void giveRewards(Player player, List<Reward> rewards) {
        if (rewards == null) return;
        for (Reward reward : rewards) {
            if (random.nextDouble() * 100 > reward.getChance()) continue;

            if (reward.getType() == RewardType.COMMAND) {
                String cmd = reward.getValue().replace("%player%", player.getName());
                
                if (plugin.getConfig().getBoolean("rewards.block-execution", true) && isCommandBlacklisted(cmd)) {
                    plugin.getLogger().warning("Blocked execution of blacklisted command reward: " + cmd);
                    continue;
                }
                
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } else if (reward.getType() == RewardType.ITEM) {
                ItemStack item = ItemSerializer.deserialize(reward.getValue());
                if (item != null) {
                    player.getInventory().addItem(item);
                }
            } else if (reward.getType() == RewardType.CHAT_MESSAGE) {
                String message = reward.getValue().replace("%player%", player.getName());
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            } else if (reward.getType() == RewardType.CHAT_MESSAGE_BROADCAST) {
                String message = reward.getValue().replace("%player%", player.getName());
                String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);
                Bukkit.broadcastMessage(formattedMessage);
            }
        }
    }

    /**
     * Checks if a command matches any of the regex patterns in the blacklist.
     * 
     * @param command The command to check
     * @return true if the command is blacklisted, false otherwise
     */
    public boolean isCommandBlacklisted(String command) {
        List<String> blacklist = plugin.getConfig().getStringList("rewards.command-blacklist");
        if (blacklist.isEmpty()) return false;

        // Remove leading slash if present for consistent matching
        String checkCmd = command.startsWith("/") ? command.substring(1) : command;

        for (String regex : blacklist) {
            try {
                Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                if (pattern.matcher(checkCmd).find()) {
                    return true;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid regex pattern in command-blacklist: " + regex);
            }
        }
        return false;
    }
}
