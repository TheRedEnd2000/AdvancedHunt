package de.theredend2000.advancedegghunt.configurations;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class IndividualPresetConfig extends MultiFileConfiguration {
    private static final TreeMap<Double, ConfigUpgrader> upgraders = new TreeMap<>();

    public IndividualPresetConfig(JavaPlugin plugin) {
        super(plugin, "presets/individual", "yml", 1);
    }

    @Override
    public TreeMap<Double, ConfigUpgrader> getUpgrader() {
        return upgraders;
    }

    @Override
    public void registerUpgrader() {
        // Register upgraders if needed
    }

    /**
     * Saves the preset data to the configuration file.
     * @param preset The name of the preset to save.
     */
    public void savePreset(String preset) {
        saveConfig(preset);
    }

    /**
     * Loads commands from a placed egg into a preset.
     * @param preset The name of the preset.
     * @param collection The collection name.
     * @param id The egg ID.
     * @param placedEggs The configuration of placed eggs.
     */
    public void loadCommandsIntoPreset(String preset, String collection, String id, ConfigurationSection placedEggs) {
        if (placedEggs.contains("PlacedEggs." + id + ".Rewards.")) {
            ConfigurationSection rewards = placedEggs.getConfigurationSection("PlacedEggs." + id + ".Rewards.");
            for (String commandID : rewards.getKeys(false)) {
                String command = rewards.getString(commandID + ".command");
                boolean enabled = rewards.getBoolean(commandID + ".enabled");
                double chance = rewards.getDouble(commandID + ".chance");
                set(preset, "Commands." + commandID + ".command", command);
                set(preset, "Commands." + commandID + ".enabled", enabled);
                set(preset, "Commands." + commandID + ".chance", chance);
            }
            savePreset(preset);
        }
    }

    /**
     * Loads a preset into an egg's commands.
     * @param preset The name of the preset.
     * @param id The egg ID.
     * @return A map of command data.
     */
    public Map<String, Object> loadPresetIntoEggCommands(String preset, String id) {
        Map<String, Object> commandData = new TreeMap<>();
        ConfigurationSection commands = getConfig(preset).getConfigurationSection("Commands.");
        if (commands != null) {
            for (String commandID : commands.getKeys(false)) {
                String command = commands.getString(commandID + ".command");
                boolean enabled = commands.getBoolean(commandID + ".enabled");
                double chance = commands.getDouble(commandID + ".chance");
                Map<String, Object> commandInfo = new TreeMap<>();
                commandInfo.put("command", command);
                commandInfo.put("enabled", enabled);
                commandInfo.put("chance", chance);
                commandData.put(commandID, commandInfo);
            }
        }
        return commandData;
    }

    /**
     * Gets all commands as a lore list.
     * @param preset The name of the preset.
     * @param isDefault Whether this preset is the default.
     * @return A list of lore strings.
     */
    public List<String> getAllCommandsAsLore(String preset, boolean isDefault) {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add("§9Commands:");
        ConfigurationSection commands = getConfig(preset).getConfigurationSection("Commands.");
        if (commands != null) {
            int counter = 0;
            for (String commandID : commands.getKeys(false)) {
                if (counter < 10)
                    lore.add("§7- §b" + commands.getString(commandID + ".command"));
                counter++;
            }
            if (counter > 10)
                lore.add("  §7§o+" + (counter - 10) + " more...");
        }
        if (isDefault) {
            lore.add(" ");
            lore.add("§2This preset is selected as default preset.");
            lore.add("§2It will be loaded every time a new egg is created.");
        }
        lore.add(" ");
        lore.add("§eLEFT-CLICK to load.");
        lore.add("§eMIDDLE-CLICK to set it as default preset.");
        lore.add("§eRIGHT-CLICK to delete.");
        lore.add("§eDROP to load it in all placed eggs.");
        return lore;
    }

    /**
     * Returns a list of all saved presets.
     * @return A list of preset names.
     */
    public List<String> savedPresets() {
        File folder = new File(getDataFolder(), configFolder);
        String[] files = folder.list((dir, name) -> name.endsWith(fileExtension));
        List<String> presets = new ArrayList<>();
        if (files != null) {
            for (String file : files) {
                presets.add(file.substring(0, file.length() - fileExtension.length()));
            }
        }
        return presets;
    }

    /**
     * Adds default reward commands to a preset.
     * @param preset The name of the preset.
     */
    public void addDefaultRewardCommands(String preset) {
        set(preset, "Commands.0.command", "tellraw %PLAYER% \"%PREFIX%&aYou found an egg. &7(&e%EGGS_FOUND%&7/&e%EGGS_MAX%&7)\"");
        set(preset, "Commands.0.enabled", true);
        set(preset, "Commands.0.chance", 100);
        set(preset, "Commands.1.command", "minecraft:give %PLAYER% diamond");
        set(preset, "Commands.1.enabled", true);
        set(preset, "Commands.1.chance", 100);
        savePreset(preset);
    }
}
