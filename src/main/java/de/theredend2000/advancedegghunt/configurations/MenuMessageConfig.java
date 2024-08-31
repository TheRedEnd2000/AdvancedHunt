package de.theredend2000.advancedegghunt.configurations;

import org.bukkit.plugin.java.JavaPlugin;

import java.text.MessageFormat;
import java.util.TreeMap;

public class MenuMessageConfig extends Configuration {
    private static TreeMap<Double, ConfigUpgrader> upgraders = new TreeMap<>();

    private static volatile MenuMessageConfig instance;

    public MenuMessageConfig(JavaPlugin plugin, String configName) {
        super(plugin, MessageFormat.format("menuMessages/messages-{0}.yml", configName));
    }

    @Override
    public TreeMap<Double, ConfigUpgrader> getUpgrader() {
        return upgraders;
    }

    @Override
    public void registerUpgrader() {

    }

    public String getMenuMessage(String menuMessages) {
        return getConfig().getString(menuMessages);
    }
}
