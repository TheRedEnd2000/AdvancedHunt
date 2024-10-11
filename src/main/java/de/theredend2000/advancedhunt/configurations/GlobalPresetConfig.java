package de.theredend2000.advancedhunt.configurations;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class GlobalPresetConfig extends MultiFileConfiguration {
    private static final TreeMap<Double, ConfigUpgrader> upgraders = new TreeMap<>();
    private MessageManager messageManager;

    public GlobalPresetConfig(JavaPlugin plugin) {
        super(plugin, "presets/global", "yml", 1.0);
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
     * Loads commands from a collection into a preset.
     * @param preset The name of the preset.
     * @param collection The collection name.
     * @param placedEggs The configuration of placed eggs.
     */
    public void loadCommandsIntoPreset(String preset, String collection, ConfigurationSection placedEggs) {
        if (placedEggs.contains("GlobalRewards.")) {
            for (String commandID : placedEggs.getConfigurationSection("GlobalRewards.").getKeys(false)) {
                String command = placedEggs.getString("GlobalRewards." + commandID + ".command");
                boolean enabled = placedEggs.getBoolean("GlobalRewards." + commandID + ".enabled");
                double chance = placedEggs.getDouble("GlobalRewards." + commandID + ".chance");
                set(preset, "Commands." + commandID + ".command", command);
                set(preset, "Commands." + commandID + ".enabled", enabled);
                set(preset, "Commands." + commandID + ".chance", chance);
            }
            saveConfig(preset);
        }
    }

    /**
     * Loads a preset into a collection's commands.
     * @param preset The name of the preset.
     * @param collection The collection name.
     * @return A map of command data.
     */
    public Map<String, Object> loadPresetIntoCollectionCommands(String preset) {
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
        messageManager = Main.getInstance().getMessageManager();
        List<String> commandLore = new ArrayList<>();
        List<String> defaultLore = new ArrayList<>();

        ConfigurationSection commands = getConfig(preset).getConfigurationSection("Commands.");
        if (commands != null) {
            int counter = 0;
            for (String commandID : commands.getKeys(false)) {
                if (counter < 10) {
                    commandLore.add("§7- §b" + commands.getString(commandID + ".command"));
                }
                counter++;
            }
            if (counter > 10) {
                commandLore.add("  §7§o+" + (counter - 10) + " " + messageManager.getMessage(MessageKey.REQUIREMENTS_MORE) + "...");
            }
        }
        if (isDefault) {
            defaultLore.add(" ");
            defaultLore.add(messageManager.getMessage(MessageKey.PRESET_IS_DEFAULT_1));
            defaultLore.add(messageManager.getMessage(MessageKey.PRESET_IS_DEFAULT_2));
        }
        List<String> finalLore = new ArrayList<>();
        List<String> globalLoreTemplate = Main.getInstance().getMenuManager().getMenuItemLore(MenuMessageKey.PRESET_GLOBAL);

        for (String line : globalLoreTemplate) {
            if (line.contains("%COMMANDS%")) {
                finalLore.addAll(commandLore);
            }
            else if (line.contains("%IS_DEFAULT%")) {
                finalLore.addAll(defaultLore);
            } else {
                finalLore.add(line);
            }
        }

        return finalLore;
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
        set(preset, "Commands.0.command", "tellraw %PLAYER% \"%PREFIX%&6You found all "+Main.getInstance().getPluginConfig().getPluginNamePlural()+"!\"");
        set(preset, "Commands.0.enabled", true);
        set(preset, "Commands.0.chance", 100);
        set(preset, "Commands.1.command", "minecraft:give %PLAYER% diamond");
        set(preset, "Commands.1.enabled", true);
        set(preset, "Commands.1.chance", 100);
        saveConfig(preset);
    }
}
