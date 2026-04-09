package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.TreasureCore;
import de.theredend2000.advancedhunt.util.MessageUtils;
import de.theredend2000.advancedhunt.util.PlayerSnapshot;
import org.bukkit.Location;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ProximityManager {

    private final Main plugin;
    private final TreasureManager treasureManager;
    private final PlayerManager playerManager;
    private boolean enabled;
    private int range;
    private int rangeSq;

    public ProximityManager(Main plugin, TreasureManager treasureManager, PlayerManager playerManager) {
        this.plugin = plugin;
        this.treasureManager = treasureManager;
        this.playerManager = playerManager;
        reloadConfig();
    }

    public void reloadConfig() {
        this.enabled = plugin.getConfig().getBoolean("proximity-settings.enabled", false);
        this.range = plugin.getConfig().getInt("proximity-settings.range", 10);
        this.rangeSq = range * range;
    }

    public void processTick(List<PlayerSnapshot> snapshots, Set<UUID> availableCollections) {
        if (!enabled || range <= 0) return;

        for (PlayerSnapshot snapshot : snapshots) {
            checkPlayer(snapshot, availableCollections);
        }
    }

    private void checkPlayer(PlayerSnapshot snapshot, Set<UUID> availableCollections) {
        Location playerLoc = snapshot.location();
        if (playerLoc == null || playerLoc.getWorld() == null) {
            return;
        }

        int chunkX = playerLoc.getBlockX() >> 4;
        int chunkZ = playerLoc.getBlockZ() >> 4;
        int chunkRadius = (int) Math.ceil((double) range / 16.0);

        boolean foundNear = false;

        for (int x = chunkX - chunkRadius; x <= chunkX + chunkRadius; x++) {
            for (int z = chunkZ - chunkRadius; z <= chunkZ + chunkRadius; z++) {
                List<TreasureCore> treasures = treasureManager.getTreasureCoresInChunk(x, z);
                if (treasures == null || treasures.isEmpty()) continue;

                for (TreasureCore treasure : treasures) {
                    Location treasureLoc = treasure.getLocation();
                    if (treasureLoc == null || !treasure.isInLoadedWorld(playerLoc.getWorld())) continue;
                    
                    if (treasureLoc.distanceSquared(playerLoc) <= rangeSq) {
                        // Check if player has found it
                        if (!playerManager.getPlayerData(snapshot.player().getUniqueId()).hasFound(treasure.getId())) {
                            // Check if collection is available (O(1) lookup)
                            if (availableCollections.contains(treasure.getCollectionId())) {
                                foundNear = true;
                                break;
                            }
                        }
                    }
                }
                if (foundNear) break;
            }
            if (foundNear) break;
        }

        if (foundNear) {
            MessageUtils.sendActionBar(snapshot.player(), plugin.getMessageManager().getMessage("proximity.near_treasure"));
        }
    }
}
