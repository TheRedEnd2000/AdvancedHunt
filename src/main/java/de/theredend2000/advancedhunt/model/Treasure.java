package de.theredend2000.advancedHunt.model;

import org.bukkit.Location;

import java.util.List;
import java.util.UUID;

public class Treasure {
    private final UUID id;
    private final UUID collectionId;
    private final Location location;
    private final List<Reward> rewards;
    private final String nbtData;
    private final String material;
    private final String blockState;

    public Treasure(UUID id, UUID collectionId, Location location, List<Reward> rewards, String nbtData, String material, String blockState) {
        this.id = id;
        this.collectionId = collectionId;
        this.location = location;
        this.rewards = rewards;
        this.nbtData = nbtData;
        this.material = material;
        this.blockState = blockState;
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

    public List<Reward> getRewards() {
        return rewards;
    }

    public String getNbtData() {
        return nbtData;
    }

    public String getMaterial() {
        return material;
    }

    public String getBlockState() {
        return blockState;
    }
}
