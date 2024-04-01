package de.theredend2000.advancedhunt.managers.inventorymanager.eggrewards.global;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.configurations.GlobalPresetConfig;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;

public class GlobalPresetDataManager {

    private final Main plugin;
    private final GlobalPresetConfig presetConfig;

    public GlobalPresetDataManager(Main plugin) {
        this.plugin = plugin;
        this.presetConfig = new GlobalPresetConfig(plugin);

        if (savedPresets().isEmpty()) {
            presetConfig.addDefaultRewardCommands("default");
        }
    }

    public void reload() {
        presetConfig.reloadConfigs();
    }

    public void loadCommandsIntoPreset(String preset, String collection) {
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        presetConfig.loadCommandsIntoPreset(preset, collection, placedEggs);
    }

    public void loadPresetIntoCollectionCommands(String preset, String collection) {
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        Map<String, Object> commandData = presetConfig.loadPresetIntoCollectionCommands(preset);

        placedEggs.set("GlobalRewards", null);

        for (Map.Entry<String, Object> entry : commandData.entrySet()) {
            String commandID = entry.getKey();
            Map<String, Object> commandInfo = (Map<String, Object>) entry.getValue();
            placedEggs.set("GlobalRewards." + commandID + ".command", commandInfo.get("command"));
            placedEggs.set("GlobalRewards." + commandID + ".enabled", commandInfo.get("enabled"));
            placedEggs.set("GlobalRewards." + commandID + ".chance", commandInfo.get("chance"));
        }

        plugin.getEggDataManager().savePlacedEggs(collection);
    }

    public List<String> getAllCommandsAsLore(String preset, boolean isDefault) {
        return presetConfig.getAllCommandsAsLore(preset, isDefault);
    }

    public boolean containsPreset(String preset) {
        return presetConfig.containsConfig(preset);
    }

    public List<String> savedPresets() {
        return presetConfig.savedPresets();
    }

    public void deletePreset(String preset) {
        presetConfig.deleteConfig(preset);
    }

    public void savePreset(String preset, Map<String, Object> commandData) {
        for (Map.Entry<String, Object> entry : commandData.entrySet()) {
            String commandID = entry.getKey();
            Map<String, Object> commandInfo = (Map<String, Object>) entry.getValue();
            presetConfig.set(preset, "Commands." + commandID + ".command", commandInfo.get("command"));
            presetConfig.set(preset, "Commands." + commandID + ".enabled", commandInfo.get("enabled"));
            presetConfig.set(preset, "Commands." + commandID + ".chance", commandInfo.get("chance"));
        }
        presetConfig.saveConfig(preset);
    }

    public void createPresetFile(String preset) {
        if (!containsPreset(preset)) {
            presetConfig.addDefaultRewardCommands(preset);
        }
    }

    public void addDefaultRewardCommands(String preset) {
        presetConfig.addDefaultRewardCommands(preset);
    }

    public Map<String, Object> getPresets(String preset) {
        return presetConfig.loadPresetIntoCollectionCommands(preset);
    }
}

