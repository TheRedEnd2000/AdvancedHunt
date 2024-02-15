package de.theredend2000.advancedegghunt.util.messages;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.HexColor;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class MessageManager {

    private final Main plugin;
    private FileConfiguration messagesConfig;

    public MessageManager() {
        this.plugin = Main.getInstance();
        reloadMessages();
    }

    public void reloadMessages() {
        String lang = plugin.getConfig().getString("messages-lang");
        if (lang == null)
            lang = "en";

        File messagesFolder = new File(plugin.getDataFolder(), "messages");
        if (!messagesFolder.exists()) {
            messagesFolder.mkdirs();
        }

        File messagesFile = new File(messagesFolder, "messages-" + lang + ".yml");
        if (!messagesFile.exists()) {
            try {
                InputStream in = plugin.getResource("messageFiles/messages-" + lang + ".yml");
                if (in != null) {
                    Files.copy(in, messagesFile.toPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public boolean isUpToDate(){
        return messagesConfig.getDouble("version") == 2.4;
    }

    public String getMessage(MessageKey key) {
        String message = messagesConfig.getString(key.getPath());
        boolean prefix_enabled = Main.getInstance().getConfig().getBoolean("Settings.PluginPrefixEnabled");
        if (message != null) {
            if(key.name().equals("EGG_NEARBY")) return HexColor.color(message);
            if(prefix_enabled)
                message = HexColor.color(ChatColor.translateAlternateColorCodes('&', Main.PREFIX + message));
            else
                message = HexColor.color(ChatColor.translateAlternateColorCodes('&', message));
            return message;
        }
        return "Message not found: " + key.name();
    }
}