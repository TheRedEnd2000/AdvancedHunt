package de.theredend2000.advancedegghunt.util.messages;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.configurations.MessageConfig;
import de.theredend2000.advancedegghunt.util.HexColor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

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
        for(String blacklisted :  blacklistKeys())
            if(key.name().equals(blacklisted)) return HexColor.color(message);
        if(prefix_enabled)
            message = HexColor.color(ChatColor.translateAlternateColorCodes('&', Main.PREFIX + message));
        else
            message = HexColor.color(ChatColor.translateAlternateColorCodes('&', message));
        return message.replaceAll("%PLUGIN_COMMAND%",plugin.getPluginConfig().getCommandFirstEntry()).replaceAll("%PLUGIN_NAME_S%",plugin.getPluginConfig().getPluginNameSingular()).replaceAll("%PLUGIN_NAME_P%",plugin.getPluginConfig().getPluginNamePlural());
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

    private List<String> blacklistKeys(){
        ArrayList<String> blacklisted = new ArrayList<>();
        blacklisted.add("EGG_NEARBY");
        blacklisted.add("REQUIREMENTS_NAME_HOUR");
        blacklisted.add("REQUIREMENTS_NAME_DATE");
        blacklisted.add("REQUIREMENTS_NAME_WEEKDAY");
        blacklisted.add("REQUIREMENTS_NAME_MONTH");
        blacklisted.add("REQUIREMENTS_NAME_YEAR");
        blacklisted.add("REQUIREMENTS_NAME_SEASON");
        blacklisted.add("DAY_MONDAY");
        blacklisted.add("DAY_TUESDAY");
        blacklisted.add("DAY_WEDNESDAY");
        blacklisted.add("DAY_THURSDAY");
        blacklisted.add("DAY_FRIDAY");
        blacklisted.add("DAY_SATURDAY");
        blacklisted.add("DAY_SUNDAY");
        blacklisted.add("REQUIREMENTS_SEASON_WINTER");
        blacklisted.add("REQUIREMENTS_SEASON_SUMMER");
        blacklisted.add("REQUIREMENTS_SEASON_FALL");
        blacklisted.add("REQUIREMENTS_SEASON_SPRING");
        blacklisted.add("MONTH_JANUARY");
        blacklisted.add("MONTH_FEBRUARY");
        blacklisted.add("MONTH_MARCH");
        blacklisted.add("MONTH_APRIL");
        blacklisted.add("MONTH_MAY");
        blacklisted.add("MONTH_JUNE");
        blacklisted.add("MONTH_JULY");
        blacklisted.add("MONTH_AUGUST");
        blacklisted.add("MONTH_SEPTEMBER");
        blacklisted.add("MONTH_OCTOBER");
        blacklisted.add("MONTH_NOVEMBER");
        blacklisted.add("MONTH_DECEMBER");
        blacklisted.add("HOUR_FORMAT");
        blacklisted.add("REQUIREMENTS_MORE");
        blacklisted.add("REQUIREMENTS_CLICK_TO_CHANGE");
        blacklisted.add("PRESET_IS_DEFAULT_1");
        blacklisted.add("PRESET_IS_DEFAULT_2");
        return blacklisted;
    }
}
