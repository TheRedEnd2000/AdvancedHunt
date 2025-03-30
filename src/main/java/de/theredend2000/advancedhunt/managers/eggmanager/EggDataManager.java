package de.theredend2000.advancedhunt.managers.eggmanager;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.configurations.EggConfig;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EggDataManager {

    private final Main plugin;
    private final File dataFolder;
    private final EggConfig eggConfig;

    public EggDataManager(Main plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.eggConfig = new EggConfig(plugin);

        dataFolder.mkdirs();
        new File(dataFolder, "playerdata").mkdirs();
        if(eggConfig.savedEggCollections().size() < 1) {
            eggConfig.createEggCollectionFile("default", true);
            Main.setupDefaultCollection = true;
        }
    }

    public void reload() {
        eggConfig.reloadConfigs();
    }

    public void initEggs() {
        List<String> savedEggCollections = new ArrayList<>(eggConfig.savedEggCollections());

        for (String collection : savedEggCollections) {
            eggConfig.getConfig(collection);
        }
    }

    /**
     * Checks if a requirement is enabled for a specific collection
     * @param collection The collection name
     * @param requirementType The type of requirement (e.g., "Season", "Year", "Month")
     * @param key The specific key for the requirement
     * @return True if the requirement is enabled, false otherwise
     */
    public boolean isRequirementEnabled(String collection, String requirementType, String key) {
        return eggConfig.isRequirementEnabled(collection, requirementType, key);
    }

    /**
     * Sets the enabled state of a requirement for a specific collection
     * @param collection The collection name
     * @param requirementType The type of requirement (e.g., "Season", "Year", "Month")
     * @param key The specific key for the requirement
     * @param enabled Whether the requirement should be enabled
     */
    public void setRequirementEnabled(String collection, String requirementType, String key, boolean enabled) {
        eggConfig.setRequirementEnabled(collection, requirementType, key, enabled);
    }

    /**
     * Gets the order configuration for requirements (AND/OR)
     * @param collection The collection name
     * @return The requirements order string
     */
    public String getRequirementsOrder(String collection) {
        return eggConfig.getRequirementsOrder(collection);
    }

    /**
     * Sets the order configuration for requirements
     * @param collection The collection name
     * @param order The order to set (AND/OR)
     */
    public void setRequirementsOrder(String collection, String order) {
        eggConfig.setRequirementsOrder(collection, order);
    }

    /**
     * Gets the FileConfiguration for a specific collection.
     * This method should only be used within the EggDataManager class 
     * and should be phased out as encapsulated methods are implemented.
     * @param collection The collection name
     * @return The FileConfiguration for the collection
     * @deprecated Use specific getter and setter methods instead
     */
    @Deprecated
    public FileConfiguration getPlacedEggs(String collection) {
        return eggConfig.getConfig(collection);
    }

    public void setRewards(String commandID, String command, String collection, String path) {
        eggConfig.setReward(collection, commandID, command, path);
    }

    public boolean containsSectionFile(String section) {
        for (String collection : eggConfig.savedEggCollections()) {
            if (eggConfig.containsSection(collection, section)) {
                return true;
            }
        }
        return false;
    }

    public List<String> savedEggCollections() {
        return eggConfig.savedEggCollections();
    }

    public void createEggCollectionFile(String collection, boolean enabled) {
        eggConfig.createEggCollectionFile(collection, enabled);
    }

    public void savePlacedEggs(String collection) {
        eggConfig.saveData(collection);
    }

    public void deleteCollection(String collection) {
        eggConfig.deleteCollection(collection);
    }

    public boolean containsCollection(String collection) {
        return eggConfig.containsCollection(collection);
    }

    public List<UUID> savedPlayers() {
        List<UUID> playerUUIDs = new ArrayList<>();
        File playerDataFolder = new File(this.dataFolder, "playerdata");
        if (playerDataFolder.exists() && playerDataFolder.isDirectory()) {
            File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (playerFiles != null) {
                for (File playerFile : playerFiles) {
                    String fileName = playerFile.getName();
                    UUID playerUUID = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                    playerUUIDs.add(playerUUID);
                }
            }
        }
        return playerUUIDs;
    }
}

