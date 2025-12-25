package de.theredend2000.advancedHunt.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData {
    private final UUID playerUuid;
    private final Set<UUID> foundTreasures;
    private UUID selectedCollectionId;

    public PlayerData(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.foundTreasures = new HashSet<>();
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public Set<UUID> getFoundTreasures() {
        return foundTreasures;
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

    public UUID getSelectedCollectionId() {
        return selectedCollectionId;
    }

    public void setSelectedCollectionId(UUID selectedCollectionId) {
        this.selectedCollectionId = selectedCollectionId;
    }
}
