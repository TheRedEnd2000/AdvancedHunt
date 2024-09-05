package de.theredend2000.advancedegghunt.util;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class Converter {

    private Main plugin;

    private MessageManager messageManager;

    public Converter(){
        this.messageManager = new MessageManager();
        this.plugin = Main.getInstance();
    }

    public void convertAllSystems(){
        convertToNewCommandSystem();
        addChances();
    }

    private void convertToNewCommandSystem() {
        for (String collection : plugin.getEggDataManager().savedEggCollections()) {
            FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
            if (!placedEggs.contains("Rewards.") || !placedEggs.contains("PlacedEggs.")) {
                continue;
            }
            ConfigurationSection rewardsSection = placedEggs.getConfigurationSection("Rewards.");
            ConfigurationSection placedEggsSection = placedEggs.getConfigurationSection("PlacedEggs.");

            if (rewardsSection == null || placedEggsSection == null) {
                continue;
            }
            for (String rewardsID : rewardsSection.getKeys(false)) {
                if (placedEggs.getInt("Rewards." + rewardsID + ".type") == 0) {
                    for (String eggID : placedEggsSection.getKeys(false)) {
                        ConfigurationSection eggRewardsSection = placedEggs.getConfigurationSection("PlacedEggs." + eggID + ".Rewards");
                        if (eggRewardsSection == null) {
                            eggRewardsSection = placedEggs.createSection("PlacedEggs." + eggID + ".Rewards");
                        }

                        String command = placedEggs.getString("Rewards." + rewardsID + ".command");
                        boolean enabled = placedEggs.getBoolean("Rewards." + rewardsID + ".enabled");

                        int nextNumber = getNextRewardIndex(eggRewardsSection);

                        eggRewardsSection.set(nextNumber + ".command", command);
                        eggRewardsSection.set(nextNumber + ".enabled", enabled);
                    }
                }else if (placedEggs.getInt("Rewards." + rewardsID + ".type") == 1) {
                    ConfigurationSection eggRewardsSection = placedEggs.getConfigurationSection("GlobalRewards");
                    if (eggRewardsSection == null) {
                        eggRewardsSection = placedEggs.createSection("GlobalRewards");
                    }

                    String command = placedEggs.getString("Rewards." + rewardsID + ".command");
                    boolean enabled = placedEggs.getBoolean("Rewards." + rewardsID + ".enabled");

                    int nextNumber = getNextRewardIndex(eggRewardsSection);

                    eggRewardsSection.set(nextNumber + ".command", command);
                    eggRewardsSection.set(nextNumber + ".enabled", enabled);
                }
            }

            placedEggs.set("Rewards", null);
            plugin.getEggDataManager().savePlacedEggs(collection);

            messageManager.sendMessage(Bukkit.getConsoleSender(), MessageKey.COMMAND_SYSTEM_UPDATED);
            messageManager.sendMessage(Bukkit.getConsoleSender(), MessageKey.COMMAND_SYSTEM_CHECK);
        }
    }

    private int getNextRewardIndex(ConfigurationSection rewardsSection) {
        int maxIndex = -1;
        if (rewardsSection == null) {
            return maxIndex + 1;
        }
        for (String key : rewardsSection.getKeys(false)) {
            try {
                int index = Integer.parseInt(key);
                maxIndex = Math.max(maxIndex, index);
            } catch (NumberFormatException ignored) {
            }
        }
        return maxIndex + 1;
    }

    public void addChances(){
        boolean added = false;
        for (String collection : plugin.getEggDataManager().savedEggCollections()) {
            FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
            if (!placedEggs.contains("PlacedEggs.")) continue;
            ConfigurationSection placedEggsSection = placedEggs.getConfigurationSection("PlacedEggs.");

            if (placedEggsSection == null) continue;

            for(String eggID : placedEggsSection.getKeys(false)){
                for(String commandID : placedEggs.getConfigurationSection("PlacedEggs."+eggID+".Rewards.").getKeys(false)) {
                    if (!placedEggs.contains("PlacedEggs." + eggID + ".Rewards." + commandID+".chance")){
                        placedEggs.set("PlacedEggs." + eggID + ".Rewards." + commandID+".chance",100);
                        plugin.getEggDataManager().savePlacedEggs(collection);
                        added = true;
                    }
                }
            }
            for(String commandID : placedEggs.getConfigurationSection("GlobalRewards.").getKeys(false)) {
                if (!placedEggs.contains("GlobalRewards." + commandID+".chance")){
                    placedEggs.set("GlobalRewards." + commandID+".chance",100);
                    plugin.getEggDataManager().savePlacedEggs(collection);
                    added = true;
                }
            }
        }
        for(String gPresets : plugin.getGlobalPresetDataManager().savedPresets()){
            FileConfiguration presets = plugin.getGlobalPresetDataManager().getPresets(gPresets);
            if (!presets.contains("Commands.")) continue;
            for(String commandID : presets.getConfigurationSection("Commands.").getKeys(false)) {
                if(!presets.contains("Commands."+commandID+".chance")){
                    presets.set("Commands."+commandID+".chance",100);
                    plugin.getGlobalPresetDataManager().savePreset(gPresets,presets);
                    added = true;
                }
            }
        }
        for(String iPresets : plugin.getIndividualPresetDataManager().savedPresets()){
            FileConfiguration presets = plugin.getIndividualPresetDataManager().getPresets(iPresets);
            if (!presets.contains("Commands.")) continue;
            for(String commandID : presets.getConfigurationSection("Commands.").getKeys(false)) {
                if(!presets.contains("Commands."+commandID+".chance")){
                    presets.set("Commands."+commandID+".chance",100);
                    plugin.getIndividualPresetDataManager().savePreset(iPresets,presets);
                    added = true;
                }
            }
        }
        if(added){
            messageManager.sendMessage(Bukkit.getConsoleSender(), MessageKey.COMMAND_SYSTEM_CHANCES_UPDATED);
            messageManager.sendMessage(Bukkit.getConsoleSender(), MessageKey.COMMAND_SYSTEM_CHANCES_CONTAINS);
        }
    }
}
