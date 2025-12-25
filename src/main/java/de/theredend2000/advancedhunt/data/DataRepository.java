package de.theredend2000.advancedhunt.data;

import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.model.PlayerData;
import de.theredend2000.advancedhunt.model.Treasure;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DataRepository {

    void init();
    void shutdown();

    // Player Data
    CompletableFuture<PlayerData> loadPlayerData(UUID playerUuid);
    CompletableFuture<Void> savePlayerData(PlayerData playerData);
    /**
     * Loads ALL player data into memory.
     * WARNING: This is extremely memory intensive and should ONLY be used for migration.
     * Do not use for gameplay logic.
     */
    CompletableFuture<List<PlayerData>> loadAllPlayerData();

    // Treasures
    CompletableFuture<List<Treasure>> loadTreasures();
    CompletableFuture<Void> saveTreasure(Treasure treasure);
    CompletableFuture<Void> deleteTreasure(UUID treasureId);
    
    /**
     * Loads a single treasure by ID.
     * Useful for on-demand loading of heavy treasure data.
     * @param treasureId the treasure UUID
     * @return the full treasure object, or null if not found
     */
    CompletableFuture<Treasure> loadTreasure(UUID treasureId);

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
     * Gets all treasure IDs that a player has found within a specific collection.
     * Optimized for progress menu displays and collection-specific queries.
     * @param playerUuid the player's UUID
     * @param collectionId the collection to filter by
     * @return set of treasure UUIDs the player has found in the collection
     */
    CompletableFuture<Set<UUID>> getPlayerFoundInCollection(UUID playerUuid, UUID collectionId);
    
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
