package de.theredend2000.advancedegghunt.util;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

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
                        eggRewardsSection.set(nextNumber + ".chance", 100);
                    }
                } else if (placedEggs.getInt("Rewards." + rewardsID + ".type") == 1) {
                    ConfigurationSection eggRewardsSection = placedEggs.getConfigurationSection("GlobalRewards");
                    if (eggRewardsSection == null) {
                        eggRewardsSection = placedEggs.createSection("GlobalRewards");
                    }

                    String command = placedEggs.getString("Rewards." + rewardsID + ".command");
                    boolean enabled = placedEggs.getBoolean("Rewards." + rewardsID + ".enabled");

                    int nextNumber = getNextRewardIndex(eggRewardsSection);

                    eggRewardsSection.set(nextNumber + ".command", command);
                    eggRewardsSection.set(nextNumber + ".enabled", enabled);
                    eggRewardsSection.set(nextNumber + ".chance", 100);
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
                ConfigurationSection rewardsSection = placedEggs.getConfigurationSection("PlacedEggs."+eggID+".Rewards.");
                if (rewardsSection == null) continue;
                for(String commandID : rewardsSection.getKeys(false)) {
                    if (!rewardsSection.contains(commandID+".chance")){
                        rewardsSection.set(commandID+".chance", 100);
                        added = true;
                    }
                }
            }
            ConfigurationSection globalRewardsSection = placedEggs.getConfigurationSection("GlobalRewards.");
            if (globalRewardsSection != null) {
                for(String commandID : globalRewardsSection.getKeys(false)) {
                    if (!globalRewardsSection.contains(commandID+".chance")){
                        globalRewardsSection.set(commandID+".chance", 100);
                        added = true;
                    }
                }
            }
            if (added) {
                plugin.getEggDataManager().savePlacedEggs(collection);
            }
        }
        for(String gPresets : plugin.getGlobalPresetDataManager().savedPresets()){
            Map<String, Object> presets = plugin.getGlobalPresetDataManager().getPresets(gPresets);
            for(Map.Entry<String, Object> entry : presets.entrySet()) {
                Map<String, Object> commandInfo = (Map<String, Object>) entry.getValue();
                if(!commandInfo.containsKey("chance")){
                    commandInfo.put("chance", 100.0);
                    added = true;
                }
            }
            if (added) {
                plugin.getGlobalPresetDataManager().savePreset(gPresets, presets);
            }
        }
        for(String iPresets : plugin.getIndividualPresetDataManager().savedPresets()){
            Map<String, Object> presets = plugin.getIndividualPresetDataManager().getPresets(iPresets);
            for(Map.Entry<String, Object> entry : presets.entrySet()) {
                Map<String, Object> commandInfo = (Map<String, Object>) entry.getValue();
                if(!commandInfo.containsKey("chance")){
                    commandInfo.put("chance", 100.0);
                    added = true;
                }
            }
            if (added) {
                plugin.getIndividualPresetDataManager().savePreset(iPresets, presets);
            }
        }
        if(added){
            messageManager.sendMessage(Bukkit.getConsoleSender(), MessageKey.COMMAND_SYSTEM_CHANCES_UPDATED);
            messageManager.sendMessage(Bukkit.getConsoleSender(), MessageKey.COMMAND_SYSTEM_CHANCES_CONTAINS);
        }
    }
}
