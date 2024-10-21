package de.theredend2000.advancedhunt.configurations;

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
    private final boolean hasTemplates;

    public abstract TreeMap<Double, ConfigUpgrader> getUpgrader();
    public abstract void registerUpgrader();

    public MultiFileConfiguration(JavaPlugin plugin, String configFolder, String fileExtension) {
        this(plugin, configFolder, fileExtension, -1);
    }

    public MultiFileConfiguration(JavaPlugin plugin, String configFolder, String fileExtension, double latestVersion) {
        this.plugin = plugin;
        this.configFolder = configFolder;
        this.fileExtension = fileExtension.startsWith(".") ? fileExtension : "." + fileExtension;
        this.configs = new HashMap<>();
        this.configFiles = new HashMap<>();
        this.hasTemplates = !getResourcesInFolder(configFolder).isEmpty();
        this.latestVersion = latestVersion;

        if (!hasTemplates && latestVersion <= 0) {
            throw new IllegalArgumentException("Latest Version must be greater than 0 when no templates exist");
        }

        if (hasTemplates) {
            saveDefaultConfigs();
        }
        reloadConfigs();

        registerUpgrader();
        loadConfigs();
    }

    protected File getDataFolder() {
        return plugin.getDataFolder();
    }

    protected void loadConfigs() {
        for (String configName : configs.keySet()) {
            YamlConfiguration config = configs.get(configName);
            double currentVersion = config.getDouble("config-version", 0.0);
            double targetVersion = hasTemplates ? getTemplateVersion(configName) : latestVersion;

            if (currentVersion < targetVersion) {
                upgradeConfig(configName, currentVersion, targetVersion);
            }
        }
    }

    private double getTemplateVersion(String configName) {
        try (InputStream defaultStream = plugin.getResource(configFolder + "/" + configName)) {
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
                return defaultConfig.getDouble("config-version", 1.0);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to read template config version for " + configName, e);
        }
        return 1.0; // Default to 1.0 if we can't read the version for some reason
    }

    private void upgradeConfig(String configName, double currentVersion, double targetVersion) {
        YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFiles.get(configName));

        if (hasTemplates) {
            configFiles.get(configName).delete();
            saveDefaultConfig(configName);

            reloadConfig(configName);
        } else {
            configs.put(configName, new YamlConfiguration());
        }

        YamlConfiguration newConfig = configs.get(configName);
        standardUpgrade(oldConfig, newConfig);

        TreeMap<Double, ConfigUpgrader> upgraders = getUpgrader();
        if (upgraders != null) {
            for (Map.Entry<Double, ConfigUpgrader> entry : upgraders.tailMap(currentVersion, false).entrySet()) {
                    if (entry.getKey() > targetVersion) break;
                ConfigUpgrader upgrader = entry.getValue();
                if (upgrader != null) {
                    upgrader.upgrade(oldConfig, newConfig);
                }
            }
        }

        newConfig.set("config-version", targetVersion);
        saveConfig(configName);
    }

    private void standardUpgrade(YamlConfiguration oldConfig, YamlConfiguration newConfig) {
        Set<String> allKeys = oldConfig.getKeys(true);
        List<String> keyList = new ArrayList<>(allKeys);

        keyList.sort((key1, key2) -> Integer.compare(key2.split("\\.").length, key1.split("\\.").length));

        for (String key : keyList) {
            if (oldConfig.isSet(key) && !oldConfig.isConfigurationSection(key) &&
                    (!hasTemplates || newConfig.contains(key))) {
                newConfig.set(key, oldConfig.get(key));
            }
        }
    }

    public void reloadConfigs() {
        File folder = new File(plugin.getDataFolder(), configFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        configFiles = new HashMap<>();

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

        if (hasTemplates) {
            try (InputStream defaultStream = plugin.getResource(configFolder + "/" + configName)) {
                if (defaultStream != null) {
                    YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
                    config.setDefaults(defaultConfig);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load default config for " + configName, e);
            }
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

    public FileConfiguration getConfig(String configName) {
        if (!configName.endsWith(fileExtension)) {
            configName += fileExtension;
        }
        if (!configs.containsKey(configName)) {
            reloadConfig(configName);
        }
        return configs.get(configName);
    }

    public Boolean containsConfig(String configName) {
        if (!configName.endsWith(fileExtension)) {
            configName += fileExtension;
        }
        return configs.containsKey(configName);
    }

    public void set(String configName, String path, Object value) {
        getConfig(configName).set(path, value);
    }

    public void saveConfig(String configName) {
        if (!configName.endsWith(fileExtension)) {
            configName += fileExtension;
        }

        try {
            getConfig(configName).save(configFiles.get(configName));
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFiles.get(configName), ex);
        }
    }

    public boolean deleteConfig(String configName) {
        if (!configName.endsWith(fileExtension)) {
            configName += fileExtension;
        }

        if (!configs.containsKey(configName) || !configFiles.containsKey(configName)) {
            plugin.getLogger().warning("Attempted to delete non-existent config: " + configName);
            return false;
        }

        File configFile = configFiles.get(configName);

        configs.remove(configName);
        configFiles.remove(configName);

        boolean deleted = configFile.delete();

        if (deleted) {
            plugin.getLogger().info("Successfully deleted config: " + configName);
        } else {
            plugin.getLogger().warning("Failed to delete config file: " + configName);
        }

        return deleted;
    }

    public void unloadConfig(String configName) {
        if (!configName.endsWith(fileExtension)) {
            configName += fileExtension;
        }

        configs.remove(configName);
        configFiles.remove(configName);
        plugin.getLogger().info("Unloaded config from memory: " + configName);
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
