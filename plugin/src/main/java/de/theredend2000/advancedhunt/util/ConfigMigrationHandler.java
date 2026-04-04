package de.theredend2000.advancedhunt.util;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Handles version-specific migration logic for configuration files.
 * This class is responsible for modifying the in-memory configuration
 * before the ConfigUpdater applies the new structure from the JAR.
 */
public class ConfigMigrationHandler {

    private static final VersionComparator VERSION_COMPARATOR = new VersionComparator();
    private static final NavigableMap<String, Consumer<FileConfiguration>> configMigrations = new TreeMap<>(VERSION_COMPARATOR);
    private static final NavigableMap<String, Consumer<FileConfiguration>> messageMigrations = new TreeMap<>(VERSION_COMPARATOR);

    static {
        // Register config migrations here
        // Key is the target version (the version we are upgrading TO)
        
        // Example: Upgrade to version 4.1
        // configMigrations.put("4.1", config -> {
        //     if (config.contains("old.key")) {
        //         config.set("new.key", config.get("old.key"));
        //         config.set("old.key", null);
        //     }
        // });
    }

    static {
        // Register message migrations here
        
        // Example: Upgrade to version 2.1
        // messageMigrations.put("2.1", config -> {
        //     if (config.contains("typo.key")) {
        //         config.set("fixed.key", config.get("typo.key"));
        //         config.set("typo.key", null);
        //     }
        // });
    }

    /**
     * Migrates the main config.yml.
     *
     * @param config         The current user configuration.
     * @param currentVersion The version of the user's configuration.
     * @param newVersion     The target version from the JAR.
     */
    public static void migrateConfig(FileConfiguration config, String currentVersion, String newVersion) {
        applyMigrations(config, currentVersion, newVersion, configMigrations);
    }

    /**
     * Migrates message files (e.g., messages_en.yml).
     *
     * @param config         The current user configuration.
     * @param currentVersion The version of the user's configuration.
     * @param newVersion     The target version from the JAR.
     */
    public static void migrateMessages(FileConfiguration config, String currentVersion, String newVersion) {
        applyMigrations(config, currentVersion, newVersion, messageMigrations);
    }

    static void applyMigrations(FileConfiguration config, String currentVersion, String newVersion,
                                NavigableMap<String, Consumer<FileConfiguration>> migrations) {
        String normalizedCurrent = normalizeVersion(currentVersion);
        String normalizedNew = normalizeVersion(newVersion);

        if (VERSION_COMPARATOR.compare(normalizedCurrent, normalizedNew) >= 0 || migrations.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Consumer<FileConfiguration>> entry : migrations.entrySet()) {
            String targetVersion = normalizeVersion(entry.getKey());
            if (VERSION_COMPARATOR.compare(targetVersion, normalizedCurrent) > 0
                && VERSION_COMPARATOR.compare(targetVersion, normalizedNew) <= 0) {
                entry.getValue().accept(config);
            }
        }
    }

    private static String normalizeVersion(String version) {
        String normalized = version;
        while (normalized.endsWith(".0")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        return normalized;
    }
}
