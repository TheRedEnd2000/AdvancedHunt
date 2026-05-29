package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.model.Reward;
import de.theredend2000.advancedhunt.model.RewardType;
import de.theredend2000.advancedhunt.model.Treasure;
import de.theredend2000.advancedhunt.model.TreasureCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

public class TreasureManagerCacheTest {

    private static World world(String name) {
        World world = mock(World.class);
        when(world.getName()).thenReturn(name);
        return world;
    }

    private static Treasure treasure(UUID id, UUID collectionId, String worldName, int x, int y, int z, List<Reward> rewards) {
        return new Treasure(id, collectionId, new Location(world(worldName), x, y, z), rewards, "nbt", "STONE", null);
    }

    @Test
    public void addTreasureAsyncPersistsAndHydratesAllCaches() {
        DataRepository repository = mock(DataRepository.class);
        TreasureManager manager = new TreasureManager(repository);
        UUID treasureId = UUID.randomUUID();
        UUID collectionId = UUID.randomUUID();
        Treasure treasure = treasure(treasureId, collectionId, "world", 8, 64, 8, Collections.<Reward>emptyList());

        when(repository.saveTreasure(treasure)).thenReturn(CompletableFuture.completedFuture(null));

        manager.addTreasureAsync(treasure).join();

        verify(repository).saveTreasure(treasure);
        assertEquals(1, manager.getTreasureCount());
        assertEquals(collectionId, manager.getCollectionIdForTreasure(treasureId));
        assertEquals(1, manager.getTreasureCoresInCollection(collectionId).size());
        assertEquals(1, manager.getTreasureCoresInChunk(0, 0).size());
        assertSame(treasure, manager.getFullTreasure(treasureId));
        verify(repository, times(0)).loadTreasure(treasureId);
    }

    @Test
    public void loadTreasuresReplacesIndexesAndClearsFullTreasureCache() {
        DataRepository repository = mock(DataRepository.class);
        TreasureManager manager = new TreasureManager(repository);
        UUID firstCollectionId = UUID.randomUUID();
        UUID secondCollectionId = UUID.randomUUID();
        Treasure firstTreasure = treasure(UUID.randomUUID(), firstCollectionId, "world", 0, 64, 0, Collections.<Reward>emptyList());
        TreasureCore replacementCore = TreasureCore.from(
            treasure(UUID.randomUUID(), secondCollectionId, "world_nether", 64, 70, 64, Collections.<Reward>emptyList())
        );

        when(repository.saveTreasure(firstTreasure)).thenReturn(CompletableFuture.completedFuture(null));
        when(repository.loadTreasureCores()).thenReturn(CompletableFuture.completedFuture(Collections.singletonList(replacementCore)));

        manager.addTreasureAsync(firstTreasure).join();
        assertEquals(1L, manager.getCachedFullTreasureCount());

        manager.loadTreasures();

        assertEquals(1, manager.getTreasureCount());
        assertNull(manager.getTreasureCoreById(firstTreasure.getId()));
        assertNotNull(manager.getTreasureCoreById(replacementCore.getId()));
        assertEquals(0L, manager.getCachedFullTreasureCount());
    }

    @Test
    public void updateTreasureWithSameIdMovesIndexesWithoutLeavingDuplicates() {
        DataRepository repository = mock(DataRepository.class);
        TreasureManager manager = new TreasureManager(repository);
        UUID treasureId = UUID.randomUUID();
        UUID oldCollectionId = UUID.randomUUID();
        UUID newCollectionId = UUID.randomUUID();
        Treasure oldTreasure = treasure(treasureId, oldCollectionId, "world", 1, 64, 1, Collections.<Reward>emptyList());
        Treasure newTreasure = treasure(treasureId, newCollectionId, "world", 32, 70, 32, Collections.<Reward>emptyList());

        when(repository.saveTreasure(oldTreasure)).thenReturn(CompletableFuture.completedFuture(null));
        when(repository.saveTreasure(newTreasure)).thenReturn(CompletableFuture.completedFuture(null));

        manager.addTreasureAsync(oldTreasure).join();
        manager.updateTreasure(oldTreasure, newTreasure).join();

        assertTrue(manager.getTreasureCoresInCollection(oldCollectionId).isEmpty());
        assertEquals(1, manager.getTreasureCoresInCollection(newCollectionId).size());
        assertTrue(manager.getTreasureCoresInChunk(0, 0).isEmpty());
        assertEquals(1, manager.getTreasureCoresInChunk(2, 2).size());
        assertEquals(newCollectionId, manager.getCollectionIdForTreasure(treasureId));
        TreasureCore updatedCore = manager.getTreasureCoreById(treasureId);
        assertNotNull(updatedCore);
        assertEquals(32, updatedCore.getLocation().getBlockX());
        assertEquals(32, updatedCore.getLocation().getBlockZ());
    }

    @Test
    public void getTreasureCoreAtIgnoresCachedTreasuresWithMissingWorld() {
        DataRepository repository = mock(DataRepository.class);
        TreasureManager manager = new TreasureManager(repository);
        Treasure missingWorldTreasure = new Treasure(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new Location(null, 16, 64, 16),
            Collections.<Reward>emptyList(),
            "nbt",
            "STONE",
            null
        );

        when(repository.saveTreasure(missingWorldTreasure)).thenReturn(CompletableFuture.completedFuture(null));

        manager.addTreasureAsync(missingWorldTreasure).join();

        assertNull(manager.getTreasureCoreAt(new Location(world("world"), 16, 64, 16)));
    }

    @Test
    public void updateTreasureRewardsRefreshesCachedFullTreasureData() {
        DataRepository repository = mock(DataRepository.class);
        TreasureManager manager = new TreasureManager(repository);
        Treasure treasure = treasure(UUID.randomUUID(), UUID.randomUUID(), "world", 10, 64, 10, Collections.<Reward>emptyList());
        List<Reward> rewards = Collections.singletonList(
            new Reward(RewardType.COMMAND, 100.0, null, null, "say hello")
        );

        when(repository.saveTreasure(treasure)).thenReturn(CompletableFuture.completedFuture(null));
        when(repository.updateTreasureRewards(treasure.getId(), rewards)).thenReturn(CompletableFuture.completedFuture(null));

        manager.addTreasureAsync(treasure).join();
        manager.updateTreasureRewards(treasure.getId(), rewards).join();

        assertEquals(rewards, manager.getFullTreasure(treasure.getId()).getRewards());
        verify(repository, times(0)).loadTreasure(treasure.getId());
    }

    @Test
    public void addTreasuresBatchAsyncUsesRepositoryBatchAndIndexesEveryTreasure() {
        DataRepository repository = mock(DataRepository.class);
        TreasureManager manager = new TreasureManager(repository);
        UUID collectionId = UUID.randomUUID();
        Treasure first = treasure(UUID.randomUUID(), collectionId, "world", 0, 64, 0, Collections.<Reward>emptyList());
        Treasure second = treasure(UUID.randomUUID(), collectionId, "world", 31, 64, 31, Collections.<Reward>emptyList());

        when(repository.saveTreasuresBatch(anyList())).thenReturn(CompletableFuture.completedFuture(null));

        manager.addTreasuresBatchAsync(Arrays.asList(first, second)).join();

        verify(repository).saveTreasuresBatch(Arrays.asList(first, second));
        assertEquals(2, manager.getTreasureCount());
        assertEquals(2, manager.getTreasureCoresInCollection(collectionId).size());
        assertFalse(manager.getTreasureIdsInCollection(collectionId).isEmpty());
    }

    @Test
    public void deleteTreasureRemovesIndexesAndFullCacheEntry() {
        DataRepository repository = mock(DataRepository.class);
        TreasureManager manager = new TreasureManager(repository);
        UUID collectionId = UUID.randomUUID();
        Treasure treasure = treasure(UUID.randomUUID(), collectionId, "world", 4, 64, 4, Collections.<Reward>emptyList());

        when(repository.saveTreasure(treasure)).thenReturn(CompletableFuture.completedFuture(null));
        when(repository.deleteTreasure(treasure.getId())).thenReturn(CompletableFuture.completedFuture(null));

        manager.addTreasureAsync(treasure).join();
        manager.deleteTreasure(treasure);

        verify(repository).deleteTreasure(treasure.getId());
        assertEquals(0, manager.getTreasureCount());
        assertTrue(manager.getTreasureCoresInCollection(collectionId).isEmpty());
        assertTrue(manager.getTreasureCoresInChunk(0, 0).isEmpty());
        assertEquals(0L, manager.getCachedFullTreasureCount());
        assertNull(manager.getCollectionIdForTreasure(treasure.getId()));
    }

    @Test
    public void removeCollectionPurgesOnlyTargetCollectionCaches() {
        DataRepository repository = mock(DataRepository.class);
        TreasureManager manager = new TreasureManager(repository);
        UUID targetCollectionId = UUID.randomUUID();
        UUID otherCollectionId = UUID.randomUUID();
        Treasure targetTreasure = treasure(UUID.randomUUID(), targetCollectionId, "world", 0, 64, 0, Collections.<Reward>emptyList());
        Treasure otherTreasure = treasure(UUID.randomUUID(), otherCollectionId, "world", 48, 64, 48, Collections.<Reward>emptyList());

        when(repository.saveTreasure(targetTreasure)).thenReturn(CompletableFuture.completedFuture(null));
        when(repository.saveTreasure(otherTreasure)).thenReturn(CompletableFuture.completedFuture(null));

        manager.addTreasureAsync(targetTreasure).join();
        manager.addTreasureAsync(otherTreasure).join();

        manager.removeCollection(targetCollectionId);

        assertTrue(manager.getTreasureCoresInCollection(targetCollectionId).isEmpty());
        assertNull(manager.getTreasureCoreById(targetTreasure.getId()));
        assertNull(manager.getCollectionIdForTreasure(targetTreasure.getId()));
        assertEquals(1, manager.getTreasureCount());
        assertEquals(1, manager.getTreasureCoresInCollection(otherCollectionId).size());
        assertEquals(otherCollectionId, manager.getCollectionIdForTreasure(otherTreasure.getId()));
        assertEquals(1L, manager.getCachedFullTreasureCount());
    }
}