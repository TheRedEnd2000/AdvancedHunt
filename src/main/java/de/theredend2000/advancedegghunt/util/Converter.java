package de.theredend2000.advancedegghunt.util;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.PresetDataManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

public class Converter {

    private Main plugin;

    public Converter(){
        this.plugin = Main.getInstance();
    }

    public void convertAllSystems(){
        convertToNewCommandSystem();
    }

    private void convertToNewCommandSystem(){
        for(String collections : plugin.getEggDataManager().savedEggCollections()){
            FileConfiguration placeEggs = plugin.getEggDataManager().getPlacedEggs(collections);
            if(placeEggs.contains("Rewards")){
                placeEggs.set("Rewards", null);
                for(String ids : placeEggs.getConfigurationSection("PlacedEggs.").getKeys(false)){
                    PresetDataManager presetDataManager = plugin.getPresetDataManager();
                    presetDataManager.loadPresetIntoEggCommands("default", collections, ids);
                    Bukkit.getConsoleSender().sendMessage(Main.PREFIX + "§7Loaded preset §adefault §7into §ecollection " + collections + " §7at §begg #" + ids);
                }
                Bukkit.getConsoleSender().sendMessage(Main.PREFIX + "§2UPDATED THE COMMAND SYSTEM");
                Bukkit.getConsoleSender().sendMessage(Main.PREFIX + "§2PLEASE CHECK YOUR COMMANDS!");
            }
        }
    }
}
