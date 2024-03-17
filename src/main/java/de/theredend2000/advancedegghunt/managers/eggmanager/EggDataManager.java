package de.theredend2000.advancedegghunt.managers.eggmanager;

import de.theredend2000.advancedegghunt.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EggDataManager {

    private final Main plugin;
    private final File dataFolder;
    private HashMap<String, FileConfiguration> eggCollectionsConfigs;
    private HashMap<String, File> eggsFile;

    public EggDataManager(Main plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        eggCollectionsConfigs = new HashMap<>();
        eggsFile = new HashMap<>();

        dataFolder.mkdirs();
        new File(dataFolder, "playerdata").mkdirs();
        new File(dataFolder, "eggs").mkdirs();
        if(savedEggCollections().size() < 1) {
            createEggCollectionFile("default", true);
            Main.setupDefaultCollection = true;
        }
    }

    public void reload() {
        eggCollectionsConfigs = new HashMap<>();
    }

    public void unloadEggData(String collection) {
        if (!eggCollectionsConfigs.containsKey(collection)) {
            return;
        }
        eggCollectionsConfigs.remove(collection);
    }

    public void initEggs() {
        List<String> savedEggCollections = new ArrayList(this.plugin.getEggDataManager().savedEggCollections());

        for (String collection : savedEggCollections) {
            this.getPlacedEggs(collection);
        }
    }

    private void loadEggData(String collection) {
        FileConfiguration config = getPlacedEggs(collection);
        if(!eggCollectionsConfigs.containsKey(collection))
            this.eggCollectionsConfigs.put(collection, config);
    }

    private File getFile(String collection) {
        if(!eggsFile.containsKey(collection))
            eggsFile.put(collection, new File(this.dataFolder + "/eggs/", collection + ".yml"));
        return eggsFile.get(collection);
    }

    public FileConfiguration getPlacedEggs(String collection) {
        File playerFile = this.getFile(collection);
        if(!eggCollectionsConfigs.containsKey(collection))
            this.eggCollectionsConfigs.put(collection, YamlConfiguration.loadConfiguration(playerFile));
        return eggCollectionsConfigs.get(collection);
    }

    public void savePlacedEggs(String collection, FileConfiguration config) {
        try {
            config.save(this.getFile(collection));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createEggCollectionFile(String collection, boolean enabled) {
        FileConfiguration config = this.getPlacedEggs(collection);
        File playerFile = this.getFile(collection);
        if (!playerFile.exists()) {
            try {
                playerFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.eggCollectionsConfigs.put(collection, config);
        this.loadEggData(collection);
        this.savePlacedEggs(collection, config);
        config.set("Enabled", enabled);
        config.set("RequirementsOrder", "OR");
        this.savePlacedEggs(collection, config);
    }

    public boolean containsSectionFile(String section) {
        Iterator savedEggCollectionsIterator = this.savedEggCollections().iterator();

        String collection;
        do {
            if (!savedEggCollectionsIterator.hasNext()) {
                return false;
            }

            collection = (String)savedEggCollectionsIterator.next();
        } while(!collection.contains(section));

        return true;
    }

    public List<String> savedEggCollections() {
        List<String> eggCollections = new ArrayList();
        File eggsSectionsFolder = new File(this.dataFolder + "/eggs/");
        if (eggsSectionsFolder.exists() && eggsSectionsFolder.isDirectory()) {
            File[] playerFiles = eggsSectionsFolder.listFiles((dir, name) -> {
                return name.endsWith(".yml");
            });
            if (playerFiles != null) {
                int playerFilesLength = playerFiles.length;

                for(int i = 0; i < playerFilesLength; ++i) {
                    File playerFile = playerFiles[i];
                    String fileName = playerFile.getName();
                    String collectionName = fileName.substring(0, fileName.length() - 4);
                    eggCollections.add(collectionName);
                }
            }
        }
        return eggCollections;
    }

    public void deleteCollection(String collection) {
        File collectionFile = this.getFile(collection);
        if (collectionFile.exists()) {
            collectionFile.delete();
        }
    }

    public List<UUID> savedPlayers() {
        List<UUID> playerUUIDs = new ArrayList();
        File playerDataFolder = new File(this.dataFolder + "/playerdata/");
        if (playerDataFolder.exists() && playerDataFolder.isDirectory()) {
            File[] playerFiles = playerDataFolder.listFiles((dir, name) -> {
                return name.endsWith(".yml");
            });
            if (playerFiles != null) {
                int playerFilesLength = playerFiles.length;

                for(int i = 0; i < playerFilesLength; ++i) {
                    File playerFile = playerFiles[i];
                    String fileName = playerFile.getName();
                    UUID playerUUID = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                    playerUUIDs.add(playerUUID);
                }
            }
        }

        return playerUUIDs;
    }
}

