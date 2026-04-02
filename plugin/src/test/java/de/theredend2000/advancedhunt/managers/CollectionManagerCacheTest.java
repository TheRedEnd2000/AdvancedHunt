package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.model.Collection;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CollectionManagerCacheTest {

    private static final Unsafe UNSAFE = getUnsafe();

    private static Unsafe getUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to access Unsafe for test setup", e);
        }
    }

    private static void setObjectField(Object target, String fieldName, Object value) {
        try {
            Field field = CollectionManager.class.getDeclaredField(fieldName);
            long offset = UNSAFE.objectFieldOffset(field);
            UNSAFE.putObject(target, offset, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to set field " + fieldName, e);
        }
    }

    private static final class Harness implements AutoCloseable {
        final DataRepository repository = mock(DataRepository.class);
        final RecordingTreasureManager treasureManager = new RecordingTreasureManager();
        final ActRuleEvaluator actRuleEvaluator = new ActRuleEvaluator(null);

        Harness(List<Collection> initialCollections, List<Collection> reloadedCollections) {
            final AtomicInteger index = new AtomicInteger();
            when(repository.loadCollections()).thenAnswer(invocation -> {
                int current = index.getAndIncrement();
                if (current <= 0) {
                    return CompletableFuture.completedFuture(initialCollections);
                }
                return CompletableFuture.completedFuture(reloadedCollections);
            });
        }

        CollectionManager createManager() {
            try {
                CollectionManager manager = (CollectionManager) UNSAFE.allocateInstance(CollectionManager.class);
                setObjectField(manager, "repository", repository);
                setObjectField(manager, "treasureManager", treasureManager);
                setObjectField(manager, "playerManager", null);
                setObjectField(manager, "rewardManager", null);
                setObjectField(manager, "actRuleEvaluator", actRuleEvaluator);
                setObjectField(manager, "cachedCollections", new ArrayList<>(Collections.<Collection>emptyList()));
                return manager;
            } catch (InstantiationException e) {
                throw new RuntimeException("Unable to allocate CollectionManager for test", e);
            }
        }

        @Override
        public void close() {
        }
    }

    private static final class RecordingTreasureManager extends TreasureManager {
        UUID removedCollectionId;

        RecordingTreasureManager() {
            super(mock(DataRepository.class));
        }

        @Override
        public void removeCollection(UUID collectionId) {
            removedCollectionId = collectionId;
        }
    }

    @Test
    public void saveCollectionReloadsCachedCollectionsAfterRepositorySave() {
        UUID collectionId = UUID.randomUUID();
        Collection initial = new Collection(collectionId, "Spring_Event", true);
        Collection updated = new Collection(collectionId, "Spring_Festival", true);

        try (Harness harness = new Harness(Collections.singletonList(initial), Collections.singletonList(updated))) {
            when(harness.repository.saveCollection(updated)).thenReturn(CompletableFuture.completedFuture(null));

            CollectionManager manager = harness.createManager();
            manager.reloadCollections().join();
            manager.saveCollection(updated).join();

            assertTrue(manager.getCollectionById(collectionId).isPresent());
            assertEquals("Spring_Festival", manager.getCollectionById(collectionId).get().getName());
            verify(harness.repository).saveCollection(updated);
        }
    }

    @Test
    public void createCollectionNormalizesNameSavesAndReloadsCache() {
        UUID collectionId = UUID.randomUUID();
        Collection saved = new Collection(collectionId, "Spring_Event", true);

        try (Harness harness = new Harness(Collections.<Collection>emptyList(), Collections.singletonList(saved))) {
            when(harness.repository.saveCollection(any(Collection.class))).thenReturn(CompletableFuture.completedFuture(null));

            CollectionManager manager = harness.createManager();
            manager.reloadCollections().join();

            assertTrue(manager.createCollection("Spring Event").join());

            ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
            verify(harness.repository).saveCollection(captor.capture());
            assertEquals("Spring_Event", captor.getValue().getName());
            assertTrue(manager.getCollectionByName("Spring Event").isPresent());
            assertEquals(collectionId, manager.getCollectionByName("Spring_Event").get().getId());
        }
    }

    @Test
    public void createCollectionSkipsSaveWhenNormalizedDuplicateExists() {
        UUID existingId = UUID.randomUUID();
        Collection existing = new Collection(existingId, "Spring_Event", true);

        try (Harness harness = new Harness(Collections.singletonList(existing), Collections.singletonList(existing))) {
            CollectionManager manager = harness.createManager();
            manager.reloadCollections().join();

            assertFalse(manager.createCollection("Spring Event").join());

            verify(harness.repository, times(0)).saveCollection(any(Collection.class));
            assertEquals(existingId, manager.getCollectionByName("Spring_Event").get().getId());
        }
    }

    @Test
    public void deleteCollectionClearsTreasureCacheAndReloadsCollectionCache() {
        UUID collectionId = UUID.randomUUID();
        Collection existing = new Collection(collectionId, "Autumn_Event", true);

        try (Harness harness = new Harness(Collections.singletonList(existing), Collections.<Collection>emptyList())) {
            when(harness.repository.deleteCollection(collectionId)).thenReturn(CompletableFuture.completedFuture(null));

            CollectionManager manager = harness.createManager();
            manager.reloadCollections().join();
            manager.deleteCollection(collectionId).join();

            assertFalse(manager.getCollectionById(collectionId).isPresent());
            assertTrue(manager.getAllCollections().isEmpty());
            assertEquals(collectionId, harness.treasureManager.removedCollectionId);
        }
    }
}