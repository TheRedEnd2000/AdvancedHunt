package de.theredend2000.advancedhunt.managers.eggmanager;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.configurations.EggConfig;
import de.theredend2000.advancedhunt.mysql.Database;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

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
        if (plugin.getMySQLConfig().isEnabled()) {
            try {
                Database db = plugin.getDatabase();
                PreparedStatement check = db.getConnection()
                        .prepareStatement("SELECT 1 FROM collections WHERE collection_id = ?");
                check.setString(1, "default");
                ResultSet rs = check.executeQuery();

                // Wenn die Collection noch nicht existiert, anlegen
                if (!rs.next()) {
                    PreparedStatement insert = db.getConnection()
                            .prepareStatement("INSERT INTO collections (collection_id, max_eggs, enabled) VALUES (?, ?, ?)");
                    insert.setString(1, "default");
                    insert.setInt(2, 0);          // default max_eggs
                    insert.setBoolean(3, true);   // enabled
                    insert.executeUpdate();
                    insert.close();
                }
                Bukkit.getConsoleSender().sendMessage("§aCreated!");

                rs.close();
                check.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
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

    /**
     * Resets all egg statistics for all collections.
     */
    public void resetAllEggStatistics() {
        eggConfig.resetAllEggStatistics();
    }
}

