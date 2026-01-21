package de.theredend2000.advancedhunt.migration.legacy;

import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.model.*;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.util.ZipBackupUtil;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class LegacyDataMigrator {

    private final Plugin plugin;
    private final DataRepository repository;
    private final LegacyMigrationConfig config;

    public LegacyDataMigrator(Plugin plugin, DataRepository repository, LegacyMigrationConfig config) {
        this.plugin = plugin;
        this.repository = repository;
        this.config = config;
    }

    /**
     * Force cleanup of legacy files, even if migration marker exists.
     * Useful for completing cleanup after a migration that used older cleanup logic.
     */
    public CompletableFuture<Void> forceCleanup() {
        return CompletableFuture.runAsync(() -> {
            File legacyRoot = config.sourceFolder();
            if (legacyRoot == null || !legacyRoot.exists() || !legacyRoot.isDirectory()) {
                plugin.getLogger().warning("Cannot cleanup: Legacy source folder not found: " + legacyRoot);
                return;
            }
            
            plugin.getLogger().info("Running forced legacy cleanup on: " + legacyRoot.getAbsolutePath());
            cleanupLegacyFiles(legacyRoot);
        });
    }

    public CompletableFuture<LegacyMigratorResult> run() {
        if (!config.enabled()) {
            return CompletableFuture.completedFuture(new LegacyMigratorResult(0, 0, 0, 0, 0, 0, 0, null));
        }

        File marker = new File(plugin.getDataFolder(), config.markerFileName());
        if (marker.exists()) {
            plugin.getLogger().info("Legacy migration marker exists (" + marker.getName() + ") - skipping.");
            
            // Still run cleanup in case previous migration used old cleanup logic
            plugin.getLogger().info("Re-running legacy cleanup to ensure all files are removed...");
            File legacyRoot = config.sourceFolder();
            if (legacyRoot != null && legacyRoot.exists() && legacyRoot.isDirectory()) {
                cleanupLegacyFiles(legacyRoot);
            }
            
            return CompletableFuture.completedFuture(new LegacyMigratorResult(0, 0, 0, 0, 0, 0, 0, null));
        }

        File legacyRoot = config.sourceFolder();
        if (legacyRoot == null || !legacyRoot.exists() || !legacyRoot.isDirectory()) {
            return failed("Legacy migration source folder not found: " + legacyRoot);
        }

        return doMigration(marker, legacyRoot);
    }

    private CompletableFuture<LegacyMigratorResult> doMigration(File marker, File legacyRoot) {
        File backupsDir = new File(plugin.getDataFolder(), "backups");
        File targetBackup = null;

        if (config.createBackups()) {
            try {
                targetBackup = ZipBackupUtil.createZipBackup(plugin.getDataFolder(), backupsDir, "legacy-migration-target");
                plugin.getLogger().info("Created migration backup: " + targetBackup.getAbsolutePath());
            } catch (IOException e) {
                return failedFuture(e);
            }
        }

        final File finalTargetBackup = targetBackup;

        // Parse legacy data
        List<LegacyEggsParser.LegacyCollection> legacyCollections = LegacyEggsParser.parseAll(legacyRoot);
        List<LegacyPlayerDataParser.LegacyPlayerData> legacyPlayers = LegacyPlayerDataParser.parseAll(legacyRoot);
        List<RewardPreset> rewardPresets = LegacyRewardPresetParser.parseAll(legacyRoot);
        List<PlaceItem> placeItems = LegacyPlacePresetParser.parseAll(legacyRoot);

        plugin.getLogger().info("Legacy migration loaded " + legacyCollections.size() + " collection(s), "
            + legacyPlayers.size() + " player(s), "
            + rewardPresets.size() + " reward preset(s), "
            + placeItems.size() + " place preset(s).");

        // Build new collections and gather placed eggs
        Map<String, UUID> legacyNameToCollectionId = new HashMap<>();
        List<Collection> newCollections = new ArrayList<>();
        List<PlacedEggToCreate> eggsToCreate = new ArrayList<>();

        for (LegacyEggsParser.LegacyCollection lc : legacyCollections) {
            UUID newCollectionId = UUID.randomUUID();
            legacyNameToCollectionId.put(lc.name, newCollectionId);

            Collection collection = new Collection(newCollectionId, lc.name, lc.enabled);
            collection.setSinglePlayerFind(lc.onePlayer);
            collection.setCompletionRewards(lc.globalRewards);
            collection.setActRules(LegacyActRuleTranslator.translate(newCollectionId, lc.requirements));
            collection.setProgressResetCron(translateProgressResetCron(lc.reset));

            newCollections.add(collection);

            for (LegacyEggsParser.LegacyPlacedEgg egg : lc.placedEggs) {
                eggsToCreate.add(new PlacedEggToCreate(lc.name, newCollectionId, egg));
            }
        }

        // Snapshot blocks on main thread
        List<LegacyLocationKey> snapshotKeys = new ArrayList<>();
        for (PlacedEggToCreate e : eggsToCreate) {
            snapshotKeys.add(new LegacyLocationKey(e.legacyCollectionName, e.egg.world, e.egg.x, e.egg.y, e.egg.z));
        }

        return LegacyBlockSnapshotter.snapshotBlocks(plugin, snapshotKeys, config.snapshotBatchPerTick())
            .thenCompose(snapshots -> {
                // Create treasures with snapshot data
                List<Treasure> treasures = new ArrayList<>();
                Map<LegacyLocationKey, UUID> legacyToTreasureId = new HashMap<>();

                Set<UUID> usedTreasureIds = new HashSet<>();
                for (PlacedEggToCreate e : eggsToCreate) {
                    LegacyLocationKey key = new LegacyLocationKey(e.legacyCollectionName, e.egg.world, e.egg.x, e.egg.y, e.egg.z);
                    LegacyBlockSnapshotter.Snapshot snap = snapshots.get(key);
                    if (snap == null) {
                        plugin.getLogger().warning("Skipping legacy egg (no world/block snapshot): " + key);
                        continue;
                    }

                    UUID treasureId = generateUniqueId(usedTreasureIds);
                    usedTreasureIds.add(treasureId);

                    org.bukkit.World world = org.bukkit.Bukkit.getWorld(e.egg.world);
                    if (world == null) {
                        plugin.getLogger().warning("Skipping legacy egg (world not loaded): " + key);
                        continue;
                    }

                    org.bukkit.Location loc = new org.bukkit.Location(world, e.egg.x, e.egg.y, e.egg.z);

                    Treasure treasure = new Treasure(
                        treasureId,
                        e.collectionId,
                        loc,
                        e.egg.rewards,
                        snap.nbtData,
                        snap.material,
                        snap.blockState
                    );

                    treasures.add(treasure);
                    legacyToTreasureId.put(key, treasureId);
                }

                // Convert players
                List<PlayerData> playerDataList = new ArrayList<>();
                int foundLinks = 0;
                int missingLinks = 0;

                for (LegacyPlayerDataParser.LegacyPlayerData lp : legacyPlayers) {
                    PlayerData pd = new PlayerData(lp.playerUuid);

                    for (LegacyPlayerDataParser.LegacyFoundEgg fe : lp.found) {
                        if (!legacyNameToCollectionId.containsKey(fe.collectionName)) {
                            continue;
                        }

                        LegacyLocationKey key = new LegacyLocationKey(fe.collectionName, fe.world, fe.x, fe.y, fe.z);
                        UUID treasureId = legacyToTreasureId.get(key);
                        if (treasureId == null) {
                            missingLinks++;
                            continue;
                        }
                        pd.addFoundTreasure(treasureId);
                        foundLinks++;
                    }

                    playerDataList.add(pd);
                }

                // Persist all data
                CompletableFuture<?>[] saveCollections = newCollections.stream()
                    .map(repository::saveCollection)
                    .toArray(CompletableFuture[]::new);

                CompletableFuture<?>[] saveRewardPresets = rewardPresets.stream()
                    .map(repository::saveRewardPreset)
                    .toArray(CompletableFuture[]::new);

                CompletableFuture<?>[] savePlaceItems = placeItems.stream()
                    .map(repository::savePlaceItem)
                    .toArray(CompletableFuture[]::new);

                int finalFoundLinks = foundLinks;
                int finalMissingLinks = missingLinks;
                int rewardPresetCount = rewardPresets.size();
                int placePresetCount = placeItems.size();

                return CompletableFuture.allOf(saveCollections)
                    .thenCompose(v2 -> repository.saveTreasuresBatch(treasures))
                    .thenCompose(v2 -> repository.savePlayerDataBatch(playerDataList))
                    .thenCompose(v2 -> CompletableFuture.allOf(saveRewardPresets))
                    .thenCompose(v2 -> CompletableFuture.allOf(savePlaceItems))
                    .thenApply(v2 -> {
                        writeMarker(marker, legacyRoot, newCollections.size(), treasures.size(), 
                            playerDataList.size(), rewardPresetCount, placePresetCount, finalTargetBackup);

                        cleanupLegacyFiles(legacyRoot);

                        return new LegacyMigratorResult(
                            newCollections.size(),
                            treasures.size(),
                            playerDataList.size(),
                            rewardPresetCount,
                            placePresetCount,
                            finalFoundLinks,
                            finalMissingLinks,
                            finalTargetBackup
                        );
                    });
            }).handle((result, ex) -> {
                if (ex != null) {
                    plugin.getLogger().log(Level.SEVERE, "Legacy migration failed", ex);
                    return LegacyDataMigrator.<LegacyMigratorResult>failedFuture(ex);
                }
                return CompletableFuture.completedFuture(result);
            }).thenCompose(f -> f);
    }

    private <T> CompletableFuture<T> failed(String message) {
        IllegalStateException ex = new IllegalStateException(message);
        plugin.getLogger().severe(message);
        return failedFuture(ex);
    }

    private static <T> CompletableFuture<T> failedFuture(Throwable ex) {
        CompletableFuture<T> f = new CompletableFuture<>();
        f.completeExceptionally(ex);
        return f;
    }

    private static UUID generateUniqueId(Set<UUID> used) {
        UUID id;
        do {
            id = UUID.randomUUID();
        } while (used.contains(id));
        return id;
    }

    private static String translateProgressResetCron(LegacyEggsParser.LegacyReset reset) {
        if (reset == null) {
            return null;
        }

        int year = reset.year;
        int month = reset.month;
        int date = reset.date;
        int hour = reset.hour;
        int minute = reset.minute;
        int second = reset.second;

        boolean allZero = year == 0 && month == 0 && date == 0 && hour == 0 && minute == 0 && second == 0;
        if (allZero) {
            return null;
        }

        // Best-effort: support simple interval-only resets
        if (year == 0 && month == 0 && date == 0) {
            if (hour > 0 && minute == 0 && second == 0) {
                return "0 0 0/" + hour + " * * ?";
            }
            if (minute > 0 && second == 0 && hour == 0) {
                return "0 0/" + minute + " * * * ?";
            }
            if (second > 0 && minute == 0 && hour == 0) {
                return "0/" + second + " * * * * ?";
            }
        }

        return null;
    }

    /**
     * Writes a marker file recording the successful migration.
     */
    private void writeMarker(File marker, File legacyRoot, int collections, int treasures, 
                            int players, int rewardPresets, int placePresets, File targetBackup) {
        try {
            String summary = "Legacy migration completed successfully.\n"
                + "Date: " + java.time.Instant.now() + "\n"
                + "Source: " + legacyRoot.getAbsolutePath() + "\n"
                + "Collections: " + collections + "\n"
                + "Treasures: " + treasures + "\n"
                + "Players: " + players + "\n"
                + "Reward Presets: " + rewardPresets + "\n"
                + "Place Presets: " + placePresets + "\n"
                + (targetBackup != null ? "Backup: " + targetBackup.getAbsolutePath() + "\n" : "");
            
            Files.write(marker.toPath(), summary.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            plugin.getLogger().info("Migration marker written: " + marker.getName());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to write migration marker", e);
        }
    }

    /**
     * Clean up all legacy files from the source folder.
     * Always runs after successful migration or when marker file exists.
     */
    public void cleanupLegacyFiles(File legacyRoot) {
        try {
            int deletedDirs = 0;
            int deletedFiles = 0;
            
            // Remove legacy folders
            deletedDirs += deleteLegacySubfolder(legacyRoot, "eggs") ? 1 : 0;
            deletedDirs += deleteLegacySubfolder(legacyRoot, "playerdata") ? 1 : 0;
            deletedDirs += deleteLegacySubfolder(legacyRoot, "invs") ? 1 : 0;
            deletedDirs += deleteLegacySubfolder(legacyRoot, "menus") ? 1 : 0;
            deletedDirs += deleteLegacySubfolder(legacyRoot, "messages") ? 1 : 0;
            deletedDirs += deleteLegacySubfolder(legacyRoot, "presets") ? 1 : 0;
            
            // Remove legacy config files
            deletedFiles += deleteLegacyFile(legacyRoot, "config.yml") ? 1 : 0;
            deletedFiles += deleteLegacyFile(legacyRoot, "mysql.yml") ? 1 : 0;
            deletedFiles += deleteLegacyFile(legacyRoot, "plugin_data.yml") ? 1 : 0;

            if (deletedDirs > 0 || deletedFiles > 0) {
                plugin.getLogger().info("Legacy cleanup complete. Removed " + deletedDirs + " folder(s) and " + deletedFiles + " file(s) from: " + legacyRoot.getAbsolutePath());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Legacy cleanup failed", e);
        }
    }

    private static boolean deleteLegacySubfolder(File root, String name) throws Exception {
        File folder = new File(root, name);
        if (!folder.exists() || !folder.isDirectory()) {
            return false;
        }
        deleteRecursively(folder.toPath());
        return true;
    }

    private static boolean deleteLegacyFile(File root, String name) throws Exception {
        File file = new File(root, name);
        if (!file.exists() || file.isDirectory()) {
            return false;
        }
        return Files.deleteIfExists(file.toPath());
    }

    private static void deleteRecursively(Path root) throws Exception {
        if (root == null || !Files.exists(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws java.io.IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, java.io.IOException exc) throws java.io.IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static final class PlacedEggToCreate {
        final String legacyCollectionName;
        final UUID collectionId;
        final LegacyEggsParser.LegacyPlacedEgg egg;

        private PlacedEggToCreate(String legacyCollectionName, UUID collectionId, LegacyEggsParser.LegacyPlacedEgg egg) {
            this.legacyCollectionName = legacyCollectionName;
            this.collectionId = collectionId;
            this.egg = egg;
        }
    }
}
