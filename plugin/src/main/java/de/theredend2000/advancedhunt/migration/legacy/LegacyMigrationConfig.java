package de.theredend2000.advancedhunt.migration.legacy;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public final class LegacyMigrationConfig {

    private final boolean enabled;
    private final boolean allowMerge;
    private final boolean dryRun;
    private final boolean createBackups;
    private final boolean failFast;
    private final int snapshotBatchPerTick;
    private final File pluginDataFolder;

    private LegacyMigrationConfig(boolean enabled,
                                 boolean allowMerge,
                                 boolean dryRun,
                                 boolean createBackups,
                                 boolean failFast,
                                 int snapshotBatchPerTick,
                                 File pluginDataFolder) {
        this.enabled = enabled;
        this.allowMerge = allowMerge;
        this.dryRun = dryRun;
        this.createBackups = createBackups;
        this.failFast = failFast;
        this.snapshotBatchPerTick = snapshotBatchPerTick;
        this.pluginDataFolder = pluginDataFolder;
    }

    public static LegacyMigrationConfig fromConfig(FileConfiguration config, File pluginDataFolder) {
        boolean enabled = config.getBoolean("migration.legacy.enabled", false);
        boolean allowMerge = config.getBoolean("migration.legacy.allow-merge", false);
        boolean dryRun = config.getBoolean("migration.legacy.dry-run", false);
        boolean createBackups = config.getBoolean("migration.legacy.create-backup", true);
        boolean failFast = config.getBoolean("migration.legacy.fail-fast", true);
        int perTick = Math.max(1, config.getInt("migration.legacy.snapshot-batch-per-tick", 250));

        return new LegacyMigrationConfig(enabled, allowMerge, dryRun, createBackups, failFast, perTick, pluginDataFolder);
    }

    public boolean enabled() {
        return enabled;
    }

    public File sourceFolder() {
        return pluginDataFolder;
    }

    public boolean allowMerge() {
        return allowMerge;
    }

    public boolean dryRun() {
        return dryRun;
    }

    public boolean createBackups() {
        return createBackups;
    }

    public boolean failFast() {
        return failFast;
    }

    public int snapshotBatchPerTick() {
        return snapshotBatchPerTick;
    }

    public LegacyMigrationConfig withEnabled(boolean enabled) {
        return new LegacyMigrationConfig(
            enabled,
            this.allowMerge,
            this.dryRun,
            this.createBackups,
            this.failFast,
            this.snapshotBatchPerTick,
            this.pluginDataFolder
        );
    }
}
