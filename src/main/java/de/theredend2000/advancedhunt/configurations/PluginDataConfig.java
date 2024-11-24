package de.theredend2000.advancedhunt.configurations;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Paths;
import java.util.TreeMap;

public class PluginDataConfig extends Configuration {
    private static TreeMap<Double, ConfigUpgrader> upgraders = new TreeMap<>();

    public PluginDataConfig(JavaPlugin plugin) {
        super(plugin, "plugin_data.yml", 1.1);
    }

    @Override
    public TreeMap<Double, ConfigUpgrader> getUpgrader() {
        return upgraders;
    }

    @Override
    public void registerUpgrader() {
        upgraders.put(1.1, (oldConfig, newConfig) -> {
            String updateDir = Bukkit.getUpdateFolderFile().getPath();

            newConfig.getConfigurationSection("paths.");

            for (String path : newConfig.getConfigurationSection("paths").getKeys(false)) {
                if (newConfig.getString("paths." + path).startsWith(updateDir)) {
                    newConfig.set("paths." + path, null);
                }
            }
        });
    }

    public void savePluginPath(String pluginName, String path) {
        set("paths." + pluginName.replaceAll("\\.","_"), path);
        saveConfig();
    }

    public String getStoredPluginPath(String pluginName) {
        return getConfig().getString("paths." + pluginName.replaceAll("\\.","_"));
    }
}
