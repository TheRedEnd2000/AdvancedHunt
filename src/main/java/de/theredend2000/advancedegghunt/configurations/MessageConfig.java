package de.theredend2000.advancedegghunt.configurations;

import org.bukkit.plugin.java.JavaPlugin;

import java.text.MessageFormat;
import java.util.TreeMap;

public class MessageConfig extends Configuration {
    private static TreeMap<Double, ConfigUpgrader> upgraders = new TreeMap<>();

    private static volatile MessageConfig instance;

    private MessageConfig(JavaPlugin plugin, String configName) {
        super(plugin, MessageFormat.format("/messages/messages-{0}.yml", configName));
    }

    @Override
    public TreeMap<Double, ConfigUpgrader> getUpgrader() {
        return upgraders;
    }

    @Override
    public void registerUpgrader() {

    }

    public static MessageConfig getInstance(JavaPlugin plugin, String configName) {
        if (instance == null) {
            synchronized (MessageConfig.class) {
                if (instance == null) {
                    instance = new MessageConfig(plugin, configName);
                }
            }
        }
        return instance;
    }

    public String getMessage(String message) {
        return getConfig().getString(message);
    }
}
