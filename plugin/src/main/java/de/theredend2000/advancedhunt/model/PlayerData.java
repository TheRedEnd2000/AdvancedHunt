package de.theredend2000.advancedhunt.model;

import java.util.*;

public class PlayerData {
    private final UUID playerUuid;
    private final Set<UUID> foundTreasures;

    public PlayerData(UUID playerUuid) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        this.foundTreasures = new HashSet<>();
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public Set<UUID> getFoundTreasures() {
        return Collections.unmodifiableSet(foundTreasures);
    }

    public void addFoundTreasure(UUID treasureId) {
        foundTreasures.add(treasureId);
    }

    public boolean hasFound(UUID treasureId) {
        return foundTreasures.contains(treasureId);
    }

    public void reset() {
        foundTreasures.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerData)) return false;
        PlayerData that = (PlayerData) o;
        return playerUuid.equals(that.playerUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerUuid);
    }

    @Override
    public String toString() {
        return "PlayerData{playerUuid=" + playerUuid + ", foundCount=" + foundTreasures.size() + "}";
    }
}
