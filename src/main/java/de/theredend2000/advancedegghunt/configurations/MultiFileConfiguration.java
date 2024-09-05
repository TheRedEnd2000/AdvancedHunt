package de.theredend2000.advancedegghunt.configurations;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public abstract class MultiFileConfiguration {
    protected final JavaPlugin plugin;
    protected Map<String, YamlConfiguration> configs;
    protected Map<String, File> configFiles;
    protected final String configFolder;
    protected final String fileExtension;
    private final double latestVersion;
    private final boolean template;

    public abstract Map<String, TreeMap<Double, ConfigUpgrader>> getUpgraders();
    public abstract void registerUpgraders();

    public MultiFileConfiguration(JavaPlugin plugin, String configFolder, String fileExtension) {
        this(plugin, configFolder, fileExtension, true, -1d);
    }

    public MultiFileConfiguration(JavaPlugin plugin, String configFolder, String fileExtension, double latestVersion) {
        this(plugin, configFolder, fileExtension, true, latestVersion);
    }

    public MultiFileConfiguration(JavaPlugin plugin, String configFolder, String fileExtension, boolean template) {
        this(plugin, configFolder, fileExtension, template, -1d);
    }

    public MultiFileConfiguration(JavaPlugin plugin, String configFolder, String fileExtension, boolean template, double latestVersion) {
        this.plugin = plugin;
        this.configFolder = configFolder;
        this.fileExtension = fileExtension.startsWith(".") ? fileExtension : "." + fileExtension;
        this.template = template;
        this.latestVersion = latestVersion;
        this.configs = new HashMap<>();
        this.configFiles = new HashMap<>();

        if (template) {
            saveDefaultConfigs();
        }
        reloadConfigs();

        registerUpgraders();
        loadConfigs();
    }

    protected void loadConfigs() {
        for (String configName : configs.keySet()) {
            YamlConfiguration config = configs.get(configName);
            double currentVersion = config.getDouble("config-version", 0.0);
            double effectiveLatestVersion = latestVersion;

            if (effectiveLatestVersion == -1d) {
                effectiveLatestVersion = getDefaultConfigVersion(configName);
            }

            if (currentVersion < effectiveLatestVersion || currentVersion > effectiveLatestVersion) {
                upgradeConfig(configName, currentVersion, effectiveLatestVersion);
            }
        }
    }

    private double getDefaultConfigVersion(String configName) {
        try (InputStream defaultStream = plugin.getResource(configFolder + "/" + configName)) {
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
                return defaultConfig.getDouble("config-version", -1d);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to read default config version for " + configName, e);
        }
        return -1d;
    }

    private void upgradeConfig(String configName, double currentVersion, double targetVersion) {
        YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFiles.get(configName));

        configFiles.get(configName).delete();
        if (template) {
            saveDefaultConfig(configName);
        }
        reloadConfig(configName);

        standardUpgrade(oldConfig, configs.get(configName));

        TreeMap<Double, ConfigUpgrader> upgraders = getUpgraders().get(configName);
        if (upgraders != null) {
            for (Map.Entry<Double, ConfigUpgrader> entry : upgraders.tailMap(currentVersion + 0.1, true).entrySet()) {
                ConfigUpgrader upgrader = entry.getValue();
                if (upgrader != null) {
                    upgrader.upgrade(oldConfig, configs.get(configName));
                }
            }
        }

        configs.get(configName).set("config-version", targetVersion);
        saveConfig(configName);
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

    public void reloadConfigs() {
        File folder = new File(plugin.getDataFolder(), configFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        for (File file : folder.listFiles()) {
            if (file.isFile() && file.getName().endsWith(fileExtension)) {
                String configName = file.getName();
                reloadConfig(configName);
            }
        }
    }

    private void reloadConfig(String configName) {
        File configFile = new File(plugin.getDataFolder(), configFolder + "/" + configName);
        configFiles.put(configName, configFile);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        configs.put(configName, config);

        try (InputStream defaultStream = plugin.getResource(configFolder + "/" + configName)) {
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
                config.setDefaults(defaultConfig);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load default config for " + configName, e);
        }
    }

    protected void saveDefaultConfigs() {
        File folder = new File(plugin.getDataFolder(), configFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        Set<String> resourceNames = getResourcesInFolder(configFolder);
        for (String resource : resourceNames) {
            if (resource.endsWith(fileExtension)) {
                String configName = new File(resource).getName();
                saveDefaultConfig(configName);
            }
        }
    }

    private Set<String> getResourcesInFolder(String folderPath) {
        Set<String> resources = new HashSet<>();
        try {
            InputStream stream = plugin.getResource(folderPath);
            if (stream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.endsWith(fileExtension)) {
                        resources.add(folderPath + "/" + line);
                    }
                }
                reader.close();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to read resources in folder: " + folderPath, e);
        }
        return resources;
    }

    private void saveDefaultConfig(String configName) {
        if (configFiles.get(configName) == null) {
            configFiles.put(configName, new File(plugin.getDataFolder(), configFolder + "/" + configName));
        }

        if (!configFiles.get(configName).exists()) {
            plugin.saveResource(configFolder + "/" + configName, false);
        }
    }

    protected FileConfiguration getConfig(String configName) {
        if (!configName.endsWith(fileExtension)) {
            configName += fileExtension;
        }
        if (!configs.containsKey(configName)) {
            reloadConfig(configName);
        }
        return configs.get(configName);
    }

    protected void set(String configName, String path, Object value) {
        getConfig(configName).set(path, value);
    }

    protected void saveConfig(String configName) {
        if (!configName.endsWith(fileExtension)) {
            configName += fileExtension;
        }
        
        try {
            getConfig(configName).save(configFiles.get(configName));
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFiles.get(configName), ex);
        }
    }

    /**
     * Deletes a specific configuration file and removes it from the managed configurations.
     *
     * @param configName The name of the configuration file to delete (including .yml extension)
     * @return true if the config was successfully deleted, false otherwise
     */
    public boolean deleteConfig(String configName) {
        if (!configName.endsWith(fileExtension)) {
            configName += fileExtension;
        }

        if (!configs.containsKey(configName) || !configFiles.containsKey(configName)) {
            plugin.getLogger().warning("Attempted to delete non-existent config: " + configName);
            return false;
        }

        File configFile = configFiles.get(configName);

        // Remove the config from memory
        configs.remove(configName);
        configFiles.remove(configName);

        // Delete the file
        boolean deleted = configFile.delete();

        if (deleted) {
            plugin.getLogger().info("Successfully deleted config: " + configName);
        } else {
            plugin.getLogger().warning("Failed to delete config file: " + configName);
        }

        return deleted;
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