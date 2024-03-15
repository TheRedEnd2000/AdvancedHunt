package de.theredend2000.advancedegghunt.configurations;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

public abstract class Configuration {
    protected JavaPlugin plugin;
    protected FileConfiguration config = null;
    protected File configFile = null;
    protected String configName;
    private boolean template;

    public abstract TreeMap<Double, ConfigUpgrader> getUpgrader();


    public Configuration(JavaPlugin plugin, String configName) {
        this(plugin, configName, true);
    }
    public Configuration(JavaPlugin plugin, String configName, boolean template) {
        this.plugin = plugin;
        this.configName = configName;
        this.template = template;

        if (template)
            this.saveDefaultConfig();
        reloadConfig();

        registerUpgrader();
        //loadConfig();
    }

    public abstract void registerUpgrader();

    public void loadConfig() {
        double currentVersion = config.getDouble("config-version", 0.0);
        if (getUpgrader().isEmpty()) return;
        double latestVersion = getUpgrader().lastKey();

        if (currentVersion < latestVersion) {
            upgradeConfig(currentVersion);
        }
    }

    private void upgradeConfig(double currentVersion) {
        YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);
        File backupFile = new File(plugin.getDataFolder(), configName + ".bak");

        try {
            oldConfig.save(backupFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        YamlConfiguration newConfig = new YamlConfiguration();
        newConfig.options().copyDefaults(true);

        // Run the standard upgrade first to copy all keys from the old config
        standardUpgrade(oldConfig, newConfig);

        // Loop through each version above the current version and run the upgrade method
        for (Map.Entry<Double, ConfigUpgrader> entry : getUpgrader().tailMap(currentVersion + 0.1, true).entrySet()) {
            double version = entry.getKey();
            ConfigUpgrader upgrader = entry.getValue();
            if (upgrader != null) {
                upgrader.upgrade(oldConfig, newConfig);
            }
        }

        try {
            newConfig.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        config = newConfig;
    }
    private void standardUpgrade(YamlConfiguration oldConfig, YamlConfiguration newConfig) {
        // Copy all keys from the old config to the new config
        for (String key : oldConfig.getKeys(true)) {
            newConfig.set(key, oldConfig.get(key));
        }
    }

    public void reloadConfig() {
        if (this.configFile == null)
            this.configFile = new File(this.plugin.getDataFolder(), this.configName);

        this.config = YamlConfiguration.loadConfiguration(configFile);

        InputStream defaultStream = this.plugin.getResource(configName);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            this.config.setDefaults(defaultConfig);
        }
    }

    protected FileConfiguration getConfig() {
        if (this.config == null)
            reloadConfig();

        return this.config;
    }

    protected void set(String path, Object value) {
        getConfig().set(path, value);
    }

    protected void saveConfig() {
        try {
            this.getConfig().save(this.configFile);
        } catch (IOException ex) {
            this.plugin.getLogger().log(Level.SEVERE, MessageFormat.format("Could not save config to {0}", this.configFile), ex);
        }
    }

    protected void saveDefaultConfig() {
        if (this.configFile == null)
            this.configFile = new File(this.plugin.getDataFolder(), configName);

        if (!configFile.exists()) {
            plugin.saveResource(this.configName, false);
        }
    }

    public static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (! Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        file.delete();
    }

    public interface ConfigUpgrader {
        void upgrade(YamlConfiguration oldConfig, YamlConfiguration NewConfig);
    }
}