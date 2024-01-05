package de.theredend2000.advancedegghunt.managers.eggmanager;

import de.theredend2000.advancedegghunt.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EggDataManager {

    private final Main plugin;
    private final File dataFolder;

    private static File eggsFile;
    private static FileConfiguration eggs;

    public EggDataManager(Main plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();

        dataFolder.mkdirs();
        new File(dataFolder, "playerdata").mkdirs();
        new File(dataFolder, "eggs").mkdirs();
        createEggFile();
    }

    public void createEggFile() {
        eggsFile = new File(dataFolder + "/eggs/placedEggs.yml");

        if (!eggsFile.exists()) {
            try {
                eggsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        eggs = YamlConfiguration.loadConfiguration(eggsFile);
        savePlacedEggs();
    }

    public void savePlacedEggs(){
        try {
            eggs.save(eggsFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FileConfiguration getPlacedEggs() {
        return eggs;
    }

    /*
            PLAYER DATA
    */

    public List<UUID> savedPlayers() {
        List<UUID> playerUUIDs = new ArrayList<>();
        File playerDataFolder = new File(dataFolder + "/playerdata/");

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

