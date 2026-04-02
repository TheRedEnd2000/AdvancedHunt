package de.theredend2000.advancedhunt.migration.legacy;

import de.theredend2000.advancedhunt.managers.CollectionManager;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class LegacyDataMigratorTest {

    @Test
    public void legacyLocationKeyNormalizesCollectionNamesLikePluginCollections() {
        LegacyLocationKey spaced = new LegacyLocationKey("Test collection", "World", 10, 20, 30);
        LegacyLocationKey slugged = new LegacyLocationKey("test_collection", "world", 10, 20, 30);

        assertEquals(spaced, slugged);
        assertEquals(spaced.hashCode(), slugged.hashCode());
    }

    @Test
    public void buildCollectionFallbackLinksAssignsTreasuresInDeterministicOrder() {
        LegacyLocationKey first = new LegacyLocationKey("Test collection", "world", 1, 2, 3);
        LegacyLocationKey second = new LegacyLocationKey("Test collection", "world", 4, 5, 6);

        Map<String, LinkedHashSet<LegacyLocationKey>> missingByCollection = new LinkedHashMap<>();
        LinkedHashSet<LegacyLocationKey> missingKeys = new LinkedHashSet<>();
        missingKeys.add(first);
        missingKeys.add(second);
        missingByCollection.put(CollectionManager.normalizeCollectionName("Test collection"), missingKeys);

        UUID treasureA = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID treasureB = UUID.fromString("00000000-0000-0000-0000-000000000002");

        Map<String, List<UUID>> treasureIdsByCollection = new LinkedHashMap<>();
        treasureIdsByCollection.put(CollectionManager.normalizeCollectionName("Test collection"), Arrays.asList(treasureA, treasureB));

        Map<LegacyLocationKey, UUID> resolved = LegacyDataMigrator.buildCollectionFallbackLinks(missingByCollection, treasureIdsByCollection);

        assertEquals(2, resolved.size());
        assertEquals(treasureA, resolved.get(first));
        assertEquals(treasureB, resolved.get(second));
    }

    @Test
    public void resolveMigratedSnapshotUsesStoneFallbackWhenSnapshotIsMissing() {
        LegacyDataMigrator.MigratedSnapshotData resolved = LegacyDataMigrator.resolveMigratedSnapshot(null);

        assertTrue(resolved.usedFallback);
        assertEquals("STONE", resolved.material);
        assertNull(resolved.blockState);
        assertNull(resolved.nbtData);
    }

    @Test
    public void resolveMigratedSnapshotPreservesCapturedBlockData() {
        LegacyBlockSnapshotter.Snapshot snapshot = LegacyBlockSnapshotter.Snapshot.of(
            "PLAYER_HEAD",
            "minecraft:player_head[rotation=0]",
            "{SkullOwner:\"GrafterCrafter\"}"
        );

        LegacyDataMigrator.MigratedSnapshotData resolved = LegacyDataMigrator.resolveMigratedSnapshot(snapshot);

        assertFalse(resolved.usedFallback);
        assertEquals("PLAYER_HEAD", resolved.material);
        assertEquals("minecraft:player_head[rotation=0]", resolved.blockState);
        assertEquals("{SkullOwner:\"GrafterCrafter\"}", resolved.nbtData);
    }
}