package de.theredend2000.advancedhunt.migration.legacy;

import java.io.File;

public final class LegacyMigratorResult {

    private final int collectionsImported;
    private final int treasuresImported;
    private final int playersImported;
    private final int playerFoundLinks;
    private final int missingTreasureLinks;
    private final File targetBackupZip;

    public LegacyMigratorResult(int collectionsImported,
                               int treasuresImported,
                               int playersImported,
                               int playerFoundLinks,
                               int missingTreasureLinks,
                               File targetBackupZip) {
        this.collectionsImported = collectionsImported;
        this.treasuresImported = treasuresImported;
        this.playersImported = playersImported;
        this.playerFoundLinks = playerFoundLinks;
        this.missingTreasureLinks = missingTreasureLinks;
        this.targetBackupZip = targetBackupZip;
    }

    public int collectionsImported() {
        return collectionsImported;
    }

    public int treasuresImported() {
        return treasuresImported;
    }

    public int playersImported() {
        return playersImported;
    }

    public int playerFoundLinks() {
        return playerFoundLinks;
    }

    public int missingTreasureLinks() {
        return missingTreasureLinks;
    }

    public File targetBackupZip() {
        return targetBackupZip;
    }
}
