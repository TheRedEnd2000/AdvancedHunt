package de.theredend2000.advancedegghunt.util;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.global.GlobalPresetDataManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.individual.IndividualPresetDataManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Set;

public class Converter {

    private Main plugin;

    public Converter(){
        this.plugin = Main.getInstance();
    }

    public void convertAllSystems(){
        convertToNewCommandSystem();
    }

    private void convertToNewCommandSystem() {
        for (String collection : plugin.getEggDataManager().savedEggCollections()) {
            FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
            if (placedEggs.contains("Rewards.") && placedEggs.contains("PlacedEggs.")) {
                ConfigurationSection rewardsSection = placedEggs.getConfigurationSection("Rewards.");
                ConfigurationSection placedEggsSection = placedEggs.getConfigurationSection("PlacedEggs.");

                if (rewardsSection != null && placedEggsSection != null) {
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
                    plugin.getEggDataManager().savePlacedEggs(collection, placedEggs);

                    Bukkit.getConsoleSender().sendMessage(Main.PREFIX + "§2UPDATED THE COMMAND SYSTEM");
                    Bukkit.getConsoleSender().sendMessage(Main.PREFIX + "§2PLEASE CHECK YOUR COMMANDS!");
                }
            }
        }
    }

    private int getNextRewardIndex(ConfigurationSection rewardsSection) {
        int maxIndex = -1;
        if (rewardsSection != null) {
            for (String key : rewardsSection.getKeys(false)) {
                try {
                    int index = Integer.parseInt(key);
                    maxIndex = Math.max(maxIndex, index);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return maxIndex + 1;
    }
}
