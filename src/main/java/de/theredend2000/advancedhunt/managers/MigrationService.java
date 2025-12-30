package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class MigrationService {

    private final Logger logger;

    public MigrationService(Logger logger) {
        this.logger = logger;
    }

    public CompletableFuture<Void> migrate(DataRepository source, DataRepository target) {
        return migrate(source, target, null);
    }

    public record MigrationProgress(int percent, String stage, int current, int total) {
    }

    public CompletableFuture<Void> migrate(DataRepository source, DataRepository target, Consumer<MigrationProgress> progressCallback) {
        logger.info("Starting migration...");

        CompletableFuture<List<Collection>> collectionsFuture = source.loadCollections();
        CompletableFuture<List<Treasure>> treasuresFuture = source.loadTreasures();
        CompletableFuture<List<UUID>> playerUUIDsFuture = source.getAllPlayerUUIDs();
        CompletableFuture<List<RewardPreset>> treasurePresetsFuture = source.loadRewardPresets(RewardPresetType.TREASURE);
        CompletableFuture<List<RewardPreset>> collectionPresetsFuture = source.loadRewardPresets(RewardPresetType.COLLECTION);

        return CompletableFuture.allOf(
                collectionsFuture,
                treasuresFuture,
                playerUUIDsFuture,
                treasurePresetsFuture,
                collectionPresetsFuture
            )
                .thenCompose(v -> {
                    List<Collection> collections = collectionsFuture.join();
                    List<Treasure> treasures = treasuresFuture.join();
                    List<UUID> playerUUIDs = playerUUIDsFuture.join();
                    List<RewardPreset> treasurePresets = treasurePresetsFuture.join();
                    List<RewardPreset> collectionPresets = collectionPresetsFuture.join();

                    final int totalWork = collections.size() + treasures.size() + playerUUIDs.size() + treasurePresets.size() + collectionPresets.size();
                    final ProgressTracker tracker = new ProgressTracker(totalWork, progressCallback);
                    tracker.report(0, "starting", 0);

                    logger.info("Migrating " + collections.size() + " collections, " +
                            treasures.size() + " treasures, and " +
                    playerUUIDs.size() + " player data entries, plus " +
                    treasurePresets.size() + " treasure reward presets and " +
                    collectionPresets.size() + " collection reward presets.");

                    List<CompletableFuture<Void>> saveFutures = new ArrayList<>();

                        for (Collection c : collections) {
                        saveFutures.add(target.saveCollection(c)
                            .thenRun(() -> tracker.increment("collections", 1)));
                        }
                        for (Treasure t : treasures) {
                        saveFutures.add(target.saveTreasure(t)
                            .thenRun(() -> tracker.increment("treasures", 1)));
                        }
                        for (RewardPreset p : treasurePresets) {
                        saveFutures.add(target.saveRewardPreset(p)
                            .thenRun(() -> tracker.increment("reward_presets", 1)));
                        }
                        for (RewardPreset p : collectionPresets) {
                        saveFutures.add(target.saveRewardPreset(p)
                            .thenRun(() -> tracker.increment("reward_presets", 1)));
                        }

                    return CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0]))
                            .thenCompose(v2 -> migratePlayersInChunks(source, target, playerUUIDs, tracker))
                            .thenRun(() -> tracker.report(100, "done", tracker.current()));
                    }).thenRun(() -> logger.info("Migration completed successfully!"));
    }

                private CompletableFuture<Void> migratePlayersInChunks(DataRepository source,
                                          DataRepository target,
                                          List<UUID> playerUUIDs,
                                          ProgressTracker tracker) {
        int chunkSize = 100;
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        int totalChunks = (playerUUIDs.size() + chunkSize - 1) / chunkSize;

        for (int i = 0; i < playerUUIDs.size(); i += chunkSize) {
            int start = i;
            int end = Math.min(i + chunkSize, playerUUIDs.size());
            int currentChunk = (i / chunkSize) + 1;
            List<UUID> chunk = playerUUIDs.subList(start, end);

            future = future.thenCompose(v -> {
                List<CompletableFuture<PlayerData>> loadFutures = new ArrayList<>();
                for (UUID uuid : chunk) {
                    loadFutures.add(source.loadPlayerData(uuid));
                }
                
                return CompletableFuture.allOf(loadFutures.toArray(new CompletableFuture[0]))
                    .thenCompose(v2 -> {
                        List<PlayerData> loadedData = new ArrayList<>();
                        for (CompletableFuture<PlayerData> f : loadFutures) {
                            loadedData.add(f.join());
                        }
                        
                        logger.info("Migrating player chunk " + currentChunk + "/" + totalChunks);
                        return target.savePlayerDataBatch(loadedData)
                                .thenRun(() -> tracker.increment("players", loadedData.size()));
                    });
            });
        }
        return future;
    }

    private static final class ProgressTracker {
        private final int total;
        private final Consumer<MigrationProgress> callback;
        private final AtomicInteger current = new AtomicInteger(0);
        private final AtomicInteger nextPercentToReport = new AtomicInteger(10);

        private ProgressTracker(int total, Consumer<MigrationProgress> callback) {
            this.total = Math.max(0, total);
            this.callback = callback;
        }

        private int current() {
            return current.get();
        }

        private void increment(String stage, int delta) {
            if (delta <= 0) return;
            int updated = current.addAndGet(delta);
            reportIfNeeded(stage, updated);
        }

        private void report(int percent, String stage, int currentValue) {
            if (callback == null) return;
            callback.accept(new MigrationProgress(clamp(percent, 0, 100), stage, currentValue, total));
        }

        private void reportIfNeeded(String stage, int currentValue) {
            if (callback == null) return;
            if (total <= 0) return;

            int percent = (int) Math.floor((currentValue * 100.0) / total);
            int next = nextPercentToReport.get();
            if (percent < next) return;

            while (percent >= next && next < 100) {
                if (nextPercentToReport.compareAndSet(next, next + 10)) {
                    report(next, stage, currentValue);
                    next += 10;
                } else {
                    next = nextPercentToReport.get();
                }
            }
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
