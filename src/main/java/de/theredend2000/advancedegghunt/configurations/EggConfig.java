package de.theredend2000.advancedegghunt.configurations;

import org.bukkit.plugin.java.JavaPlugin;

import java.text.MessageFormat;

public class EggConfig extends Configuration {
    public EggConfig(JavaPlugin plugin, String configName) {
        super(plugin, MessageFormat.format("/eggs/{0}", configName));
    }
}
