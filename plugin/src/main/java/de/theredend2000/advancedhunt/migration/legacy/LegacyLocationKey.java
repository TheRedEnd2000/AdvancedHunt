package de.theredend2000.advancedhunt.migration.legacy;

import de.theredend2000.advancedhunt.managers.CollectionManager;

import java.util.Locale;
import java.util.Objects;

/**
 * A stable key to reconcile legacy placed eggs with new treasure UUIDs.
 *
 * Legacy does not store a treasure UUID, so we match by collection name + block coordinates.
 */
public final class LegacyLocationKey {

    private final String collectionName;
    private final String world;
    private final int x;
    private final int y;
    private final int z;

    public LegacyLocationKey(String collectionName, String world, int x, int y, int z) {
        this.collectionName = normalizeCollectionName(collectionName);
        this.world = normalize(world);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    private static String normalizeCollectionName(String s) {
        return CollectionManager.normalizeCollectionName(s);
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }

    public String collectionName() {
        return collectionName;
    }

    public String world() {
        return world;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LegacyLocationKey)) return false;
        LegacyLocationKey that = (LegacyLocationKey) o;
        return x == that.x && y == that.y && z == that.z && Objects.equals(collectionName, that.collectionName) && Objects.equals(world, that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collectionName, world, x, y, z);
    }

    @Override
    public String toString() {
        return "LegacyLocationKey{" +
            "collection='" + collectionName + '\'' +
            ", world='" + world + '\'' +
            ", x=" + x +
            ", y=" + y +
            ", z=" + z +
            '}';
    }
}
