package de.theredend2000.advancedhunt.migration.legacy;

/**
 * Shared utilities for legacy data migration.
 */
public final class LegacyMigrationUtil {

    private LegacyMigrationUtil() {
    }

    /**
     * Migrates legacy placeholder names to their modern equivalents.
     * Normalizes all placeholders to lowercase format.
     * 
     * @param command The command string containing placeholders
     * @return The command with migrated and normalized placeholders
     */
    public static String migrateLegacyPlaceholders(String command) {
        if (command == null) {
            return null;
        }
        return command
                .replace("%EGGS_FOUND%", "%found_treasures%")
                .replace("%EGGS_MAX%", "%max_treasures%")
                .replace("%TREASURES_FOUND%", "%found_treasures%")
                .replace("%TREASURES_MAX%", "%max_treasures%")
                .replace("%PLAYER%", "%player%")
                .replace("%PREFIX%", "%prefix%");
    }
}
