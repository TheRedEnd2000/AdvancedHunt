package de.theredend2000.advancedhunt.util;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Handles version-specific migration logic for configuration files.
 * This class is responsible for modifying the in-memory configuration
 * before the ConfigUpdater applies the new structure from the JAR.
 */
public class ConfigMigrationHandler {

    private static final Map<Integer, Consumer<FileConfiguration>> configMigrations = new ConcurrentHashMap<>();
    private static final Map<Integer, Consumer<FileConfiguration>> messageMigrations = new ConcurrentHashMap<>();

    static {
        // Register config migrations here
        // Key is the target version (the version we are upgrading TO)
        
        // Example: Upgrade to version 2
        // configMigrations.put(2, config -> {
        //     if (config.contains("old.key")) {
        //         config.set("new.key", config.get("old.key"));
        //         config.set("old.key", null);
        //     }
        // });
    }

    static {
        // Register message migrations here
        
        // Example: Upgrade to version 2
        // messageMigrations.put(2, config -> {
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
    public static void migrateConfig(FileConfiguration config, int currentVersion, int newVersion) {
        applyMigrations(config, currentVersion, newVersion, configMigrations);
    }

    /**
     * Migrates message files (e.g., messages_en.yml).
     *
     * @param config         The current user configuration.
     * @param currentVersion The version of the user's configuration.
     * @param newVersion     The target version from the JAR.
     */
    public static void migrateMessages(FileConfiguration config, int currentVersion, int newVersion) {
        applyMigrations(config, currentVersion, newVersion, messageMigrations);
    }

    private static void applyMigrations(FileConfiguration config, int currentVersion, int newVersion, Map<Integer, Consumer<FileConfiguration>> migrations) {
        for (int version = currentVersion + 1; version <= newVersion; version++) {
            if (migrations.containsKey(version)) {
                migrations.get(version).accept(config);
            }
        }
    }
}
