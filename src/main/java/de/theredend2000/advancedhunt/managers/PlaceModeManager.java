package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.util.MessageUtils;
import de.theredend2000.advancedhunt.util.PlayerSnapshot;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlaceModeManager {

    private final Map<UUID, UUID> placeModePlayers = new ConcurrentHashMap<>();
    private final Main plugin;

    public PlaceModeManager(Main plugin){
        this.plugin = plugin;
    }

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

    public void processTick(List<PlayerSnapshot> snapshots) {
        for(PlayerSnapshot snapshot : snapshots){
            Player player = snapshot.player();
            if(isInPlaceMode(player))
                MessageUtils.sendActionBar(snapshot.player(), plugin.getMessageManager().getMessage("proximity.in_place_mode"));
        }
    }
}
