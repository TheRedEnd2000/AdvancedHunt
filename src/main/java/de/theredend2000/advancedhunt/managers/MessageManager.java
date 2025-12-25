package de.theredend2000.advancedHunt.managers;

import de.theredend2000.advancedHunt.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageManager {

    private final Main plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private final Map<String, String> messageCache = new HashMap<>();
    private final Map<String, List<String>> messageListCache = new HashMap<>();

    public MessageManager(Main plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        String lang = plugin.getConfig().getString("language", "en");
        messagesFile = new File(plugin.getDataFolder(), "messages/messages_" + lang + ".yml");

        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            String resourcePath = "messages/messages_" + lang + ".yml";
            InputStream resource = plugin.getResource(resourcePath);
            
            if (resource != null) {
                plugin.saveResource(resourcePath, false);
            } else {
                // Fallback to en if the requested language resource doesn't exist
                if (!lang.equals("en")) {
                    plugin.getLogger().warning("Language file for " + lang + " not found. Falling back to English.");
                    messagesFile = new File(plugin.getDataFolder(), "messages/messages_en.yml");
                    if (!messagesFile.exists()) {
                         plugin.saveResource("messages/messages_en.yml", false);
                    }
                } else {
                     // If en is requested but not found (shouldn't happen if built correctly)
                     plugin.saveResource("messages/messages_en.yml", false);
                }
            }
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        messageCache.clear();
        messageListCache.clear();
    }

    public String getMessage(String key) {
        if (messageCache.containsKey(key)) {
            return messageCache.get(key);
        }

        String message = messagesConfig.getString(key);
        if (message == null) {
            return "Missing message: " + key;
        }
        
        String prefix = messagesConfig.getString("prefix", "&6AdvancedHunt &8>> &7");
        message = message.replace("%prefix%", prefix);
        message = ChatColor.translateAlternateColorCodes('&', message);
        
        messageCache.put(key, message);
        return message;
    }

    public String getMessage(String key, String... placeholders) {
        String message = getMessage(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return message;
    }

    /**
     * Gets a message without applying the prefix replacement.
     * Useful for GUI titles, item names, and lore where prefix is not needed.
     * 
     * @param key The message key
     * @param applyPrefix Whether to apply the prefix placeholder replacement
     * @return The formatted message
     */
    public String getMessage(String key, boolean applyPrefix) {
        String message = messagesConfig.getString(key);
        if (message == null) {
            return "Missing message: " + key;
        }
        
        if (applyPrefix) {
            String prefix = messagesConfig.getString("prefix", "&6AdvancedHunt &8>> &7");
            message = message.replace("%prefix%", prefix);
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Gets a message without prefix, with placeholder support.
     * 
     * @param key The message key
     * @param applyPrefix Whether to apply the prefix placeholder replacement
     * @param placeholders Key-value pairs for placeholder replacement
     * @return The formatted message
     */
    public String getMessage(String key, boolean applyPrefix, String... placeholders) {
        String message = getMessage(key, applyPrefix);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return message;
    }

    public List<String> getMessageList(String key) {
        if (messageListCache.containsKey(key)) {
            return messageListCache.get(key);
        }

        List<String> messages = messagesConfig.getStringList(key);
        if (messages == null || messages.isEmpty()) {
            // Fallback to single string if list is empty but the key exists as string
            String single = messagesConfig.getString(key);
            if (single != null) {
                messages = new ArrayList<>();
                messages.add(single);
            } else {
                messages = new ArrayList<>();
                messages.add("Missing message list: " + key);
                return messages;
            }
        }

        String prefix = messagesConfig.getString("prefix", "&6AdvancedHunt &8>> &7");
        
        List<String> processedMessages = messages.stream()
                .map(msg -> msg.replace("%prefix%", prefix))
                .map(msg -> ChatColor.translateAlternateColorCodes('&', msg))
                .collect(Collectors.toList());
        
        messageListCache.put(key, processedMessages);
        return processedMessages;
    }

    public List<String> getMessageList(String key, String... placeholders) {
        List<String> messages = getMessageList(key);
        List<String> replacedMessages = new ArrayList<>();
        
        for (String msg : messages) {
            for (int i = 0; i < placeholders.length; i += 2) {
                if (i + 1 < placeholders.length) {
                    msg = msg.replace(placeholders[i], placeholders[i + 1]);
                }
            }
            replacedMessages.add(msg);
        }
        return replacedMessages;
    }

    /**
     * Gets a message list without applying the prefix replacement.
     * Useful for GUI lore where prefix is not needed.
     * 
     * @param key The message key
     * @param applyPrefix Whether to apply the prefix placeholder replacement
     * @return The formatted message list
     */
    public List<String> getMessageList(String key, boolean applyPrefix) {
        List<String> messages = messagesConfig.getStringList(key);
        if (messages == null || messages.isEmpty()) {
            String single = messagesConfig.getString(key);
            if (single != null) {
                messages = new ArrayList<>();
                messages.add(single);
            } else {
                messages = new ArrayList<>();
                messages.add("Missing message list: " + key);
                return messages;
            }
        }

        String prefix = messagesConfig.getString("prefix", "&6AdvancedHunt &8>> &7");
        
        return messages.stream()
                .map(msg -> applyPrefix ? msg.replace("%prefix%", prefix) : msg)
                .map(msg -> ChatColor.translateAlternateColorCodes('&', msg))
                .collect(Collectors.toList());
    }

    /**
     * Gets a message list without a prefix, with placeholder support.
     * 
     * @param key The message key
     * @param applyPrefix Whether to apply the prefix placeholder replacement
     * @param placeholders Key-value pairs for placeholder replacement
     * @return The formatted message list
     */
    public List<String> getMessageList(String key, boolean applyPrefix, String... placeholders) {
        List<String> messages = getMessageList(key, applyPrefix);
        List<String> replacedMessages = new ArrayList<>();
        
        for (String msg : messages) {
            for (int i = 0; i < placeholders.length; i += 2) {
                if (i + 1 < placeholders.length) {
                    msg = msg.replace(placeholders[i], placeholders[i + 1]);
                }
            }
            replacedMessages.add(msg);
        }
        return replacedMessages;
    }
    
    public void reloadMessages() {
        loadMessages();
    }

    /**
     * Formats a ZonedDateTime for display to players
     * @param dateTime the time to format
     * @return formatted string
     */
    public String formatDateTime(ZonedDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        return dateTime.format(formatter);
    }
}
