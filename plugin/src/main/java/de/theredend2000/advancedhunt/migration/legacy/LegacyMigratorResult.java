package de.theredend2000.advancedhunt.migration.legacy;

import java.io.File;

public final class LegacyMigratorResult {

    private final int collectionsImported;
    private final int treasuresImported;
    private final int playersImported;
    private final int rewardPresetsImported;
    private final int placePresetsImported;
    private final int playerFoundLinks;
    private final int missingTreasureLinks;
    private final File targetBackupZip;

    public LegacyMigratorResult(int collectionsImported,
                               int treasuresImported,
                               int playersImported,
                               int rewardPresetsImported,
                               int placePresetsImported,
                               int playerFoundLinks,
                               int missingTreasureLinks,
                               File targetBackupZip) {
        this.collectionsImported = collectionsImported;
        this.treasuresImported = treasuresImported;
        this.playersImported = playersImported;
        this.rewardPresetsImported = rewardPresetsImported;
        this.placePresetsImported = placePresetsImported;
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

    public int rewardPresetsImported() {
        return rewardPresetsImported;
    }

    public int placePresetsImported() {
        return placePresetsImported;
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
