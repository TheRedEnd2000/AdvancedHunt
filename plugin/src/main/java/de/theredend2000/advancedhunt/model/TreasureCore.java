package de.theredend2000.advancedhunt.model;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;
import java.util.UUID;

/**
 * Lightweight treasure representation containing only essential fields.
 * Used for efficient caching and lookups without loading heavy data like NBT.
 * 
 * Memory footprint: ~150 bytes per treasure vs ~2-10KB for full Treasure with NBT
 */
public class TreasureCore {
    private final UUID id;
    private final UUID collectionId;
    private final Location location;
    private final String material;
    private final String blockState;

    public TreasureCore(UUID id, UUID collectionId, Location location, String material, String blockState) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.collectionId = Objects.requireNonNull(collectionId, "collectionId cannot be null");
        this.location = location != null ? location.clone() : null;
        this.material = Objects.requireNonNull(material, "material cannot be null");
        this.blockState = blockState;
    }

    public TreasureCore(UUID id, UUID collectionId, Location location, String material) {
        this(id, collectionId, location, material, null);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCollectionId() {
        return collectionId;
    }

    public Location getLocation() {
        return location != null ? location.clone() : null;
    }

    public String getMaterial() {
        return material;
    }

    public String getBlockState() {
        return blockState;
    }

    public boolean isInLoadedWorld(World world) {
        return location != null
            && location.getWorld() != null
            && world != null
            && location.getWorld().getName().equals(world.getName());
    }

    public String getWorldNameOr(String fallback) {
        if (location == null || location.getWorld() == null) {
            return fallback;
        }
        return location.getWorld().getName();
    }
    
    /**
     * Creates a TreasureCore from a full Treasure object.
     */
    public static TreasureCore from(Treasure treasure) {
        Objects.requireNonNull(treasure, "treasure cannot be null");
        return new TreasureCore(
            treasure.getId(),
            treasure.getCollectionId(),
            treasure.getLocation(),
            treasure.getMaterial(),
            treasure.getBlockState()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TreasureCore)) return false;
        TreasureCore that = (TreasureCore) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        String locationSummary = location != null 
            ? String.format("%s@(%d,%d,%d)", 
                location.getWorld() != null ? location.getWorld().getName() : "null",
                location.getBlockX(), location.getBlockY(), location.getBlockZ())
            : "null";
        return String.format("TreasureCore{id=%s, material=%s, location=%s}", id, material, locationSummary);
    }
}
