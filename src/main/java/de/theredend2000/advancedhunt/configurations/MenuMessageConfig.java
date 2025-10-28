package de.theredend2000.advancedhunt.configurations;

import org.bukkit.plugin.java.JavaPlugin;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

public class MenuMessageConfig extends Configuration {
    private static TreeMap<Double, ConfigUpgrader> upgraders = new TreeMap<>();

    private static volatile MenuMessageConfig instance;

    public MenuMessageConfig(JavaPlugin plugin, String configName) {
        super(plugin, MessageFormat.format("menus/menu-{0}.yml", configName));
    }

    @Override
    public TreeMap<Double, ConfigUpgrader> getUpgrader() {
        return upgraders;
    }

    @Override
    public void registerUpgrader() {
        upgraders.put(1.3, (oldConfig, newConfig) -> {
            newConfig.set("settings_sound_volume.lore", newConfig.getDefaults().getStringList("settings_sound_volume.lore"));
            newConfig.set("collection_editor_reset.displayname", newConfig.getDefaults().getString("collection_editor_reset.displayname"));
        });
        upgraders.put(1.1, (oldConfig, newConfig) -> {
            List<ConfigMigration.ReplacementEntry> keyReplacements = Arrays.asList(
                    new ConfigMigration.ReplacementEntry("egg", "treasure", false, true)
            );


            List<ConfigMigration.ReplacementEntry> valueReplacements = Arrays.asList(
                    new ConfigMigration.ReplacementEntry("%EGG", "%TREASURE", false, false),
                    new ConfigMigration.ReplacementEntry("%MAX_EGGS%", "%MAX_TREASURES%", false, false)
            );

            ConfigMigration migration = new ConfigMigration(true, keyReplacements, valueReplacements);
            migration.standardUpgrade(oldConfig, newConfig);
        });
    }

    public String getMenuMessage(String menuMessages) {
        return getConfig().getString(menuMessages);
    }

    public List<String> getMenuMessageList(String path) {
        return config.getStringList(path);
    }
}
