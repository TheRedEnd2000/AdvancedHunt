package de.theredend2000.advancedegghunt.configurations;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public abstract class Configuration {
    protected final JavaPlugin plugin;
    protected YamlConfiguration config;
    protected File configFile;
    protected final String configName;
    private final double latestVersion;
    private final boolean template;

    public abstract TreeMap<Double, ConfigUpgrader> getUpgrader();
    public abstract void registerUpgrader();

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

        if (template) {
            saveDefaultConfig();
        }
        reloadConfig();

        registerUpgrader();
        loadConfig();
    }

    protected void loadConfig() {
        double currentVersion = config.getDouble("config-version", 0.0);
        double effectiveLatestVersion = latestVersion;

        if (effectiveLatestVersion == -1d) {
            effectiveLatestVersion = getDefaultConfigVersion();
        }

        if (currentVersion < effectiveLatestVersion || currentVersion > effectiveLatestVersion) {
            upgradeConfig(currentVersion, effectiveLatestVersion);
        }
    }

    private double getDefaultConfigVersion() {
        try (InputStream defaultStream = plugin.getResource(configName)) {
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
                return defaultConfig.getDouble("config-version", -1d);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to read default config version", e);
        }
        return -1d;
    }

    private void upgradeConfig(double currentVersion, double targetVersion) {
        YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);

        configFile.delete();
        if (template) {
            saveDefaultConfig();
        }
        reloadConfig();

        standardUpgrade(oldConfig, config);

        for (Map.Entry<Double, ConfigUpgrader> entry : getUpgrader().tailMap(currentVersion + 0.1, true).entrySet()) {
            ConfigUpgrader upgrader = entry.getValue();
            if (upgrader != null) {
                upgrader.upgrade(oldConfig, config);
            }
        }

        config.set("config-version", targetVersion);
        saveConfig();
    }

    private void standardUpgrade(YamlConfiguration oldConfig, YamlConfiguration newConfig) {
        Set<String> allKeys = oldConfig.getKeys(true);
        List<String> keyList = new ArrayList<>(allKeys);

        keyList.sort((key1, key2) -> Integer.compare(key2.split("\\.").length, key1.split("\\.").length));

        for (String key : keyList) {
            if (oldConfig.isSet(key) && !oldConfig.isConfigurationSection(key) && newConfig.contains(key)) {
                newConfig.set(key, oldConfig.get(key));
            }
        }
    }

    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), configName);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        try (InputStream defaultStream = plugin.getResource(configName)) {
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
                config.setDefaults(defaultConfig);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load default config", e);
        }
    }

    protected void saveDefaultConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), configName);
        }

        if (!configFile.exists()) {
            plugin.saveResource(configName, false);
        }
    }

    protected FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    protected void set(String path, Object value) {
        getConfig().set(path, value);
    }

    protected void saveConfig() {
        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
        }
    }

    public static void deleteDir(File file) {
        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File f : contents) {
                    if (!Files.isSymbolicLink(f.toPath())) {
                        deleteDir(f);
                    }
                }
            }
        }
        if (!file.delete()) {
            throw new RuntimeException("Failed to delete " + file);
        }
    }

    public interface ConfigUpgrader {
        void upgrade(YamlConfiguration oldConfig, YamlConfiguration newConfig);
    }
}