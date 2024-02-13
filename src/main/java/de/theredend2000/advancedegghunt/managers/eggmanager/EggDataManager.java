package de.theredend2000.advancedegghunt.managers.eggmanager;

import de.theredend2000.advancedegghunt.Main;
import org.bukkit.Bukkit;
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

    public EggDataManager(Main plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();

        dataFolder.mkdirs();
        new File(dataFolder, "playerdata").mkdirs();
        new File(dataFolder, "eggs").mkdirs();
        if(savedEggSections().size() < 1)
            createEggSectionFile("default",true);
    }

    public void initEggs() {
        List<String> savedEggSections = new ArrayList<>(plugin.getEggDataManager().savedEggSections());
        for(String section : savedEggSections)
            getPlacedEggs(section);
    }

    private void loadEggData(String section) {
        getPlacedEggs(section);
    }

    private File getFile(String section) {
        return new File(dataFolder + "/eggs/", section + ".yml");
    }

    public FileConfiguration getPlacedEggs(String section) {
        File playerFile = getFile(section);
        return YamlConfiguration.loadConfiguration(playerFile);
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
        savePlacedEggs(section,config);
    }

    public boolean containsSectionFile(String section){
        for(String sections : savedEggSections()){
            if(sections.contains(section)) return true;
        }
        return false;
    }
    public List<String> savedEggSections() {
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

    public void deleteCollection(String section) {
        File sectionFile = getFile(section);
        if (sectionFile.exists()) {
            sectionFile.delete();
        }
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

