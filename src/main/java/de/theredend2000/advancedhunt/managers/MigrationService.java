package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.model.Treasure;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class MigrationService {

    private final Logger logger;

    public MigrationService(Logger logger) {
        this.logger = logger;
    }

    public CompletableFuture<Void> migrate(DataRepository source, DataRepository target) {
        logger.info("Starting migration...");

        CompletableFuture<List<Collection>> collectionsFuture = source.loadCollections();
        CompletableFuture<List<Treasure>> treasuresFuture = source.loadTreasures();
        CompletableFuture<List<UUID>> playerUUIDsFuture = source.getAllPlayerUUIDs();

        return CompletableFuture.allOf(collectionsFuture, treasuresFuture, playerUUIDsFuture)
                .thenCompose(v -> {
                    List<Collection> collections = collectionsFuture.join();
                    List<Treasure> treasures = treasuresFuture.join();
                    List<UUID> playerUUIDs = playerUUIDsFuture.join();

                    logger.info("Migrating " + collections.size() + " collections, " +
                            treasures.size() + " treasures, and " +
                            playerUUIDs.size() + " player data entries.");

                    List<CompletableFuture<Void>> saveFutures = new ArrayList<>();

                    for (Collection c : collections) saveFutures.add(target.saveCollection(c));
                    for (Treasure t : treasures) saveFutures.add(target.saveTreasure(t));

                    return CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0]))
                            .thenCompose(v2 -> migratePlayersInChunks(source, target, playerUUIDs));
                }).thenRun(() -> logger.info("Migration completed successfully!"));
    }

    private CompletableFuture<Void> migratePlayersInChunks(DataRepository source, DataRepository target, List<UUID> playerUUIDs) {
        int chunkSize = 100;
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        int totalChunks = (playerUUIDs.size() + chunkSize - 1) / chunkSize;

        for (int i = 0; i < playerUUIDs.size(); i += chunkSize) {
            int start = i;
            int end = Math.min(i + chunkSize, playerUUIDs.size());
            int currentChunk = (i / chunkSize) + 1;
            List<UUID> chunk = playerUUIDs.subList(start, end);

            future = future.thenCompose(v -> {
                List<CompletableFuture<Void>> chunkFutures = new ArrayList<>();
                for (UUID uuid : chunk) {
                    chunkFutures.add(source.loadPlayerData(uuid)
                            .thenCompose(target::savePlayerData));
                }
                logger.info("Migrating player chunk " + currentChunk + "/" + totalChunks);
                return CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]));
            });
        }
        return future;
    }
}
