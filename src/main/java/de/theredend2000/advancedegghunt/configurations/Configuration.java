package de.theredend2000.advancedegghunt.configurations;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.logging.Level;

public abstract class Configuration {
    protected JavaPlugin plugin;
    protected FileConfiguration config = null;
    protected File configFile = null;
    protected String configName;
    private boolean template;

    public Configuration(JavaPlugin plugin, String configName) {
        this(plugin, configName, true);
    }
    public Configuration(JavaPlugin plugin, String configName, boolean template) {
        this.plugin = plugin;
        this.configName = configName;
        this.template = template;

        if (template)
            this.saveDefaultConfig();
        else
            reloadConfig();
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
}