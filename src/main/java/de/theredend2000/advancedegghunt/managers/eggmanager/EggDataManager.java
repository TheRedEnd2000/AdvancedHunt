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
            createEggSectionFile("default", true);
            Main.setupDefaultCollection = true;
        }
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

    public void savePlacedEggs(String section, FileConfiguration config) {
        try {
            config.save(this.getFile(section));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createEggSectionFile(String section, boolean enabled) {
        FileConfiguration config = this.getPlacedEggs(section);
        File playerFile = this.getFile(section);
        if (!playerFile.exists()) {
            try {
                playerFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.eggCollectionsConfigs.put(section, config);
        this.loadEggData(section);
        this.savePlacedEggs(section, config);
        config.set("Enabled", enabled);
        config.set("RequirementsOrder", "OR");
        this.savePlacedEggs(section, config);
        this.addDefaultCommands(section);
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
        List<String> eggsSections = new ArrayList();
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
                    String sectionName = fileName.substring(0, fileName.length() - 4);
                    eggsSections.add(sectionName);
                }
            }
        }
        return eggsSections;
    }

    public void deleteCollection(String section) {
        File sectionFile = this.getFile(section);
        if (sectionFile.exists()) {
            sectionFile.delete();
        }
    }

    private void addDefaultCommands(String section) {
        FileConfiguration config = this.getPlacedEggs(section);
        config.set("Rewards.0.command", "tellraw %PLAYER% \"%PREFIX%&aYou found an egg. &7(&e%EGGS_FOUND%&7/&e%EGGS_MAX%&7)\"");
        config.set("Rewards.0.enabled", true);
        config.set("Rewards.0.type", 0);
        config.set("Rewards.1.command", "give %PLAYER% diamond");
        config.set("Rewards.1.enabled", true);
        config.set("Rewards.1.type", 0);
        config.set("Rewards.2.command", "tellraw %PLAYER% \"%PREFIX%&aYou found an egg. &7(&e%EGGS_FOUND%&7/&e%EGGS_MAX%&7)\"");
        config.set("Rewards.2.enabled", true);
        config.set("Rewards.2.type", 1);
        config.set("Rewards.3.command", "give %PLAYER% diamond");
        config.set("Rewards.3.enabled", true);
        config.set("Rewards.3.type", 1);
        config.set("Rewards.4.command", "tellraw %PLAYER% \"%PREFIX%&6You found all eggs!\"");
        config.set("Rewards.4.enabled", true);
        config.set("Rewards.4.type", 1);
        this.savePlacedEggs(section, config);
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

