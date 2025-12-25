package de.theredend2000.advancedHunt.managers;

import de.theredend2000.advancedHunt.model.Collection;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlaceModeManager {

    private final Map<UUID, UUID> placeModePlayers = new ConcurrentHashMap<>();

    public void setPlaceMode(Player player, Collection collection) {
        placeModePlayers.put(player.getUniqueId(), collection.getId());
    }

    public void removePlaceMode(Player player) {
        placeModePlayers.remove(player.getUniqueId());
    }

    public boolean isInPlaceMode(Player player) {
        return placeModePlayers.containsKey(player.getUniqueId());
    }

    public UUID getCollectionId(Player player) {
        return placeModePlayers.get(player.getUniqueId());
    }
}
