package de.theredend2000.advancedHunt.model;

import org.bukkit.Location;

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

    public TreasureCore(UUID id, UUID collectionId, Location location, String material) {
        this.id = id;
        this.collectionId = collectionId;
        this.location = location;
        this.material = material;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCollectionId() {
        return collectionId;
    }

    public Location getLocation() {
        return location;
    }

    public String getMaterial() {
        return material;
    }
    
    /**
     * Creates a TreasureCore from a full Treasure object.
     */
    public static TreasureCore from(Treasure treasure) {
        return new TreasureCore(
            treasure.getId(),
            treasure.getCollectionId(),
            treasure.getLocation(),
            treasure.getMaterial()
        );
    }
}
