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
    protected YamlConfiguration config = null;
    protected File configFile = null;
    protected String configName;
    private double latestVersion;
    private boolean template;

    public abstract TreeMap<Double, ConfigUpgrader> getUpgrader();


    public Configuration(JavaPlugin plugin, String configName) {
        this(plugin, configName, true, -1d);
    }

    public Configuration(JavaPlugin plugin, String configName, double latestVersion) {
        this(plugin, configName, true, latestVersion);
    }

    public Configuration(JavaPlugin plugin, String configName, boolean template) {
        this(plugin, configName, template, -1d);
    }

    public Configuration(JavaPlugin plugin, String configName, boolean template, double latestVersion) {
        this.plugin = plugin;
        this.configName = configName;
        this.template = template;
        this.latestVersion = latestVersion;

        if (template)
            this.saveDefaultConfig();
        reloadConfig();

        registerUpgrader();
        loadConfig();
    }

    public abstract void registerUpgrader();

    protected void loadConfig() {
        Double currentVersion = config.getDouble("config-version", 0.0);
        if (latestVersion == -1d) {
            InputStream defaultStream = this.plugin.getResource(configName);
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
                latestVersion = defaultConfig.getDouble("config-version", -1d);
            }
        }

        if (currentVersion < latestVersion) {
            upgradeConfig(currentVersion);
        }
    }

    private void upgradeConfig(double currentVersion) {
        YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);
//        File backupFile = new File(plugin.getDataFolder(), configName + ".bak");
//
//        try {
//            oldConfig.save(backupFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } //TODO: See about reimplementing later

        configFile.delete();
        reloadConfig();

        // Run the standard upgrade first to copy all keys from the old config
        standardUpgrade(oldConfig, config);

        // Loop through each version above the current version and run the upgrade method
        for (Map.Entry<Double, ConfigUpgrader> entry : getUpgrader().tailMap(currentVersion + 0.1, true).entrySet()) {
            ConfigUpgrader upgrader = entry.getValue();
            if (upgrader != null) {
                upgrader.upgrade(oldConfig, config);
            }
        }

        config.set("config-version", currentVersion);

        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
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