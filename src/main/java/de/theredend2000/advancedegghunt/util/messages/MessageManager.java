package de.theredend2000.advancedegghunt.util.messages;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.configurations.MessageConfig;
import de.theredend2000.advancedegghunt.util.HexColor;
import org.bukkit.ChatColor;

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
}