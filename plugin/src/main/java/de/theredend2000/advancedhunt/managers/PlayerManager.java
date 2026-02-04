package de.theredend2000.advancedhunt.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerManager implements Listener {

    private static final long DEFAULT_TIMEOUT_MS = 5000L;
    
    private final DataRepository repository;
    private final Cache<UUID, PlayerData> playerDataCache;
    private final Logger logger;

    public PlayerManager(JavaPlugin plugin, DataRepository repository) {
        this.repository = repository;
        this.logger = plugin.getLogger();
        this.playerDataCache = Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES) // Keep data for 10 mins after last access (or quit)
                .maximumSize(1000) // Prevent memory leaks on massive servers
                .build();
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Gets player data from cache, or returns a placeholder and loads asynchronously.
     * This method is non-blocking and safe to call from the main thread.
     * 
     * @param uuid The player's UUID
     * @return Cached PlayerData if available, otherwise a placeholder that will be updated async
     */
    public PlayerData getPlayerData(UUID uuid) {
        // Try cache first - non-blocking
        PlayerData cached = playerDataCache.getIfPresent(uuid);
        if (cached != null) {
            return cached;
        }
        
        // Return placeholder immediately, load in background
        PlayerData placeholder = new PlayerData(uuid);
        playerDataCache.put(uuid, placeholder);
        
        // Start async load - updates cache when complete
        repository.loadPlayerData(uuid).thenAccept(loaded -> {
            if (loaded != null) {
                playerDataCache.put(uuid, loaded);
            }
            // If null, keep the placeholder we already put in cache
        }).exceptionally(ex -> {
            logger.log(Level.WARNING, "Failed to load player data for " + uuid, ex);
            return null;
        });
        
        return placeholder;
    }
    
    /**
     * Gets player data with a timeout. Will block up to timeoutMs waiting for data.
     * Falls back to placeholder data if timeout is exceeded or an error occurs.
     * 
     * @param uuid The player's UUID
     * @param timeoutMs Maximum time to wait in milliseconds
     * @return PlayerData from cache/database, or placeholder if timeout/error
     */
    public PlayerData getPlayerDataWithTimeout(UUID uuid, long timeoutMs) {
        // Try cache first
        PlayerData cached = playerDataCache.getIfPresent(uuid);
        if (cached != null) {
            return cached;
        }
        
        try {
            PlayerData loaded = repository.loadPlayerData(uuid)
                .get(timeoutMs, TimeUnit.MILLISECONDS);
            
            if (loaded == null) {
                loaded = new PlayerData(uuid);
            }
            
            playerDataCache.put(uuid, loaded);
            return loaded;
        } catch (java.util.concurrent.TimeoutException e) {
            logger.log(Level.WARNING, "Timeout loading player data for " + uuid);
            PlayerData placeholder = new PlayerData(uuid);
            playerDataCache.put(uuid, placeholder);
            return placeholder;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception loading player data for " + uuid, e);
            PlayerData placeholder = new PlayerData(uuid);
            playerDataCache.put(uuid, placeholder);
            return placeholder;
        }
    }
    
    /**
     * Gets player data with the default timeout of 5 seconds.
     * 
     * @param uuid The player's UUID
     * @return PlayerData from cache/database, or placeholder if timeout/error
     */
    public PlayerData getPlayerDataWithTimeout(UUID uuid) {
        return getPlayerDataWithTimeout(uuid, DEFAULT_TIMEOUT_MS);
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
        // We rely on Caffeine's expireAfterAccess to clean up memory later
    }
    
    public void invalidate(UUID uuid) {
        playerDataCache.invalidate(uuid);
    }

    public void stop() {
        HandlerList.unregisterAll(this);
        playerDataCache.invalidateAll();
    }
}
