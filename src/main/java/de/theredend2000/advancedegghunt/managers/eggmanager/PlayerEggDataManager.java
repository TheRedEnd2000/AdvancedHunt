package de.theredend2000.advancedegghunt.managers.eggmanager;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.enums.DeletionTypes;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PlayerEggDataManager {
    private Main plugin;
    private File dataFolder;
    private HashMap<UUID, FileConfiguration> playerConfigs;
    private HashMap<UUID, File> playerFiles;

    /**
     * Constructor for PlayerEggDataManager.
     * Initializes the plugin instance, data folder, and hash maps for player configurations and files.
     */
    public PlayerEggDataManager() {
        plugin = Main.getInstance();
        dataFolder = plugin.getDataFolder();
        playerConfigs = new HashMap<>();
        playerFiles = new HashMap<>();
    }

    /**
     * Reloads the player configurations by creating a new HashMap.
     */
    public void reload() {
        playerConfigs = new HashMap<>();
    }

    /**
     * Unloads the player data for a specific UUID.
     * @param uuid The UUID of the player whose data should be unloaded.
     */
    public void unloadPlayerData(UUID uuid) {
        if (!playerConfigs.containsKey(uuid)) {
            return;
        }
        playerConfigs.remove(uuid);
    }

    /**
     * Initializes player data for all saved players.
     */
    public void initPlayers() {
        List<UUID> savedPlayers = new ArrayList<>(plugin.getEggDataManager().savedPlayers());
        for(UUID uuid : savedPlayers)
            getPlayerData(uuid);
    }

    /**
     * Gets the File object for a player's data file.
     * @param uuid The UUID of the player.
     * @return The File object for the player's data.
     */
    private File getFile(UUID uuid) {
        if(!playerFiles.containsKey(uuid))
            playerFiles.put(uuid, new File(this.dataFolder + "/playerdata/", uuid + ".yml"));
        return playerFiles.get(uuid);
    }

    /**
     * Gets the FileConfiguration for a player's data.
     * @param uuid The UUID of the player.
     * @return The FileConfiguration containing the player's data.
     */
    public FileConfiguration getPlayerData(UUID uuid) {
        File playerFile = this.getFile(uuid);
        if(!playerConfigs.containsKey(uuid)) {
            this.playerConfigs.put(uuid, YamlConfiguration.loadConfiguration(playerFile));
        }
        return playerConfigs.get(uuid);
    }

    /**
     * Saves the player's data to file.
     * @param uuid The UUID of the player.
     * @param config The FileConfiguration containing the player's data.
     */
    public void savePlayerData(UUID uuid, FileConfiguration config) {
        try {
            config.save(this.getFile(uuid));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the player's selected collection.
     * @param uuid The UUID of the player.
     * @param collection The name of the selected collection.
     */
    public void savePlayerCollection(UUID uuid, String collection) {
        FileConfiguration config = this.getPlayerData(uuid);
        config.set("SelectedSection", collection);
        this.savePlayerData(uuid, config);
    }

    /**
     * Creates a new player file if it doesn't exist.
     * @param uuid The UUID of the player.
     */
    public void createPlayerFile(UUID uuid) {
        FileConfiguration config = this.getPlayerData(uuid);
        File playerFile = this.getFile(uuid);
        if (playerFile.exists()) {
            return;
        }
        try {
            playerFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.playerConfigs.put(uuid, config);
        this.savePlayerData(uuid, config);
        this.setDeletionType(DeletionTypes.Noting, uuid);
        this.savePlayerCollection(uuid, "default");
    }

    /**
     * Sets the deletion type for a player.
     * @param deletionType The DeletionType to set.
     * @param uuid The UUID of the player.
     */
    public void setDeletionType(DeletionTypes deletionType, UUID uuid) {
        FileConfiguration config = this.getPlayerData(uuid);
        config.set("DeletionType", deletionType.name());
        this.savePlayerData(uuid, config);
    }

    /**
     * Gets the deletion type for a player.
     * @param uuid The UUID of the player.
     * @return The DeletionType for the player.
     */
    public DeletionTypes getDeletionType(UUID uuid) {
        FileConfiguration config = this.getPlayerData(uuid);
        return DeletionTypes.valueOf(config.getString("DeletionType"));
    }

    /**
     * Sets the reset timer for a specific egg.
     * @param uuid The UUID of the player.
     * @param collection The collection name.
     * @param id The ID of the egg.
     */
    public void setResetTimer(UUID uuid, String collection, String id) {
        FileConfiguration cfg = getPlayerData(uuid);
        int currentSeconds = Main.getInstance().getRequirementsManager().getOverallTime(collection);
        if (currentSeconds != 0) {
            long toSet = System.currentTimeMillis() + (long)currentSeconds * 1000L;
            cfg.set("FoundEggs." + collection + "." + id + ".ResetCooldown", toSet);

            try {
                cfg.save(this.getFile(uuid));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets the reset timer for a specific egg.
     * @param uuid The UUID of the player.
     * @param collection The collection name.
     * @param id The ID of the egg.
     * @return The reset timer value.
     */
    public long getResetTimer(UUID uuid, String collection, String id) {
        FileConfiguration cfg = getPlayerData(uuid);
        return !cfg.contains("FoundEggs." + collection + "." + id + ".ResetCooldown") ? System.currentTimeMillis() + 1000000L : cfg.getLong("FoundEggs." + collection + "." + id + ".ResetCooldown");
    }

    /**
     * Checks if an egg can be reset.
     * @param uuid The UUID of the player.
     * @param collection The collection name.
     * @param id The ID of the egg.
     * @return True if the egg can be reset, false otherwise.
     */
    public boolean canReset(UUID uuid, String collection, String id) {
        long current = System.currentTimeMillis();
        long millis = this.getResetTimer(uuid, collection, id);
        return current > millis;
    }

    /**
     * Periodically checks and resets eggs for all players if the reset time has passed.
     */
    public void checkReset(){
        new BukkitRunnable() {
            @Override
            public void run() {
                for(UUID uuid : plugin.getEggDataManager().savedPlayers()){
                    FileConfiguration cfg = playerConfigs.get(uuid);
                    if(cfg == null) continue;
                    if(!cfg.contains("FoundEggs.")) continue;
                    for(String collection : cfg.getConfigurationSection("FoundEggs.").getKeys(false)) {
                        for(String eggId : cfg.getConfigurationSection("FoundEggs." + collection).getKeys(false)) {
                            if (eggId.equals("Count") || eggId.equals("Name")) continue;
                            if (canReset(uuid, collection, eggId))
                                plugin.getEggManager().resetStatsPlayerEgg(uuid, collection, eggId);
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20, 20);
    }
}
