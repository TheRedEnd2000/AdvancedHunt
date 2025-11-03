package de.theredend2000.advancedhunt.configurations;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.TreeMap;


public class MySQLConfig extends Configuration {

    private static volatile MySQLConfig instance;
    private static final TreeMap<Double, ConfigUpgrader> upgraders = new TreeMap<>();

    private MySQLConfig(JavaPlugin plugin) {
        super(plugin, "mysql.yml");
    }

    public static MySQLConfig getInstance(JavaPlugin plugin) {
        if (instance == null) {
            synchronized (MySQLConfig.class) {
                if (instance == null) {
                    instance = new MySQLConfig(plugin);
                }
            }
        }
        return instance;
    }

    @Override
    public TreeMap<Double, ConfigUpgrader> getUpgrader() {
        return upgraders;
    }

    @Override
    public void registerUpgrader() {

    }

    public String getMessage(String path) {
        return getConfig().getString(path);
    }

    public boolean isEnabled() {
        return getConfig().getBoolean("enabled");
    }

    public String getHost() {
        return getConfig().getString("mysql.host");
    }

    public int getPort() {
        return getConfig().getInt("mysql.port");
    }

    public String getDatabase() {
        return getConfig().getString("mysql.databaseName");
    }

    public String getUser() {
        return getConfig().getString("mysql.user");
    }

    public String getPassword() {
        return getConfig().getString("mysql.password");
    }
}

