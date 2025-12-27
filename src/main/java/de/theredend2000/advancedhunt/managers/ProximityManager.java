package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.TreasureCore;
import de.theredend2000.advancedhunt.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class ProximityManager {

    private final Main plugin;
    private final TreasureManager treasureManager;
    private final PlayerManager playerManager;
    private final CollectionManager collectionManager;
    private BukkitTask task;
    private boolean enabled;
    private int range;
    private int rangeSq;

    public ProximityManager(Main plugin, TreasureManager treasureManager, PlayerManager playerManager, CollectionManager collectionManager) {
        this.plugin = plugin;
        this.treasureManager = treasureManager;
        this.playerManager = playerManager;
        this.collectionManager = collectionManager;
        reloadConfig();
    }

    public void reloadConfig() {
        this.enabled = plugin.getConfig().getBoolean("proximity-settings.enabled", false);
        this.range = plugin.getConfig().getInt("proximity-settings.range", 10);
        this.rangeSq = range * range;
        
        if (enabled && range > 0) {
            start();
        } else {
            stop();
        }
    }

    public void start() {
        stop();
        if (!enabled || range <= 0) return;

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayer(player);
        }
    }

    private void checkPlayer(Player player) {
        Location playerLoc = player.getLocation();
        int chunkX = playerLoc.getBlockX() >> 4;
        int chunkZ = playerLoc.getBlockZ() >> 4;
        int chunkRadius = (int) Math.ceil((double) range / 16.0);

        boolean foundNear = false;

        for (int x = chunkX - chunkRadius; x <= chunkX + chunkRadius; x++) {
            for (int z = chunkZ - chunkRadius; z <= chunkZ + chunkRadius; z++) {
                List<TreasureCore> treasures = treasureManager.getTreasureCoresInChunk(x, z);
                if (treasures == null || treasures.isEmpty()) continue;

                for (TreasureCore treasure : treasures) {
                    if (!treasure.getLocation().getWorld().equals(playerLoc.getWorld())) continue;
                    
                    if (treasure.getLocation().distanceSquared(playerLoc) <= rangeSq) {
                        // Check if player has found it
                        if (!playerManager.getPlayerData(player.getUniqueId()).hasFound(treasure.getId())) {
                            // Check if collection is available
                            if (collectionManager.getCollectionById(treasure.getCollectionId())
                                    .map(collectionManager::isCollectionAvailable)
                                    .orElse(false)) {
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
            MessageUtils.sendActionBar(player, plugin.getMessageManager().getMessage("proximity.near_treasure"));
        }
    }
}
