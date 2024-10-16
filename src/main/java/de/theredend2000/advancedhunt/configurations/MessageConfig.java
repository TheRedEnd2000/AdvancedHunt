package de.theredend2000.advancedhunt.configurations;

import org.bukkit.plugin.java.JavaPlugin;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

public class MessageConfig extends Configuration {
    private static TreeMap<Double, ConfigUpgrader> upgraders = new TreeMap<>();

    private static volatile MessageConfig instance;

    public MessageConfig(JavaPlugin plugin, String configName) {
        super(plugin, MessageFormat.format("messages/messages-{0}.yml", configName));
    }

    @Override
    public TreeMap<Double, ConfigUpgrader> getUpgrader() {
        return upgraders;
    }

    @Override
    public void registerUpgrader() {
        upgraders.put(2.7, (oldConfig, newConfig) -> {
            List<ConfigMigration.ReplacementEntry> keyReplacements = Arrays.asList(
                    new ConfigMigration.ReplacementEntry("^(?<=.*)egg(s?)(?=.*:)", "treasure$1", true, true),
                    new ConfigMigration.ReplacementEntry("(?<=.*)egg(s?)(?=.*)", "treasure$1", true, true)
            );

            List<ConfigMigration.ReplacementEntry> valueReplacements = Arrays.asList(
                    new ConfigMigration.ReplacementEntry("AdvancedEggHunt", "AdvancedHunt", false, false),
                    new ConfigMigration.ReplacementEntry("%EGG", "%TREASURE", false, false),
                    new ConfigMigration.ReplacementEntry("%MAX_EGGS%", "%MAX_TREASURES%", false, false),
                    new ConfigMigration.ReplacementEntry("placeEggs", "place", false, false),
                    new ConfigMigration.ReplacementEntry("/egghunt", "/%PLUGIN_COMMAND%", false, false),
                    new ConfigMigration.ReplacementEntry("(?<=^.*)\\begg(?!s?.yml)", "treasure", true, false)
            );

            ConfigMigration migration = new ConfigMigration(true, keyReplacements, valueReplacements);
            migration.standardUpgrade(oldConfig, newConfig);

            newConfig.set("help-message", newConfig.getDefaults().getString("help-message"));
        });
    }

    public String getMessage(String message) {
        return getConfig().getString(message);
    }
}
