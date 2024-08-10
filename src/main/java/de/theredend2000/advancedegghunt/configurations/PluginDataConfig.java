package de.theredend2000.advancedegghunt.configurations;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.TreeMap;

public class PluginDataConfig extends Configuration {
    public PluginDataConfig(JavaPlugin plugin) {
        super(plugin, "plugin_data.yml", false, 1.0);
    }

    @Override
    public TreeMap<Double, ConfigUpgrader> getUpgrader() {
        return new TreeMap<>();
    }

    @Override
    public void registerUpgrader() {
    }

    public void savePluginPath(String pluginName, String path) {
        set("paths." + pluginName, path);
        saveConfig();
    }

    public String getStoredPluginPath(String pluginName) {
        return getConfig().getString("paths." + pluginName);
    }
}
