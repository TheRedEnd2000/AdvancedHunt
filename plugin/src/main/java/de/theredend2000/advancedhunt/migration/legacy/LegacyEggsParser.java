package de.theredend2000.advancedhunt.migration.legacy;

import de.theredend2000.advancedhunt.model.Reward;
import de.theredend2000.advancedhunt.model.RewardType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class LegacyEggsParser {

    public static final class LegacyPlacedEgg {
        public final String world;
        public final int x;
        public final int y;
        public final int z;
        public final List<Reward> rewards;

        private LegacyPlacedEgg(String world, int x, int y, int z, List<Reward> rewards) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.rewards = rewards;
        }
    }

    public static final class LegacyRequirements {
        public final String order; // OR/AND
        public final ConfigurationSection raw; // keep raw for high-fidelity translation

        private LegacyRequirements(String order, ConfigurationSection raw) {
            this.order = order;
            this.raw = raw;
        }
    }

    public static final class LegacyReset {
        public final int year;
        public final int month;
        public final int date;
        public final int hour;
        public final int minute;
        public final int second;

        private LegacyReset(int year, int month, int date, int hour, int minute, int second) {
            this.year = year;
            this.month = month;
            this.date = date;
            this.hour = hour;
            this.minute = minute;
            this.second = second;
        }
    }

    public static final class LegacyCollection {
        public final String name;
        public final boolean enabled;
        public final boolean onePlayer;
        public final int maxEggs;
        public final LegacyReset reset;
        public final List<Reward> globalRewards;
        public final List<LegacyPlacedEgg> placedEggs;
        public final LegacyRequirements requirements;

        private LegacyCollection(String name,
                                 boolean enabled,
                                 boolean onePlayer,
                                 int maxEggs,
                                 LegacyReset reset,
                                 List<Reward> globalRewards,
                                 List<LegacyPlacedEgg> placedEggs,
                                 LegacyRequirements requirements) {
            this.name = name;
            this.enabled = enabled;
            this.onePlayer = onePlayer;
            this.maxEggs = maxEggs;
            this.reset = reset;
            this.globalRewards = globalRewards;
            this.placedEggs = placedEggs;
            this.requirements = requirements;
        }
    }

    private LegacyEggsParser() {
    }

    public static List<LegacyCollection> parseAll(File legacyRootFolder) {
        File eggsDir = new File(legacyRootFolder, "eggs");
        if (!eggsDir.exists() || !eggsDir.isDirectory()) {
            return Collections.emptyList();
        }

        File[] files = eggsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        List<LegacyCollection> out = new ArrayList<>();
        for (File f : files) {
            String name = f.getName();
            if (name.toLowerCase().endsWith(".yml")) {
                name = name.substring(0, name.length() - 4);
            }
            LegacyCollection parsed = parseCollectionFile(name, f);
            if (parsed != null) {
                out.add(parsed);
            }
        }
        return out;
    }

    private static LegacyCollection parseCollectionFile(String collectionName, File file) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        boolean enabled = cfg.getBoolean("Enabled", true);
        boolean onePlayer = cfg.getBoolean("OnePlayer", false);
        int maxEggs = cfg.getInt("MaxEggs", 0);

        ConfigurationSection resetSec = cfg.getConfigurationSection("Reset");
        LegacyReset reset = null;
        if (resetSec != null) {
            reset = new LegacyReset(
                resetSec.getInt("Year", 0),
                resetSec.getInt("Month", 0),
                resetSec.getInt("Date", 0),
                resetSec.getInt("Hour", 0),
                resetSec.getInt("Minute", 0),
                resetSec.getInt("Second", 0)
            );
        }

        List<Reward> globalRewards = parseCommandRewards(cfg.getConfigurationSection("GlobalRewards"));
        List<LegacyPlacedEgg> placed = parsePlacedEggs(cfg.getConfigurationSection("PlacedEggs"));

        String order = cfg.getString("RequirementsOrder", "OR");
        ConfigurationSection reqSec = cfg.getConfigurationSection("Requirements");
        LegacyRequirements req = new LegacyRequirements(order, reqSec);

        return new LegacyCollection(collectionName, enabled, onePlayer, maxEggs, reset, globalRewards, placed, req);
    }

    private static List<LegacyPlacedEgg> parsePlacedEggs(ConfigurationSection sec) {
        if (sec == null) {
            return Collections.emptyList();
        }

        List<LegacyPlacedEgg> out = new ArrayList<>();
        for (String key : sec.getKeys(false)) {
            ConfigurationSection egg = sec.getConfigurationSection(key);
            if (egg == null) continue;

            String world = egg.getString("World");
            int x = egg.getInt("X");
            int y = egg.getInt("Y");
            int z = egg.getInt("Z");

            List<Reward> rewards = parseCommandRewards(egg.getConfigurationSection("Rewards"));
            if (world == null || world.trim().isEmpty()) {
                continue;
            }

            out.add(new LegacyPlacedEgg(world, x, y, z, rewards));
        }
        return out;
    }

    private static List<Reward> parseCommandRewards(ConfigurationSection rewards) {
        if (rewards == null) {
            return Collections.emptyList();
        }

        List<Reward> out = new ArrayList<>();
        for (String key : rewards.getKeys(false)) {
            ConfigurationSection r = rewards.getConfigurationSection(key);
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

            // Legacy rewards are command-based.
            out.add(new Reward(RewardType.COMMAND, chance, null, null, command));
        }

        // Preserve numeric ordering if possible
        out.removeIf(Objects::isNull);
        return out;
    }
}
