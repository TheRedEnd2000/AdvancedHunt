package de.theredend2000.advancedegghunt.managers.eggmanager;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class EggDataManager {

    private final Main plugin;
    private final File dataFolder;
    private HashMap<String, FileConfiguration> eggsConfigs;

    public EggDataManager(Main plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        eggsConfigs = new HashMap<>();

        dataFolder.mkdirs();
        new File(dataFolder, "playerdata").mkdirs();
        new File(dataFolder, "eggs").mkdirs();
        if(savedEggSectionFiles().size() < 1) {
            createEggSectionFile("default", true);
            Main.setupDefaultCollection = true;
        }
    }

    public void initEggs() {
        List<String> savedEggSections = new ArrayList<>(plugin.getEggDataManager().savedEggSectionFiles());
        for(String section : savedEggSections)
            loadEggData(section);
    }

    public List<String> savedEggSectionFiles(){
        List<String> eggsSections = new ArrayList<>();
        File eggsSectionsFolder = new File(dataFolder + "/eggs/");

        if (eggsSectionsFolder.exists() && eggsSectionsFolder.isDirectory()) {
            File[] playerFiles = eggsSectionsFolder.listFiles((dir, name) -> name.endsWith(".yml"));

            if (playerFiles != null) {
                for (File playerFile : playerFiles) {
                    String fileName = playerFile.getName();
                    String sectionName = fileName.substring(0, fileName.length() - 4);
                    eggsSections.add(sectionName);
                }
            }
        }
        return eggsSections;
    }

    private void loadEggData(String section) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(getFile(section));
        eggsConfigs.put(section, config);
    }

    private File getFile(String section) {
        return new File(dataFolder + "/eggs/", section + ".yml");
    }

    public FileConfiguration getPlacedEggs(String section) {
        return eggsConfigs.get(section);
    }

    public void savePlacedEggs(String section,FileConfiguration config) {
        try {
            config.save(getFile(section));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createEggSectionFile(String section,boolean enabled) {
        FileConfiguration config = getPlacedEggs(section);
        File playerFile = getFile(section);

        if (!playerFile.exists()) {
            try {
                playerFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        loadEggData(section);
        savePlacedEggs(section,config);
        config.set("Enabled",enabled);
        config.set("RequirementsOrder","OR");
        savePlacedEggs(section,config);
        addDefaultCommands(section);
    }

    public boolean containsSectionFile(String section){
        for(String sections : savedEggSections()){
            if(sections.contains(section)) return true;
        }
        return false;
    }
    public List<String> savedEggSections() {
        return new ArrayList<>(eggsConfigs.keySet());
    }

    public void deleteCollection(String section) {
        File sectionFile = getFile(section);
        if (sectionFile.exists()) {
            sectionFile.delete();
        }
    }

    private void addDefaultCommands(String section){
        FileConfiguration config = getPlacedEggs(section);
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
        savePlacedEggs(section,config);
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

