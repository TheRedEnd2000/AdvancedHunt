package de.theredend2000.advancedhunt.data;

import de.theredend2000.advancedhunt.model.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DataRepository {

    void init();
    void reload();
    void shutdown();

    // Schema & Maintenance
    int getSchemaVersion();
    void upgradeSchema();

    // Player Data
    CompletableFuture<PlayerData> loadPlayerData(UUID playerUuid);
    CompletableFuture<Void> savePlayerData(PlayerData playerData);
    
    /**
     * Saves a list of player data objects in a batch.
     * Optimized for bulk operations like migration.
     * @param playerDataList the list of player data to save
     */
    CompletableFuture<Void> savePlayerDataBatch(List<PlayerData> playerDataList);

    /**
     * Loads ALL player data into memory.
     * WARNING: This is extremely memory intensive and should ONLY be used for migration.
     * Do not use for gameplay logic.
     */
    CompletableFuture<List<PlayerData>> loadAllPlayerData();

    /**
     * Gets all player UUIDs stored in the repository.
     * Useful for iterating over all players without loading their full data.
     */
    CompletableFuture<List<UUID>> getAllPlayerUUIDs();

    // Treasures
    CompletableFuture<List<Treasure>> loadTreasures();

    /**
     * Loads lightweight treasure cores (no rewards, no NBT/block state).
     *
     * This is the preferred startup/indexing API for memory and performance.
     * Use {@link #loadTreasure(UUID)} when full treasure data is required.
     */
    CompletableFuture<List<TreasureCore>> loadTreasureCores();

    CompletableFuture<Void> saveTreasure(Treasure treasure);

    /**
     * Saves a list of treasures in a batch.
     *
     * Implementations may use JDBC batch/transactions or optimized IO.
     * Default implementation falls back to saving each treasure individually.
     */
    default CompletableFuture<Void> saveTreasuresBatch(List<Treasure> treasures) {
        if (treasures == null || treasures.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<?>[] futures = treasures.stream()
                .map(this::saveTreasure)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }
    CompletableFuture<Void> deleteTreasure(UUID treasureId);

    /**
     * Deletes all treasures belonging to a collection.
     *
     * Implementations should prefer a single SQL statement / directory delete.
     * @return number of deleted treasures (best-effort for some backends)
     */
    CompletableFuture<Integer> deleteTreasuresInCollection(UUID collectionId);
    
    /**
     * Loads a single treasure by ID.
     * Useful for on-demand loading of heavy treasure data.
     * @param treasureId the treasure UUID
     * @return the full treasure object, or null if not found
     */
    CompletableFuture<Treasure> loadTreasure(UUID treasureId);

    /**
     * Updates only the rewards of a treasure.
     *
     * This avoids loading/saving full treasure data (e.g. NBT) for bulk operations.
     */
    CompletableFuture<Void> updateTreasureRewards(UUID treasureId, List<Reward> rewards);

    // Collections
    CompletableFuture<List<Collection>> loadCollections();
    CompletableFuture<Void> saveCollection(Collection collection);
    CompletableFuture<Void> deleteCollection(UUID id);
    CompletableFuture<Boolean> renameCollection(UUID id, String newName);

    // Advanced Operations (Optimized)
    CompletableFuture<Integer> resetAllProgress();
    CompletableFuture<Integer> resetCollectionProgress(UUID collectionId);
    CompletableFuture<Integer> resetPlayerProgress(UUID playerUuid);
    CompletableFuture<Integer> resetPlayerCollectionProgress(UUID playerUuid, UUID collectionId);
    CompletableFuture<Map<UUID, Integer>> getLeaderboard(UUID collectionId, int limit);
    CompletableFuture<List<UUID>> getPlayersWhoFound(UUID treasureId);

    /**
     * Gets the number of players who have found a treasure.
     *
     * This avoids allocating and transferring the full UUID list when callers only need the count.
     * Implementations should prefer an efficient COUNT(*) query / O(1) index lookup.
     */
    default CompletableFuture<Integer> getFoundPlayerCount(UUID treasureId) {
        return getPlayersWhoFound(treasureId).thenApply(list -> list == null ? 0 : list.size());
    }
    
    /**
     * Gets all treasure IDs that a player has found within a specific collection.
     * Optimized for progress menu displays and collection-specific queries.
     * @param playerUuid the player's UUID
     * @param collectionId the collection to filter by
     * @return set of treasure UUIDs the player has found in the collection
     */
    CompletableFuture<Set<UUID>> getPlayerFoundInCollection(UUID playerUuid, UUID collectionId);

    // Reward Presets
    CompletableFuture<List<RewardPreset>> loadRewardPresets(RewardPresetType type);
    CompletableFuture<Void> saveRewardPreset(RewardPreset preset);
    CompletableFuture<Void> deleteRewardPreset(RewardPresetType type, UUID presetId);
    
    /**
     * Flushes any pending index data to persistent storage.
     * For YAML backend, this saves the finder index to disk.
     * For SQL backend, this is a no-op since data is already persisted.
     */
    default void flushIndexes() {
        // Default no-op for backends that don't need explicit flushing
    }
    
    /**
     * Called by LeaderboardManager to trigger leaderboard recalculation.
     * For YAML backend, this rebuilds the leaderboard.yml file.
     * For SQL backend, this is a no-op since leaderboard is calculated on-demand.
     */
    default void rebuildLeaderboardCache() {
        // Default no-op for backends that calculate leaderboards dynamically
    }
}
