package de.theredend2000.advancedhunt.configurations;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.enums.DeletionTypes;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

public class PlayerEggConfig extends MultiFileConfiguration {
    private static final TreeMap<Double, ConfigUpgrader> upgraders = new TreeMap<>();

    public PlayerEggConfig(JavaPlugin plugin) {
        super(plugin, "playerdata", "yml", 1.1);
    }

    @Override
    public TreeMap<Double, ConfigUpgrader> getUpgrader() {
        return upgraders;
    }

    @Override
    public void registerUpgrader() {
    }

    public void saveData(String configName) {
        saveConfig(configName);
    }

    public void createPlayerFile(UUID uuid) {
        String configName = uuid.toString() + super.fileExtension;

        FileConfiguration config = getConfig(configName);
        File playerFile = getConfigFile(configName);
        if (playerFile != null && playerFile.exists()) {
            return;
        }
        try {
            if (playerFile != null) {
                playerFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        configs.put(configName, (YamlConfiguration) config);
        saveConfig(configName);
        setDeletionType(DeletionTypes.Noting, uuid);
        savePlayerCollection(uuid, "default");
    }

    public void setDeletionType(DeletionTypes deletionType, UUID uuid) {
        String configName = uuid.toString();
        FileConfiguration config = getConfig(configName);
        config.set("DeletionType", deletionType.name());
        saveConfig(configName);
    }

    public DeletionTypes getDeletionType(UUID uuid) {
        String configName = uuid.toString();
        FileConfiguration config = getConfig(configName);
        return DeletionTypes.valueOf(config.getString("DeletionType"));
    }

    public void savePlayerCollection(UUID uuid, String collection) {
        String configName = uuid.toString();
        FileConfiguration config = getConfig(configName);
        config.set("SelectedSection", collection);
        saveConfig(configName);
    }

    public void setResetTimer(UUID uuid, String collection, String id) {
        String configName = uuid.toString();
        FileConfiguration config = getConfig(configName);
        int currentSeconds = Main.getInstance().getRequirementsManager().getOverallTime(collection);
        if (currentSeconds != 0) {
            long toSet = System.currentTimeMillis() + (long)currentSeconds * 1000L;
            config.set("FoundEggs." + collection + "." + id + ".ResetCooldown", toSet);
            saveConfig(configName);
        }
    }

    public long getResetTimer(UUID uuid, String collection, String id) {
        String configName = uuid.toString();
        FileConfiguration config = getConfig(configName);
        return !config.contains("FoundEggs." + collection + "." + id + ".ResetCooldown") ? System.currentTimeMillis() + 1000000L : config.getLong("FoundEggs." + collection + "." + id + ".ResetCooldown");
    }

    /**
     * Resets all player data for all players.
     */
    public void resetAllPlayerData() {
        List<UUID> uuids = getSavedPlayerUUIDs();
        for (UUID uuid : uuids) {
            resetPlayerData(uuid);
        }
    }

    /**
     * Resets a specific player's data for all collections.
     * @param uuid The UUID of the player to reset data for.
     */
    public void resetPlayerData(UUID uuid) {
        String configName = uuid.toString();
        FileConfiguration config = getConfig(configName);
        config.set("FoundEggs", null);
        saveConfig(configName);
    }

    /**
     * Gets a list of all player UUIDs with saved data.
     * @return List of player UUIDs.
     */
    public List<UUID> getSavedPlayerUUIDs() {
        List<UUID> playerUUIDs = new ArrayList<>();
        File playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (playerDataFolder.exists() && playerDataFolder.isDirectory()) {
            File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (playerFiles != null) {
                for (File playerFile : playerFiles) {
                    String fileName = playerFile.getName();
                    try {
                        UUID playerUUID = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                        playerUUIDs.add(playerUUID);
                    } catch (IllegalArgumentException e) {
                        // Skip files with invalid UUIDs
                    }
                }
            }
        }
        return playerUUIDs;
    }
}
