package de.theredend2000.advancedHunt.managers;

import de.theredend2000.advancedHunt.data.DataRepository;
import de.theredend2000.advancedHunt.model.Collection;
import de.theredend2000.advancedHunt.model.PlayerData;
import de.theredend2000.advancedHunt.model.Treasure;

import java.util.ArrayList;
import java.util.List;
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
        CompletableFuture<List<PlayerData>> playerDataFuture = source.loadAllPlayerData();

        return CompletableFuture.allOf(collectionsFuture, treasuresFuture, playerDataFuture)
                .thenCompose(v -> {
                    List<Collection> collections = collectionsFuture.join();
                    List<Treasure> treasures = treasuresFuture.join();
                    List<PlayerData> allPlayerData = playerDataFuture.join();

                    logger.info("Migrating " + collections.size() + " collections, " +
                            treasures.size() + " treasures, and " +
                            allPlayerData.size() + " player data entries.");

                    List<CompletableFuture<Void>> saveFutures = new ArrayList<>();

                    for (Collection c : collections) saveFutures.add(target.saveCollection(c));
                    for (Treasure t : treasures) saveFutures.add(target.saveTreasure(t));
                    for (PlayerData pd : allPlayerData) saveFutures.add(target.savePlayerData(pd));

                    return CompletableFuture.allOf(saveFutures.toArray(new CompletableFuture[0]));
                }).thenRun(() -> logger.info("Migration completed successfully!"));
    }
}
