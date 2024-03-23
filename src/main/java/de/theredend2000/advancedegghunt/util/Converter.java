package de.theredend2000.advancedegghunt.util;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.global.GlobalPresetDataManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.individual.IndividualPresetDataManager;
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
            if(placeEggs.contains("Rewards.")){
                for(String rewardsID : placeEggs.getConfigurationSection("Rewards.").getKeys(false)){
                    if(placeEggs.getInt("Rewards."+rewardsID+".type") == 0){
                        for(String ids : placeEggs.getConfigurationSection("PlacedEggs.").getKeys(false)){
                            IndividualPresetDataManager presetDataManager = plugin.getIndividualPresetDataManager();
                            presetDataManager.loadPresetIntoEggCommands("default", collections, ids);
                            Bukkit.getConsoleSender().sendMessage(Main.PREFIX + "§7Loaded individual preset §adefault §7into §ecollection " + collections + " §7at §begg #" + ids);
                        }
                    }else if(placeEggs.getInt("Rewards."+rewardsID+".type") == 1){
                        for(String ids : placeEggs.getConfigurationSection("PlacedEggs.").getKeys(false)){
                            GlobalPresetDataManager presetDataManager = plugin.getGlobalPresetDataManager();
                            presetDataManager.loadPresetIntoCollectionCommands("default", collections);
                            Bukkit.getConsoleSender().sendMessage(Main.PREFIX + "§7Loaded global preset §adefault §7into §ecollection " + collections + " §7at §begg #" + ids);
                        }
                    }
                }
                placeEggs.set("Rewards", null);
                plugin.getEggDataManager().savePlacedEggs(collections,placeEggs);
                Bukkit.getConsoleSender().sendMessage(Main.PREFIX + "§2UPDATED THE COMMAND SYSTEM");
                Bukkit.getConsoleSender().sendMessage(Main.PREFIX + "§2PLEASE CHECK YOUR COMMANDS!");
            }
        }
    }
}
