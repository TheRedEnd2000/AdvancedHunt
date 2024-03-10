package de.theredend2000.advancedegghunt.configurations;

import org.bukkit.plugin.java.JavaPlugin;

import java.text.MessageFormat;
import java.util.TreeMap;

public class EggConfig extends Configuration {
    private static TreeMap<Double, ConfigUpgrader> upgraders = new TreeMap<>();

    public EggConfig(JavaPlugin plugin, String configName) {
        super(plugin, MessageFormat.format("/eggs/{0}.yml", configName), false);
    }

    @Override
    public TreeMap<Double, ConfigUpgrader> getUpgrader() {
        return upgraders;
    }

    @Override
    public void registerUpgrader() {

    }
}
