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
    private HashMap<String, FileConfiguration> eggsConfigs;
    private HashMap<String, File> eggsFile;

    public EggDataManager(Main plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        eggsConfigs = new HashMap<>();
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

    private void loadEggData(String section) {
        FileConfiguration config = getPlacedEggs(section);
        if(!eggsConfigs.containsKey(section))
            this.eggsConfigs.put(section, config);
    }

    private File getFile(String section) {
        if(!eggsFile.containsKey(section))
            eggsFile.put(section,new File(this.dataFolder + "/eggs/", section + ".yml"));
        return eggsFile.get(section);
    }

    public FileConfiguration getPlacedEggs(String section) {
        File playerFile = this.getFile(section);
        if(!eggsConfigs.containsKey(section))
            this.eggsConfigs.put(section, YamlConfiguration.loadConfiguration(playerFile));
        return eggsConfigs.get(section);
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
        this.eggsConfigs.put(section, config);
        this.loadEggData(section);
        this.savePlacedEggs(section, config);
        config.set("Enabled", enabled);
        config.set("RequirementsOrder", "OR");
        this.savePlacedEggs(section, config);
        this.addDefaultCommands(section);
    }

    public boolean containsSectionFile(String section) {
        Iterator var2 = this.savedEggCollections().iterator();

        String sections;
        do {
            if (!var2.hasNext()) {
                return false;
            }

            sections = (String)var2.next();
        } while(!sections.contains(section));

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
                File[] var4 = playerFiles;
                int var5 = playerFiles.length;

                for(int var6 = 0; var6 < var5; ++var6) {
                    File playerFile = var4[var6];
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
        File playerDataFolder = new File(String.valueOf(this.dataFolder) + "/playerdata/");
        if (playerDataFolder.exists() && playerDataFolder.isDirectory()) {
            File[] playerFiles = playerDataFolder.listFiles((dir, name) -> {
                return name.endsWith(".yml");
            });
            if (playerFiles != null) {
                File[] var4 = playerFiles;
                int var5 = playerFiles.length;

                for(int var6 = 0; var6 < var5; ++var6) {
                    File playerFile = var4[var6];
                    String fileName = playerFile.getName();
                    UUID playerUUID = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                    playerUUIDs.add(playerUUID);
                }
            }
        }

        return playerUUIDs;
    }
}

