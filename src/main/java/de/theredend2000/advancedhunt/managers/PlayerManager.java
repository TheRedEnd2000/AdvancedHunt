package de.theredend2000.advancedhunt.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlayerManager implements Listener {

    private final DataRepository repository;
    private final Cache<UUID, PlayerData> playerDataCache;

    public PlayerManager(JavaPlugin plugin, DataRepository repository) {
        this.repository = repository;
        this.playerDataCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES) // Keep data for 10 mins after last access (or quit)
                .maximumSize(1000) // Prevent memory leaks on massive servers
                .build();
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public PlayerData getPlayerData(UUID uuid) {
        // If online, it should be in cache. If not, load it.
        return playerDataCache.get(uuid, key -> repository.loadPlayerData(key).join());
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataCache.getIfPresent(uuid);
        if (data != null) {
            repository.savePlayerData(data);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Pre-load data asynchronously when player joins
        repository.loadPlayerData(event.getPlayer().getUniqueId())
                .thenAccept(data -> playerDataCache.put(event.getPlayer().getUniqueId(), data));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Save data on quit, but keep in cache for a bit in case they reconnect quickly
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerData data = playerDataCache.getIfPresent(uuid);
        if (data != null) {
            repository.savePlayerData(data);
        }
        // We rely on Caffeine's expireAfterWrite to clean up memory later
    }
    
    public void invalidate(UUID uuid) {
        playerDataCache.invalidate(uuid);
    }
}
