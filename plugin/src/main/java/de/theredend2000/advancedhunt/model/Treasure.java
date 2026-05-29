package de.theredend2000.advancedhunt.model;

import org.bukkit.Location;

import java.util.*;

public class Treasure {
    private final UUID id;
    private final UUID collectionId;
    private final Location location;
    private final List<Reward> rewards;
    private final String nbtData;
    private final String material;
    private final String blockState;

    public Treasure(UUID id, UUID collectionId, Location location, List<Reward> rewards, String nbtData, String material, String blockState) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.collectionId = Objects.requireNonNull(collectionId, "collectionId must not be null");
        this.location = location != null ? location.clone() : null;
        this.rewards = rewards != null ? new ArrayList<>(rewards) : new ArrayList<>();
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
        return location != null ? location.clone() : null;
    }

    public List<Reward> getRewards() {
        return Collections.unmodifiableList(rewards);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Treasure)) return false;
        Treasure that = (Treasure) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        String locationSummary;
        if (location == null) {
            locationSummary = "null";
        } else {
            String worldName = location.getWorld() != null ? location.getWorld().getName() : "null";
            locationSummary = worldName + "@" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        }

        return "Treasure{" +
                "id=" + id +
                ", collectionId=" + collectionId +
                ", location=" + locationSummary +
                ", rewardsCount=" + rewards.size() +
                ", material='" + material + '\'' +
                '}';
    }
}
