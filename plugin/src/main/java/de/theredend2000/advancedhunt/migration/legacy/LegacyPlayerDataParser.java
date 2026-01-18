package de.theredend2000.advancedhunt.migration.legacy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class LegacyPlayerDataParser {

    public static final class LegacyFoundEgg {
        public final String collectionName;
        public final String world;
        public final int x;
        public final int y;
        public final int z;

        private LegacyFoundEgg(String collectionName, String world, int x, int y, int z) {
            this.collectionName = collectionName;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static final class LegacyPlayerData {
        public final UUID playerUuid;
        public final List<LegacyFoundEgg> found;

        private LegacyPlayerData(UUID playerUuid, List<LegacyFoundEgg> found) {
            this.playerUuid = playerUuid;
            this.found = found;
        }
    }

    private LegacyPlayerDataParser() {
    }

    public static List<LegacyPlayerData> parseAll(File legacyRootFolder) {
        File playerDir = new File(legacyRootFolder, "playerdata");
        if (!playerDir.exists() || !playerDir.isDirectory()) {
            return Collections.emptyList();
        }

        File[] files = playerDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        List<LegacyPlayerData> out = new ArrayList<>();
        for (File f : files) {
            String base = f.getName();
            if (base.toLowerCase().endsWith(".yml")) {
                base = base.substring(0, base.length() - 4);
            }

            UUID uuid;
            try {
                uuid = UUID.fromString(base);
            } catch (Exception ignored) {
                continue;
            }

            out.add(parseOne(uuid, f));
        }

        return out;
    }

    private static LegacyPlayerData parseOne(UUID playerUuid, File file) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection foundEggs = cfg.getConfigurationSection("FoundEggs");
        if (foundEggs == null) {
            return new LegacyPlayerData(playerUuid, Collections.emptyList());
        }

        List<LegacyFoundEgg> found = new ArrayList<>();
        for (String collectionName : foundEggs.getKeys(false)) {
            ConfigurationSection collSec = foundEggs.getConfigurationSection(collectionName);
            if (collSec == null) continue;

            for (String key : collSec.getKeys(false)) {
                // Skip meta keys
                if ("Count".equalsIgnoreCase(key) || "Name".equalsIgnoreCase(key)) {
                    continue;
                }

                ConfigurationSection entry = collSec.getConfigurationSection(key);
                if (entry == null) continue;

                String world = entry.getString("World");
                int x = entry.getInt("X");
                int y = entry.getInt("Y");
                int z = entry.getInt("Z");

                if (world == null || world.trim().isEmpty()) {
                    continue;
                }

                found.add(new LegacyFoundEgg(collectionName, world, x, y, z));
            }
        }

        return new LegacyPlayerData(playerUuid, found);
    }
}
