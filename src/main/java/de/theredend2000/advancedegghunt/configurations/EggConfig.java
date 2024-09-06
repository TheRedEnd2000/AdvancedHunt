package de.theredend2000.advancedegghunt.configurations;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EggConfig extends MultiFileConfiguration {
    private static final TreeMap<String, TreeMap<Double, ConfigUpgrader>> upgraders = new TreeMap<>();

    public EggConfig(JavaPlugin plugin) {
        super(plugin, "eggs", "yml", false, 1);
    }

    @Override
    public Map<String, TreeMap<Double, ConfigUpgrader>> getUpgraders() {
        return upgraders;
    }

    @Override
    public void registerUpgraders() {
        // Add upgraders if needed in the future
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
    }

    /**
     * Checks if a collection exists.
     * @param collection The name of the collection to check.
     * @return True if the collection exists, false otherwise.
     */
    public boolean containsCollection(String collection) {
        return configFiles.containsKey(collection);
    }

    /**
     * Gets a list of all saved egg collections.
     * @return A list of egg collection names.
     */
    public List<String> savedEggCollections() {
        return new ArrayList<>(configFiles.keySet());
    }
}
