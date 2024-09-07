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
    private final boolean hasTemplate;

    public abstract TreeMap<Double, ConfigUpgrader> getUpgrader();
    public abstract void registerUpgrader();

    public Configuration(JavaPlugin plugin, String configName, double latestVersion) {
        this.plugin = plugin;
        this.configName = configName;
        this.latestVersion = latestVersion;
        this.hasTemplate = plugin.getResource(configName) != null;

        if (latestVersion <= 0) {
            throw new IllegalArgumentException("Latest Version must be greater than 0");
        }

        this.configFile = new File(plugin.getDataFolder(), configName);
        registerUpgrader();
        loadConfig();
    }

    protected void loadConfig() {
        if (!configFile.exists()) {
            if (hasTemplate) {
                plugin.saveResource(configName, false);
            } else {
                try {
                    configFile.getParentFile().mkdirs();
                    configFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not create config file", e);
                }
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        double currentVersion = config.getDouble("config-version", 0.0);
        if (currentVersion < latestVersion) {
            upgradeConfig(currentVersion);
        }
    }

    private void upgradeConfig(double currentVersion) {
        YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);

        if (hasTemplate) {
            configFile.delete();
            plugin.saveResource(configName, false);
            config = YamlConfiguration.loadConfiguration(configFile);
        } else {
            config = new YamlConfiguration();
        }

        standardUpgrade(oldConfig, config);

        for (Map.Entry<Double, ConfigUpgrader> entry : getUpgrader().tailMap(currentVersion, false).entrySet()) {
            ConfigUpgrader upgrader = entry.getValue();
            if (upgrader != null) {
                upgrader.upgrade(oldConfig, config);
            }
        }

        config.set("config-version", latestVersion);
        saveConfig();
    }

    private void standardUpgrade(YamlConfiguration oldConfig, YamlConfiguration newConfig) {
        Set<String> allKeys = oldConfig.getKeys(true);
        List<String> keyList = new ArrayList<>(allKeys);

        keyList.sort((key1, key2) -> Integer.compare(key2.split("\\.").length, key1.split("\\.").length));

        for (String key : keyList) {
            if (oldConfig.isSet(key) && !oldConfig.isConfigurationSection(key) &&
                    (hasTemplate ? newConfig.contains(key) : true)) {
                newConfig.set(key, oldConfig.get(key));
            }
        }
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);

        if (hasTemplate) {
            try (InputStream defaultStream = plugin.getResource(configName)) {
                if (defaultStream != null) {
                    YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
                    config.setDefaults(defaultConfig);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load default config", e);
            }
        }
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    public void set(String path, Object value) {
        getConfig().set(path, value);
    }

    public void saveConfig() {
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