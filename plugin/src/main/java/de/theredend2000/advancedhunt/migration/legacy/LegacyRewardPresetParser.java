package de.theredend2000.advancedhunt.migration.legacy;

import de.theredend2000.advancedhunt.model.Reward;
import de.theredend2000.advancedhunt.model.RewardPreset;
import de.theredend2000.advancedhunt.model.RewardPresetType;
import de.theredend2000.advancedhunt.model.RewardType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Parses legacy reward presets from the presets/global and presets/individual folders.
 */
public final class LegacyRewardPresetParser {

    private LegacyRewardPresetParser() {
    }

    /**
     * Parses all reward presets from the legacy folder structure.
     *
     * @param legacyRootFolder The plugin data folder containing legacy data
     * @return List of parsed reward presets
     */
    public static List<RewardPreset> parseAll(File legacyRootFolder) {
        List<RewardPreset> presets = new ArrayList<>();

        File presetsDir = new File(legacyRootFolder, "presets");
        if (!presetsDir.exists() || !presetsDir.isDirectory()) {
            return presets;
        }

        // Parse global presets (collection completion rewards)
        File globalDir = new File(presetsDir, "global");
        presets.addAll(parsePresetsFromDir(globalDir, RewardPresetType.COLLECTION));

        // Parse individual presets (per-treasure rewards)
        File individualDir = new File(presetsDir, "individual");
        presets.addAll(parsePresetsFromDir(individualDir, RewardPresetType.TREASURE));

        return presets;
    }

    private static List<RewardPreset> parsePresetsFromDir(File dir, RewardPresetType type) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return Collections.emptyList();
        }

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        List<RewardPreset> presets = new ArrayList<>();
        for (File file : files) {
            RewardPreset preset = parsePresetFile(file, type);
            if (preset != null) {
                presets.add(preset);
            }
        }
        return presets;
    }

    private static RewardPreset parsePresetFile(File file, RewardPresetType type) {
        String name = file.getName();
        if (name.toLowerCase().endsWith(".yml")) {
            name = name.substring(0, name.length() - 4);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection commandsSection = cfg.getConfigurationSection("Commands");

        List<Reward> rewards = parseCommandRewards(commandsSection);

        return new RewardPreset(UUID.randomUUID(), type, name, rewards);
    }

    private static List<Reward> parseCommandRewards(ConfigurationSection commands) {
        if (commands == null) {
            return Collections.emptyList();
        }

        List<Reward> rewards = new ArrayList<>();
        for (String key : commands.getKeys(false)) {
            ConfigurationSection r = commands.getConfigurationSection(key);
            if (r == null) continue;

            boolean enabled = r.getBoolean("enabled", true);
            if (!enabled) {
                continue;
            }

            double chance = r.getDouble("chance", 100.0);
            String command = r.getString("command");
            if (command == null || command.trim().isEmpty()) {
                continue;
            }

            command = migrateLegacyPlaceholders(command);

            rewards.add(new Reward(RewardType.COMMAND, chance, null, null, command));
        }

        return rewards;
    }

    /**
     * Migrates legacy placeholder names to their modern equivalents.
     * Normalizes all placeholders to lowercase format.
     * 
     * @param command The command string containing placeholders
     * @return The command with migrated and normalized placeholders
     */
    private static String migrateLegacyPlaceholders(String command) {
        return command
                .replace("%EGGS_FOUND%", "%found_treasures%")
                .replace("%EGGS_MAX%", "%max_treasures%")
                .replace("%TREASURES_FOUND%", "%found_treasures%")
                .replace("%TREASURES_MAX%", "%max_treasures%")
                .replace("%PLAYER%", "%player%")
                .replace("%PREFIX%", "%prefix%");
    }
}
