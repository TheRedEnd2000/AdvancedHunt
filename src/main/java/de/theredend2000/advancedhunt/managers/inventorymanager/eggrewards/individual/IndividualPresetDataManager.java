package de.theredend2000.advancedhunt.managers.inventorymanager.eggrewards.individual;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.configurations.IndividualPresetConfig;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IndividualPresetDataManager {

    private final Main plugin;
    private final IndividualPresetConfig presetConfig;
    private boolean isRunning;

    public IndividualPresetDataManager(Main plugin) {
        this.plugin = plugin;
        this.presetConfig = new IndividualPresetConfig(plugin);

        if (savedPresets().isEmpty()) {
            presetConfig.addDefaultRewardCommands("default");
        }
    }

    public void reload() {
        presetConfig.reloadConfigs();
    }

    public void loadCommandsIntoPreset(String preset, String collection, String id) {
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        presetConfig.loadCommandsIntoPreset(preset, collection, id, placedEggs);
    }

    public void loadPresetIntoAllEggs(String preset, String collection, Player player) {
        var messageManager = plugin.getMessageManager();
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        ArrayList<String> ids = new ArrayList<>(placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false));
        if (isRunning) {
            messageManager.sendMessage(player, MessageKey.PRESET_LOADING_NO);
            return;
        }
        new BukkitRunnable() {
            int count = 0;
            int max = ids.size();
            @Override
            public void run() {
                if (count == max || ids.isEmpty()) {
                    messageManager.sendMessage(player, MessageKey.PRESET_LOADING_SUCCESS, "%PRESET%", preset);
                    cancel();
                    isRunning = false;
                    return;
                }
                loadPresetIntoEggCommands(preset, collection, ids.get(0));
                ids.remove(0);
                count++;
                messageManager.sendMessage(player, MessageKey.PRESET_LOADING_PROGRESS, "%COUNT%", String.valueOf(count), "%MAX%", String.valueOf(max));
                isRunning = true;
            }
        }.runTaskTimer(plugin, 0, 3);
    }

    public void loadPresetIntoEggCommands(String preset, String collection, String id) {
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        Map<String, Object> commandData = presetConfig.loadPresetIntoEggCommands(preset, id);

        placedEggs.set("PlacedEggs." + id + ".Rewards", null);

        for (Map.Entry<String, Object> entry : commandData.entrySet()) {
            String commandID = entry.getKey();
            Map<String, Object> commandInfo = (Map<String, Object>) entry.getValue();
            placedEggs.set("PlacedEggs." + id + ".Rewards." + commandID + ".command", commandInfo.get("command"));
            placedEggs.set("PlacedEggs." + id + ".Rewards." + commandID + ".enabled", commandInfo.get("enabled"));
            placedEggs.set("PlacedEggs." + id + ".Rewards." + commandID + ".chance", commandInfo.get("chance"));
        }

        Main.getInstance().getEggDataManager().savePlacedEggs(collection);
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
        presetConfig.savePreset(preset);
    }

    public void createPresetFile(String preset) {
        if (!containsPreset(preset)) {
            presetConfig.set(preset,"Commands","Contact support if exists!");
            presetConfig.saveConfig(preset);
        }
    }

    public Map<String, Object> getPresets(String preset) {
        return presetConfig.loadPresetIntoEggCommands(preset, "");
    }
}

