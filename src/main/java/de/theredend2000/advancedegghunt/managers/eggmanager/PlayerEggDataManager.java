package de.theredend2000.advancedegghunt.managers.eggmanager;

import de.theredend2000.advancedegghunt.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

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

    public PlayerEggDataManager() {
        plugin = Main.getInstance();
        dataFolder = plugin.getDataFolder();
        playerConfigs = new HashMap<>();
    }

    public void initPlayers() {
        List<UUID> savedPlayers = new ArrayList<>(plugin.getEggDataManager().savedPlayers());
        for(UUID uuid : savedPlayers)
            loadPlayerData(uuid);
    }

    private void loadPlayerData(UUID uuid) {
        FileConfiguration config = getPlayerData(uuid);
        playerConfigs.put(uuid, config);
    }

    private File getFile(UUID uuid) {
        return new File(dataFolder + "/playerdata/", uuid + ".yml");
    }

    public FileConfiguration getPlayerData(UUID uuid) {
        File playerFile = getFile(uuid);
        return YamlConfiguration.loadConfiguration(playerFile);
    }

    public void savePlayerData(UUID uuid,FileConfiguration config) {
        try {
            config.save(getFile(uuid));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createPlayerFile(UUID uuid) {
        FileConfiguration config = getPlayerData(uuid);
        File playerFile = getFile(uuid);

        if (!playerFile.exists()) {
            try {
                playerFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        loadPlayerData(uuid);
        savePlayerData(uuid,config);
    }
}