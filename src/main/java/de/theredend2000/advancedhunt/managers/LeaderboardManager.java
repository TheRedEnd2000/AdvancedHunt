package de.theredend2000.advancedhunt.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.model.Collection;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class LeaderboardManager {

    private final JavaPlugin plugin;
    private final DataRepository repository;
    private final Map<String, List<LeaderboardEntry>> leaderboardCache = new ConcurrentHashMap<>();
    private final Cache<UUID, String> nameCache;
    private final int displayLimit;
    private BukkitTask updateTask;

    public LeaderboardManager(JavaPlugin plugin, DataRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        this.displayLimit = plugin.getConfig().getInt("leaderboard.display-limit", 100);
        int refreshInterval = plugin.getConfig().getInt("leaderboard.cache-refresh-interval", 300);
        
        // Name cache: UUID -> PlayerName, expires after 30 minutes, max 5000 entries
        this.nameCache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(5000)
                .build();
        
        // Update leaderboards based on config interval
        long intervalTicks = 20L * refreshInterval;
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateLeaderboards, 0L, intervalTicks);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void updateLeaderboards() {
        // For YAML backend, trigger leaderboard cache rebuild first
        // This is a no-op for SQL which calculates on-demand
        repository.rebuildLeaderboardCache();
        
        repository.loadCollections().thenAccept(collections -> {
            for (Collection c : collections) {
                repository.getLeaderboard(c.getId(), displayLimit).thenAccept(map -> {
                    List<LeaderboardEntry> entries = new ArrayList<>();
                    int rank = 1;
                    for (Map.Entry<UUID, Integer> entry : map.entrySet()) {
                        UUID playerId = entry.getKey();
                        // Use cached name or fetch and cache it
                        String playerName = nameCache.get(playerId, uuid -> {
                            String name = Bukkit.getOfflinePlayer(uuid).getName();
                            return name != null ? name : "Unknown";
                        });
                        entries.add(new LeaderboardEntry(rank++, playerId, playerName, entry.getValue()));
                    }
                    leaderboardCache.put(c.getName(), entries);
                });
            }
        });
        
        // Also flush any pending index data (for YAML backend)
        repository.flushIndexes();
    }

    public LeaderboardEntry getEntry(String collectionName, int rank) {
        List<LeaderboardEntry> entries = leaderboardCache.get(collectionName);
        if (entries == null || rank < 1 || rank > entries.size()) {
            return null;
        }
        return entries.get(rank - 1);
    }
    
    public List<LeaderboardEntry> getEntries(String collectionName) {
        return leaderboardCache.get(collectionName);
    }
    
    public void forceUpdate() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::updateLeaderboards);
    }

    public static class LeaderboardEntry {
        private final int rank;
        private final UUID playerId;
        private final String playerName;
        private final int score;

        public LeaderboardEntry(int rank, UUID playerId, String playerName, int score) {
            this.rank = rank;
            this.playerId = playerId;
            this.playerName = playerName;
            this.score = score;
        }

        public int getRank() {
            return rank;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public String getPlayerName() {
            return playerName;
        }

        public int getScore() {
            return score;
        }
    }
}
