package de.theredend2000.advancedegghunt.util.messages;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.configurations.MessageConfig;
import de.theredend2000.advancedegghunt.util.HexColor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class MessageManager {

    private final Main plugin;
    private MessageConfig messageConfig;

    public MessageManager() {
        this.plugin = Main.getInstance();
        reloadMessages();
    }

    public void reloadMessages() {
        String lang = plugin.getPluginConfig().getLanguage();
        if (lang == null)
            lang = "en";

        messageConfig = new MessageConfig(plugin, lang);
        messageConfig.reloadConfig();
    }

    public String getMessage(MessageKey key) {
        String message = messageConfig.getMessage(key.getPath());
        boolean prefix_enabled = Main.getInstance().getPluginConfig().getPluginPrefixEnabled();
        if (message == null) {
            return "Message not found: " + key.name();
        }

        if(key.name().equals("EGG_NEARBY")) return HexColor.color(message);
        if(prefix_enabled)
            message = HexColor.color(ChatColor.translateAlternateColorCodes('&', Main.PREFIX + message));
        else
            message = HexColor.color(ChatColor.translateAlternateColorCodes('&', message));
        return message;
    }

    public void sendMessage(CommandSender player, MessageKey key) {
        player.sendMessage(this.getMessage(key));
    }

    public void sendMessage(CommandSender player, MessageKey key, String... replacements) {
        String[] messages = this.getMessage(key).split("\n");
        for (String message : messages) {
            for (int i = 0; i < replacements.length; i += 2) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
            player.sendMessage(message);
        }
    }
}
