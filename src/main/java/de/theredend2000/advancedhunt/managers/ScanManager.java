package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.util.PlayerSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScanManager {
    private final Main plugin;
    private final CollectionManager collectionManager;
    private final ProximityManager proximityManager;
    private final ParticleManager particleManager;
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private BukkitTask task;

    public ScanManager(Main plugin, CollectionManager collectionManager, 
                       ProximityManager proximityManager, ParticleManager particleManager) {
        this.plugin = plugin;
        this.collectionManager = collectionManager;
        this.proximityManager = proximityManager;
        this.particleManager = particleManager;
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        if (processing.get()) return;
        processing.set(true);

        // 1. Gather Data (Sync)
        Set<UUID> availableCollections = new HashSet<>();
        Set<UUID> singlePlayerFindCollections = new HashSet<>();
        for (Collection c : collectionManager.getAllCollections()) {
            if (collectionManager.isCollectionAvailable(c)) {
                availableCollections.add(c.getId());
                if (c.isSinglePlayerFind()) {
                    singlePlayerFindCollections.add(c.getId());
                }
            }
        }

        List<PlayerSnapshot> snapshots = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            snapshots.add(new PlayerSnapshot(player, player.getLocation()));
        }

        // 2. Process (Async)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Run Proximity Logic
                proximityManager.processTick(snapshots, availableCollections);

                // Run Particle Logic
                particleManager.processTick(snapshots, availableCollections, singlePlayerFindCollections);
            } finally {
                processing.set(false);
            }
        });
    }
}
