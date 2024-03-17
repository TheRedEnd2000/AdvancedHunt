package de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.presets;

import de.theredend2000.advancedegghunt.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class PresetDataManager {

    private final Main plugin;
    private final File dataFolder;
    private HashMap<String, FileConfiguration> presetConfigs;
    private HashMap<String, File> presetFile;

    public PresetDataManager(Main plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        presetConfigs = new HashMap<>();
        presetFile = new HashMap<>();

        dataFolder.mkdirs();
        new File(dataFolder, "presets").mkdirs();
        if(savedPresets().size() == 0){
            createPresetFile("default");
            addDefaultRewardCommands("default");
        }
    }

    public void reload() {
        presetConfigs = new HashMap<>();
    }

    public void unloadPresetData(String preset) {
        if (!presetConfigs.containsKey(preset)) {
            return;
        }
        presetConfigs.remove(preset);
    }

    private void loadPresetData(String preset) {
        FileConfiguration config = getPresets(preset);
        if(!presetConfigs.containsKey(preset))
            this.presetConfigs.put(preset, config);
    }

    private File getFile(String preset) {
        if(!presetFile.containsKey(preset))
            presetFile.put(preset, new File(this.dataFolder + "/presets/", preset + ".yml"));
        return presetFile.get(preset);
    }

    public FileConfiguration getPresets(String preset) {
        File playerFile = this.getFile(preset);
        if(!presetConfigs.containsKey(preset))
            this.presetConfigs.put(preset, YamlConfiguration.loadConfiguration(playerFile));
        return presetConfigs.get(preset);
    }

    public void savePreset(String preset, FileConfiguration config) {
        try {
            config.save(this.getFile(preset));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadCommandsIntoPreset(String preset, String collection, String id){
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        FileConfiguration presets = getPresets(preset);
        if(placedEggs.contains("PlacedEggs." + id + ".Rewards.")) {
            for (String commandID : placedEggs.getConfigurationSection("PlacedEggs." + id + ".Rewards.").getKeys(false)){
                String command = placedEggs.getString("PlacedEggs." + id + ".Rewards." + commandID + ".command");
                boolean enabled = placedEggs.getBoolean("PlacedEggs." + id + ".Rewards." + commandID + ".enabled");
                boolean foundAll = placedEggs.getBoolean("PlacedEggs." + id + ".Rewards." + commandID + ".foundAll");
                presets.set("Commands." + commandID + ".command",command);
                presets.set("Commands." + commandID + ".enabled",enabled);
                presets.set("Commands." + commandID + ".foundAll",foundAll);
                savePreset(preset,presets);
            }
        }
    }

    public void loadPresetIntoEggCommands(String preset, String collection, String id){
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        FileConfiguration presets = getPresets(preset);
        placedEggs.set("PlacedEggs." + id + ".Rewards",null);
        Main.getInstance().getEggDataManager().savePlacedEggs(collection,placedEggs);
        for (String commandID : presets.getConfigurationSection("Commands.").getKeys(false)){
            String command = presets.getString("Commands." + commandID + ".command");
            boolean enabled = presets.getBoolean("Commands." + commandID + ".enabled");
            boolean foundAll = presets.getBoolean("Commands." + commandID + ".foundAll");
            placedEggs.set("PlacedEggs." + id + ".Rewards." + commandID + ".command",command);
            placedEggs.set("PlacedEggs." + id + ".Rewards." + commandID + ".enabled",enabled);
            placedEggs.set("PlacedEggs." + id + ".Rewards." + commandID + ".foundAll",foundAll);
            Main.getInstance().getEggDataManager().savePlacedEggs(collection,placedEggs);
        }
    }

    public List<String> getAllCommandsAsLore(String preset,boolean isDefault){
        List<String> lore = new ArrayList<>();
        lore.clear();
        lore.add(" ");
        lore.add("§9Commands:");
        FileConfiguration presets = getPresets(preset);
        int counter = 0;
        for (String commandID : presets.getConfigurationSection("Commands.").getKeys(false)){
            if(counter < 10)
                lore.add("§7- §b" + presets.getString("Commands." + commandID + ".command"));
            counter++;
        }
        if(counter > 10)
            lore.add("  §7§o+" + (counter-10) + " more...");
        if(isDefault){
            lore.add(" ");
            lore.add("§2This preset is selected as default preset.");
            lore.add("§2It will be loaded every time a new egg is created.");
        }
        lore.add(" ");
        lore.add("§eLEFT-CLICK to load.");
        lore.add("§eMIDDLE-CLICK to set it as default preset.");
        lore.add("§eRIGHT-CLICK to delete.");
        return lore;
    }

    public void createPresetFile(String preset) {
        FileConfiguration config = this.getPresets(preset);
        File playerFile = this.getFile(preset);
        if (!playerFile.exists()) {
            try {
                playerFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.presetConfigs.put(preset, config);
        this.loadPresetData(preset);
        this.savePreset(preset, config);
    }

    public boolean containsPreset(String preset) {
        Iterator savedPresetsIterator = this.savedPresets().iterator();

        String collection;
        do {
            if (!savedPresetsIterator.hasNext()) {
                return false;
            }

            collection = (String)savedPresetsIterator.next();
        } while(!collection.contains(preset));

        return true;
    }

    private void addDefaultRewardCommands(String preset) {
        FileConfiguration config = this.getPresets(preset);
        config.set("Commands.0.command", "tellraw %PLAYER% \"%PREFIX%&aYou found an egg. &7(&e%EGGS_FOUND%&7/&e%EGGS_MAX%&7)\"");
        config.set("Commands.0.enabled", true);
        config.set("Commands.0.foundAll", false);
        config.set("Commands.1.command", "give %PLAYER% diamond");
        config.set("Commands.1.enabled", true);
        config.set("Commands.1.foundAll", false);
        config.set("Commands.2.command", "tellraw %PLAYER% \"%PREFIX%&aYou found an egg. &7(&e%EGGS_FOUND%&7/&e%EGGS_MAX%&7)\"");
        config.set("Commands.2.enabled", true);
        config.set("Commands.2.foundAll", true);
        config.set("Commands.3.command", "give %PLAYER% diamond");
        config.set("Commands.3.enabled", true);
        config.set("Commands.3.foundAll", true);
        config.set("Commands.4.command", "tellraw %PLAYER% \"%PREFIX%&6You found all eggs!\"");
        config.set("Commands.4.enabled", true);
        config.set("Commands.4.foundAll", true);
        this.savePreset(preset, config);
    }

    public List<String> savedPresets() {
        List<String> presets = new ArrayList();
        File presetsFolder = new File(this.dataFolder + "/presets/");
        if (presetsFolder.exists() && presetsFolder.isDirectory()) {
            File[] playerFiles = presetsFolder.listFiles((dir, name) -> {
                return name.endsWith(".yml");
            });
            if (playerFiles != null) {
                int playerFilesLength = playerFiles.length;

                for(int i = 0; i < playerFilesLength; ++i) {
                    File playerFile = playerFiles[i];
                    String fileName = playerFile.getName();
                    String collectionName = fileName.substring(0, fileName.length() - 4);
                    presets.add(collectionName);
                }
            }
        }
        return presets;
    }

    public void deletePreset(String preset) {
        File presetFile = this.getFile(preset);
        if (presetFile.exists()) {
            presetFile.delete();
        }
    }
}

