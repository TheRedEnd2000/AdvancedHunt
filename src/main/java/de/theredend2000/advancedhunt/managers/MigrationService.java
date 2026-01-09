package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    public static final class MigrationProgress {
        private final int percent;
        private final String stage;
        private final int current;
        private final int total;

        public MigrationProgress(int percent, String stage, int current, int total) {
            this.percent = percent;
            this.stage = stage;
            this.current = current;
            this.total = total;
        }

        public int percent() {
            return percent;
        }

        public String stage() {
            return stage;
        }

        public int current() {
            return current;
        }

        public int total() {
            return total;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MigrationProgress)) return false;
            MigrationProgress that = (MigrationProgress) o;
            return percent == that.percent && current == that.current && total == that.total && Objects.equals(stage, that.stage);
        }

        @Override
        public int hashCode() {
            return Objects.hash(percent, stage, current, total);
        }

        @Override
        public String toString() {
            return "MigrationProgress{" +
                    "percent=" + percent +
                    ", stage='" + stage + '\'' +
                    ", current=" + current +
                    ", total=" + total +
                    '}';
        }
    }

    public CompletableFuture<Void> migrate(DataRepository source, DataRepository target, Consumer<MigrationProgress> progressCallback) {
        logger.info("Starting migration...");

        // Report early so callers can show a 0% "loading" state while large sources are being scanned.
        if (progressCallback != null) {
            progressCallback.accept(new MigrationProgress(0, "loading", 0, 0));
        }

        CompletableFuture<List<Collection>> collectionsFuture = source.loadCollections();
        CompletableFuture<List<UUID>> treasureIdsFuture = source.getAllTreasureUUIDs();
        CompletableFuture<List<UUID>> playerUUIDsFuture = source.getAllPlayerUUIDs();
        CompletableFuture<List<RewardPreset>> treasurePresetsFuture = source.loadRewardPresets(RewardPresetType.TREASURE);
        CompletableFuture<List<RewardPreset>> collectionPresetsFuture = source.loadRewardPresets(RewardPresetType.COLLECTION);

        if (progressCallback != null) {
            collectionsFuture.thenRun(() -> progressCallback.accept(new MigrationProgress(0, "loaded_collections", 0, 0)));
            treasureIdsFuture.thenRun(() -> progressCallback.accept(new MigrationProgress(0, "loaded_treasure_ids", 0, 0)));
            playerUUIDsFuture.thenRun(() -> progressCallback.accept(new MigrationProgress(0, "loaded_player_index", 0, 0)));
            treasurePresetsFuture.thenRun(() -> progressCallback.accept(new MigrationProgress(0, "loaded_treasure_presets", 0, 0)));
            collectionPresetsFuture.thenRun(() -> progressCallback.accept(new MigrationProgress(0, "loaded_collection_presets", 0, 0)));
        }

        return CompletableFuture.allOf(
                collectionsFuture,
                treasureIdsFuture,
                playerUUIDsFuture,
                treasurePresetsFuture,
                collectionPresetsFuture
            )
                .thenCompose(v -> {
                    List<Collection> collections = collectionsFuture.join();
                    List<UUID> treasureIds = treasureIdsFuture.join();
                    List<UUID> playerUUIDs = playerUUIDsFuture.join();
                    List<RewardPreset> treasurePresets = treasurePresetsFuture.join();
                    List<RewardPreset> collectionPresets = collectionPresetsFuture.join();

                    final int totalWork = collections.size() + treasureIds.size() + playerUUIDs.size() + treasurePresets.size() + collectionPresets.size();
                    final ProgressTracker tracker = new ProgressTracker(totalWork, progressCallback);
                    tracker.report(0, "starting", 0);

                    logger.info("Migrating " + collections.size() + " collections, " +
                        treasureIds.size() + " treasures, and " +
                        playerUUIDs.size() + " player data entries, plus " +
                        treasurePresets.size() + " treasure reward presets and " +
                        collectionPresets.size() + " collection reward presets.");

                    return migrateRewardPresetsInBatches(target, treasurePresets, tracker, "reward_presets")
                        .thenCompose(v2 -> migrateRewardPresetsInBatches(target, collectionPresets, tracker, "reward_presets"))
                        .thenCompose(v2 -> migrateCollectionsInBatches(target, collections, tracker))
                        .thenCompose(v2 -> migrateTreasuresInChunks(source, target, treasureIds, tracker))
                        .thenCompose(v2 -> migratePlayersInChunks(source, target, playerUUIDs, tracker))
                        .thenRun(() -> tracker.report(100, "done", tracker.current()));
                    }).thenRun(() -> logger.info("Migration completed successfully!"));
    }

    private CompletableFuture<Void> migrateCollectionsInBatches(DataRepository target,
                                                               List<Collection> collections,
                                                               ProgressTracker tracker) {
        if (collections == null || collections.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // Collections are usually few; still batch to avoid a huge future list.
        final int batchSize = 25;
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

        for (int i = 0; i < collections.size(); i += batchSize) {
            int start = i;
            int end = Math.min(i + batchSize, collections.size());
            List<Collection> batch = collections.subList(start, end);

            chain = chain.thenCompose(v -> {
                CompletableFuture<?>[] futures = batch.stream()
                    .map(target::saveCollection)
                    .toArray(CompletableFuture[]::new);
                return CompletableFuture.allOf(futures)
                    .thenRun(() -> tracker.increment("collections", batch.size()));
            });
        }

        return chain;
    }

    private CompletableFuture<Void> migrateRewardPresetsInBatches(DataRepository target,
                                                                  List<RewardPreset> presets,
                                                                  ProgressTracker tracker,
                                                                  String stage) {
        if (presets == null || presets.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // Presets are small, but do a single batch call when supported.
        return target.saveRewardPresetsBatch(presets)
            .thenRun(() -> tracker.increment(stage, presets.size()));
    }

    private CompletableFuture<Void> migrateTreasuresInChunks(DataRepository source,
                                                             DataRepository target,
                                                             List<UUID> treasureIds,
                                                             ProgressTracker tracker) {
        if (treasureIds == null || treasureIds.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        final int chunkSize = 50;
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        int totalChunks = (treasureIds.size() + chunkSize - 1) / chunkSize;

        for (int i = 0; i < treasureIds.size(); i += chunkSize) {
            int start = i;
            int end = Math.min(i + chunkSize, treasureIds.size());
            int currentChunk = (i / chunkSize) + 1;
            List<UUID> chunk = treasureIds.subList(start, end);

            future = future.thenCompose(v -> {
                List<CompletableFuture<Treasure>> loadFutures = new ArrayList<>();
                for (UUID id : chunk) {
                    loadFutures.add(source.loadTreasure(id));
                }

                return CompletableFuture.allOf(loadFutures.toArray(new CompletableFuture[0]))
                    .thenCompose(v2 -> {
                        List<Treasure> loadedTreasures = new ArrayList<>(chunk.size());
                        int missing = 0;
                        for (CompletableFuture<Treasure> f : loadFutures) {
                            Treasure t = f.join();
                            if (t != null) {
                                loadedTreasures.add(t);
                            } else {
                                missing++;
                            }
                        }

                        logger.info("Migrating treasure chunk " + currentChunk + "/" + totalChunks +
                            (missing > 0 ? (" (missing: " + missing + ")") : ""));

                        if (loadedTreasures.isEmpty()) {
                            tracker.increment("treasures", chunk.size());
                            return CompletableFuture.completedFuture(null);
                        }

                        return target.saveTreasuresBatch(loadedTreasures)
                            // Count attempted IDs so progress always completes even if some entries were missing.
                            .thenRun(() -> tracker.increment("treasures", chunk.size()));
                    });
            });
        }

        return future;
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
                        List<PlayerData> loadedData = new ArrayList<>(chunk.size());
                        int missing = 0;
                        for (CompletableFuture<PlayerData> f : loadFutures) {
                            PlayerData data = f.join();
                            if (data != null) {
                                loadedData.add(data);
                            } else {
                                missing++;
                            }
                        }
                        
                        logger.info("Migrating player chunk " + currentChunk + "/" + totalChunks +
                            (missing > 0 ? (" (missing: " + missing + ")") : ""));

                        if (loadedData.isEmpty()) {
                            tracker.increment("players", chunk.size());
                            return CompletableFuture.completedFuture(null);
                        }

                        return target.savePlayerDataBatch(loadedData)
                            // Count attempted UUIDs so progress always completes even if some entries were missing.
                            .thenRun(() -> tracker.increment("players", chunk.size()));
                    });
            });
        }
        return future;
    }

    private static final class ProgressTracker {
        private final int total;
        private final Consumer<MigrationProgress> callback;
        private final AtomicInteger current = new AtomicInteger(0);
        private final AtomicInteger nextPercentToReport = new AtomicInteger(1);

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
                if (nextPercentToReport.compareAndSet(next, next + 1)) {
                    report(next, stage, currentValue);
                    next += 1;
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
