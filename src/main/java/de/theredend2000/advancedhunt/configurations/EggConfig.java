package de.theredend2000.advancedhunt.configurations;

import de.theredend2000.advancedhunt.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

public class EggConfig extends MultiFileConfiguration {
    private static final TreeMap<Double, ConfigUpgrader> upgraders = new TreeMap<>();

    public EggConfig(JavaPlugin plugin) {
        super(plugin, "eggs", "yml", 2.2);
    }


    //region Upgrader
    @Override
    public TreeMap<Double, ConfigUpgrader> getUpgrader() {
        return upgraders;
    }

    @Override
    public void registerUpgrader() {
        upgraders.put(1.0, (oldConfig, newConfig) -> {
            convertToNewCommandSystemV1(oldConfig, newConfig);
        });
        upgraders.put(2.0, (oldConfig, newConfig) -> {
            convertToNewCommandSystemV2(oldConfig, newConfig);
        });
        upgraders.put(2.1, (oldConfig, newConfig) -> {
            addChances(oldConfig, newConfig);
        });
        upgraders.put(2.2, (oldConfig, newConfig) -> {
            List<ConfigMigration.ReplacementEntry> keyReplacements = Arrays.asList(
                    new ConfigMigration.ReplacementEntry("^(?<=.*)egg(?=.*:)", "treasure", true, true),
                    new ConfigMigration.ReplacementEntry("(?<=.*)egg(?=.*)", "treasure", true, true)
            );

            List<ConfigMigration.ReplacementEntry> valueReplacements = Arrays.asList(
                    new ConfigMigration.ReplacementEntry("AdvancedEggHunt", "AdvancedHunt", false, false),
                    new ConfigMigration.ReplacementEntry("%EGG", "%TREASURE", false, false),
                    new ConfigMigration.ReplacementEntry("%MAX_TREASURES%", "%MAX_TREASURES%", false, false),
                    new ConfigMigration.ReplacementEntry("placeEggs", "place", false, false),
                    new ConfigMigration.ReplacementEntry("/egghunt", "/%PLUGIN_COMMAND%", false, false),
                    new ConfigMigration.ReplacementEntry("(?<=^.*)\\begg(?!s?.yml)", "treasure", true, false)
            );

            ConfigMigration migration = new ConfigMigration(true, keyReplacements, valueReplacements);
            migration.standardUpgrade(oldConfig, newConfig);

            newConfig.set("help-message", newConfig.getDefaults().getString("help-message"));
        });
    }

    private void convertToNewCommandSystemV1(ConfigurationSection oldConfig, ConfigurationSection newConfig) {
        if (!oldConfig.contains("Rewards.") || !oldConfig.contains("PlacedEggs.")) {
            return;
        }
        ConfigurationSection rewardsSection = oldConfig.getConfigurationSection("Rewards.");
        ConfigurationSection placedEggsSection = oldConfig.getConfigurationSection("PlacedEggs.");

        if (rewardsSection == null || placedEggsSection == null) {
            return;
        }
        for (String rewardsID : rewardsSection.getKeys(false)) {
            if (oldConfig.getInt("Rewards." + rewardsID + ".type") == 0) {
                for (String eggID : placedEggsSection.getKeys(false)) {
                    ConfigurationSection eggRewardsSection = newConfig.createSection("PlacedEggs." + eggID + ".Rewards");

                    String command = oldConfig.getString("Rewards." + rewardsID + ".command");
                    boolean enabled = oldConfig.getBoolean("Rewards." + rewardsID + ".enabled");

                    int nextNumber = getNextRewardIndex(eggRewardsSection);

                    eggRewardsSection.set(nextNumber + ".command", command);
                    eggRewardsSection.set(nextNumber + ".enabled", enabled);
                }
            }
        }

        newConfig.set("Rewards", null);
    }

    private void convertToNewCommandSystemV2(ConfigurationSection oldConfig, ConfigurationSection newConfig) {
        if (!oldConfig.contains("Rewards.") || !oldConfig.contains("PlacedEggs.")) {
            return;
        }
        ConfigurationSection rewardsSection = oldConfig.getConfigurationSection("Rewards.");
        ConfigurationSection placedEggsSection = oldConfig.getConfigurationSection("PlacedEggs.");

        if (rewardsSection == null || placedEggsSection == null) {
            return;
        }
        for (String rewardsID : rewardsSection.getKeys(false)) {
            if  (oldConfig.getInt("Rewards." + rewardsID + ".type") == 1) {
                ConfigurationSection globalRewardsSection = newConfig.createSection("GlobalRewards");

                String command = oldConfig.getString("Rewards." + rewardsID + ".command");
                boolean enabled = oldConfig.getBoolean("Rewards." + rewardsID + ".enabled");

                int nextNumber = getNextRewardIndex(globalRewardsSection);

                globalRewardsSection.set(nextNumber + ".command", command);
                globalRewardsSection.set(nextNumber + ".enabled", enabled);
            }
        }

        newConfig.set("Rewards", null);
    }

    private void addChances(ConfigurationSection oldConfig, ConfigurationSection newConfig) {
        ConfigurationSection placedEggsSection = newConfig.getConfigurationSection("PlacedEggs.");
        if (placedEggsSection != null) {
            for (String eggID : placedEggsSection.getKeys(false)) {
                ConfigurationSection rewardsSection = placedEggsSection.getConfigurationSection(eggID + ".Rewards.");
                if (rewardsSection != null) {
                    for (String commandID : rewardsSection.getKeys(false)) {
                        if (!rewardsSection.contains(commandID + ".chance")) {
                            rewardsSection.set(commandID + ".chance", 100);
                        }
                    }
                }
            }
        }

        ConfigurationSection globalRewardsSection = newConfig.getConfigurationSection("GlobalRewards.");
        if (globalRewardsSection != null) {
            for (String commandID : globalRewardsSection.getKeys(false)) {
                if (!globalRewardsSection.contains(commandID + ".chance")) {
                    globalRewardsSection.set(commandID + ".chance", 100);
                }
            }
        }
    }
    //endregion

    private int getNextRewardIndex(ConfigurationSection rewardsSection) {
        int maxIndex = -1;
        if (rewardsSection == null) {
            return maxIndex + 1;
        }
        for (String key : rewardsSection.getKeys(false)) {
            try {
                int index = Integer.parseInt(key);
                maxIndex = Math.max(maxIndex, index);
            } catch (NumberFormatException ignored) {
            }
        }
        return maxIndex + 1;
    }

    /**
     * Saves the configuration data to file.
     */
    public void saveData(String configName) {
        saveConfig(configName);
    }

    /**
     * Sets the enabled state of the egg collection.
     * @param configName The name of the configuration file.
     * @param enabled The enabled state to set.
     */
    public void setEnabled(String configName, boolean enabled) {
        set(configName, "Enabled", enabled);
        saveData(configName);
    }

    /**
     * Gets the enabled state of the egg collection.
     * @param configName The name of the configuration file.
     * @return The enabled state.
     */
    public boolean isEnabled(String configName) {
        return getConfig(configName).getBoolean("Enabled", false);
    }

    /**
     * Sets the requirements order for the egg collection.
     * @param configName The name of the configuration file.
     * @param order The order to set (e.g., "OR", "AND").
     */
    public void setRequirementsOrder(String configName, String order) {
        set(configName, "RequirementsOrder", order);
        saveData(configName);
    }

    /**
     * Gets the requirements order for the egg collection.
     * @param configName The name of the configuration file.
     * @return The requirements order.
     */
    public String getRequirementsOrder(String configName) {
        return getConfig(configName).getString("RequirementsOrder", "OR");
    }

    /**
     * Sets a reward for an egg.
     * @param configName The name of the configuration file.
     * @param commandID The ID of the command.
     * @param command The command to execute.
     * @param path The path in the configuration.
     */
    public void setReward(String configName, String commandID, String command, String path) {
        set(configName, path + commandID + ".command", command);
        set(configName, path + commandID + ".enabled", true);
        set(configName, path + commandID + ".chance", 100);
        saveData(configName);
    }

    /**
     * Checks if the configuration contains a specific section.
     * @param configName The name of the configuration file.
     * @param section The section to check for.
     * @return True if the section exists, false otherwise.
     */
    public boolean containsSection(String configName, String section) {
        return getConfig(configName).contains(section);
    }

    /**
     * Gets the FileConfiguration for a specific egg collection.
     * @param configName The name of the configuration file.
     * @return The FileConfiguration for the specified egg collection.
     */
    public FileConfiguration getConfig(String configName) {
        return super.getConfig(configName);
    }

    /**
     * Creates a new egg collection file.
     * @param collection The name of the collection.
     * @param enabled Whether the collection should be enabled.
     */
    public void createEggCollectionFile(String collection, boolean enabled) {
        FileConfiguration config = getConfig(collection);
        config.set("Enabled", enabled);
        config.set("RequirementsOrder", "OR");
        saveConfig(collection);
    }

    /**
     * Deletes an egg collection.
     * @param collection The name of the collection to delete.
     */
    public void deleteCollection(String collection) {
        deleteConfig(collection);
        Main.getInstance().getEggManager().spawnEggParticle();
    }

    /**
     * Checks if a collection exists.
     * @param collection The name of the collection to check.
     * @return True if the collection exists, false otherwise.
     */
    public boolean containsCollection(String collection) {
        return savedEggCollections().contains(collection);
    }

    /**
     * Gets a list of all saved egg collections.
     * @return A list of egg collection names without file extensions.
     */
    public List<String> savedEggCollections() {
        List<String> collections = new ArrayList<>();
        for (String fileName : configFiles.keySet()) {
            collections.add(fileName.substring(0, fileName.length() - fileExtension.length()));
        }
        return collections;
    }
}
