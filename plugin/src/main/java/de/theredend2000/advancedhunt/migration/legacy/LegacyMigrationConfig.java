package de.theredend2000.advancedhunt.migration.legacy;

import java.io.File;

/**
 * Configuration for legacy data migration.
 * Uses sensible defaults - migration auto-detects legacy data and runs automatically.
 */
public final class LegacyMigrationConfig {

    private static final boolean DEFAULT_CREATE_BACKUPS = true;
    private static final boolean DEFAULT_FAIL_FAST = true;
    private static final int DEFAULT_SNAPSHOT_BATCH_PER_TICK = 250;

    private final boolean enabled;
    private final File pluginDataFolder;

    private LegacyMigrationConfig(boolean enabled, File pluginDataFolder) {
        this.enabled = enabled;
        this.pluginDataFolder = pluginDataFolder;
    }

    /**
     * Creates a disabled migration config. Auto-detection in Main will enable if needed.
     */
    public static LegacyMigrationConfig create(File pluginDataFolder) {
        return new LegacyMigrationConfig(false, pluginDataFolder);
    }

    public boolean enabled() {
        return enabled;
    }

    public File sourceFolder() {
        return pluginDataFolder;
    }

    public boolean createBackups() {
        return DEFAULT_CREATE_BACKUPS;
    }

    public boolean failFast() {
        return DEFAULT_FAIL_FAST;
    }

    public int snapshotBatchPerTick() {
        return DEFAULT_SNAPSHOT_BATCH_PER_TICK;
    }

    public String markerFileName() {
        return ".legacy-migration-complete";
    }

    public LegacyMigrationConfig withEnabled(boolean enabled) {
        return new LegacyMigrationConfig(enabled, this.pluginDataFolder);
    }
}
