package de.theredend2000.advancedhunt.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.model.Treasure;
import de.theredend2000.advancedhunt.model.TreasureCore;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Manages treasure data with lazy loading for memory efficiency.
 * 
 * Architecture:
 * - TreasureCore objects (lightweight, ~150 bytes each) are always in memory
 * - Full Treasure objects (heavy, ~2-10KB with NBT) are loaded on-demand and cached
 * - Chunk and collection indexes use TreasureCore for fast spatial lookups
 */
public class TreasureManager {

    private final DataRepository repository;
    
    // Lightweight indexes using TreasureCore (~150 bytes per treasure)
    // Key: Chunk Key (x, z), Value: List of TreasureCores in that chunk
    private final Map<Long, List<TreasureCore>> treasureChunkMap = new ConcurrentHashMap<>();
    // Key: Collection ID, Value: List of TreasureCores in that collection
    private final Map<UUID, List<TreasureCore>> collectionTreasureMap = new ConcurrentHashMap<>();
    // Lightweight index: Treasure ID -> Collection ID (for O(1) collection lookups)
    private final Map<UUID, UUID> treasureToCollectionIndex = new ConcurrentHashMap<>();
    // Core data by ID for fast lookups
    private final Map<UUID, TreasureCore> treasureCoreById = new ConcurrentHashMap<>();
    
    // On-demand cache for full Treasure objects (heavy data loaded when needed)
    // Expires after 5 minutes, max 500 entries to prevent memory bloat
    private final Cache<UUID, Treasure> fullTreasureCache;
    
    public TreasureManager(DataRepository repository) {
        this.repository = repository;
        this.fullTreasureCache = Caffeine.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();
    }

    public void loadTreasures() {
        repository.loadTreasures().thenAccept(treasures -> {
            treasureChunkMap.clear();
            collectionTreasureMap.clear();
            treasureToCollectionIndex.clear();
            treasureCoreById.clear();
            fullTreasureCache.invalidateAll();
            
            for (Treasure t : treasures) {
                TreasureCore core = TreasureCore.from(t);
                addCoreToCache(core);
                // Also cache the full treasure since we just loaded it
                fullTreasureCache.put(t.getId(), t);
            }
        });
    }

    public void addTreasure(Treasure t) {
        repository.saveTreasure(t).thenRun(() -> {
            TreasureCore core = TreasureCore.from(t);
            addCoreToCache(core);
            fullTreasureCache.put(t.getId(), t);
        });
    }

    public void deleteTreasure(Treasure t) {
        repository.deleteTreasure(t.getId()).thenRun(() -> {
            removeCoreFromCache(t.getId(), t.getCollectionId(), t.getLocation());
            fullTreasureCache.invalidate(t.getId());
        });
    }
    
    /**
     * Deletes a treasure by its core data (no full treasure needed).
     */
    public void deleteTreasure(TreasureCore core) {
        repository.deleteTreasure(core.getId()).thenRun(() -> {
            removeCoreFromCache(core.getId(), core.getCollectionId(), core.getLocation());
            fullTreasureCache.invalidate(core.getId());
        });
    }

    /**
     * Removes all treasures in a collection from the cache.
     * Does NOT delete from repository (assumed to be handled by caller).
     */
    public void removeCollection(UUID collectionId) {
        List<TreasureCore> cores = collectionTreasureMap.remove(collectionId);
        if (cores != null) {
            for (TreasureCore core : cores) {
                // Remove from chunk map
                long key = getChunkKey(core.getLocation());
                List<TreasureCore> chunkList = treasureChunkMap.get(key);
                if (chunkList != null) {
                    chunkList.removeIf(c -> c.getId().equals(core.getId()));
                }
                
                // Remove from other indexes
                treasureToCollectionIndex.remove(core.getId());
                treasureCoreById.remove(core.getId());
                fullTreasureCache.invalidate(core.getId());
            }
        }
    }

    /**
     * Updates an existing treasure by replacing it with a new instance.
     * Used when modifying rewards or other treasure properties.
     */
    public void updateTreasure(Treasure oldTreasure, Treasure newTreasure) {
        // Remove old from cache first
        removeCoreFromCache(oldTreasure.getId(), oldTreasure.getCollectionId(), oldTreasure.getLocation());
        fullTreasureCache.invalidate(oldTreasure.getId());
        
        // Save new treasure and add to cache
        repository.saveTreasure(newTreasure).thenRun(() -> {
            TreasureCore core = TreasureCore.from(newTreasure);
            addCoreToCache(core);
            fullTreasureCache.put(newTreasure.getId(), newTreasure);
        });
    }

    private void addCoreToCache(TreasureCore core) {
        long key = getChunkKey(core.getLocation());
        treasureChunkMap.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(core);
        collectionTreasureMap.computeIfAbsent(core.getCollectionId(), k -> new CopyOnWriteArrayList<>()).add(core);
        treasureToCollectionIndex.put(core.getId(), core.getCollectionId());
        treasureCoreById.put(core.getId(), core);
    }

    private void removeCoreFromCache(UUID treasureId, UUID collectionId, Location location) {
        long key = getChunkKey(location);
        List<TreasureCore> chunkList = treasureChunkMap.get(key);
        if (chunkList != null) {
            chunkList.removeIf(core -> core.getId().equals(treasureId));
        }
        List<TreasureCore> collectionList = collectionTreasureMap.get(collectionId);
        if (collectionList != null) {
            collectionList.removeIf(core -> core.getId().equals(treasureId));
        }
        treasureToCollectionIndex.remove(treasureId);
        treasureCoreById.remove(treasureId);
    }

    // ==================== LIGHTWEIGHT ACCESSORS (TreasureCore) ====================
    
    /**
     * Gets lightweight treasure cores in a collection (no heavy data).
     * Use this for iteration, counting, and display purposes.
     */
    public List<TreasureCore> getTreasureCoresInCollection(UUID collectionId) {
        return collectionTreasureMap.getOrDefault(collectionId, Collections.emptyList());
    }
    
    /**
     * Gets lightweight treasure cores in a chunk (no heavy data).
     */
    public List<TreasureCore> getTreasureCoresInChunk(int chunkX, int chunkZ) {
        long key = (long) chunkX << 32 | (chunkZ & 0xFFFFFFFFL);
        return treasureChunkMap.getOrDefault(key, Collections.emptyList());
    }
    
    /**
     * Gets a treasure core at a specific location (fast, no heavy data).
     */
    public TreasureCore getTreasureCoreAt(Location loc) {
        long key = getChunkKey(loc);
        List<TreasureCore> inChunk = treasureChunkMap.get(key);
        if (inChunk == null) return null;

        for (TreasureCore core : inChunk) {
            if (locationsEqual(core.getLocation(), loc)) return core;
        }
        return null;
    }
    
    /**
     * Gets a treasure core by ID (fast, no heavy data).
     */
    public TreasureCore getTreasureCoreById(UUID treasureId) {
        return treasureCoreById.get(treasureId);
    }
    
    // ==================== FULL TREASURE ACCESSORS (On-Demand) ====================
    
    /**
     * Gets the full treasure at a location, loading heavy data on demand.
     * Returns cached instance if available, otherwise loads from repository.
     * 
     * @deprecated For most use cases, prefer {@link #getTreasureCoreAt(Location)} 
     *             and only load full treasure when NBT/rewards are needed.
     */
    @Deprecated
    public Treasure getTreasureAt(Location loc) {
        TreasureCore core = getTreasureCoreAt(loc);
        if (core == null) return null;
        return getFullTreasure(core.getId());
    }
    
    /**
     * Gets the full treasure by ID, loading heavy data on demand.
     * Uses Caffeine cache for recently accessed treasures.
     */
    public Treasure getFullTreasure(UUID treasureId) {
        // Check cache first
        Treasure cached = fullTreasureCache.getIfPresent(treasureId);
        if (cached != null) {
            return cached;
        }
        
        // Load from repository (blocking for now, could be made async)
        Treasure loaded = repository.loadTreasure(treasureId).join();
        if (loaded != null) {
            fullTreasureCache.put(treasureId, loaded);
        }
        return loaded;
    }
    
    /**
     * Gets the full treasure by ID asynchronously.
     * Preferred for non-blocking operations.
     */
    public CompletableFuture<Treasure> getFullTreasureAsync(UUID treasureId) {
        // Check cache first
        Treasure cached = fullTreasureCache.getIfPresent(treasureId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        // Load from repository
        return repository.loadTreasure(treasureId).thenApply(loaded -> {
            if (loaded != null) {
                fullTreasureCache.put(treasureId, loaded);
            }
            return loaded;
        });
    }
    
    /**
     * Gets all full treasures in a collection.
     * WARNING: This loads heavy data for all treasures. Use sparingly.
     * For counting/iteration, prefer {@link #getTreasureCoresInCollection(UUID)}.
     * 
     * @deprecated Prefer {@link #getTreasureCoresInCollection(UUID)} for lightweight access.
     */
    @Deprecated
    public List<Treasure> getTreasuresInCollection(UUID collectionId) {
        List<TreasureCore> cores = getTreasureCoresInCollection(collectionId);
        List<Treasure> treasures = new ArrayList<>(cores.size());
        for (TreasureCore core : cores) {
            Treasure full = getFullTreasure(core.getId());
            if (full != null) {
                treasures.add(full);
            }
        }
        return treasures;
    }
    
    /**
     * Gets treasures in a chunk.
     * @deprecated Prefer {@link #getTreasureCoresInChunk(int, int)} for lightweight access.
     */
    @Deprecated
    public List<Treasure> getTreasuresInChunk(int chunkX, int chunkZ) {
        List<TreasureCore> cores = getTreasureCoresInChunk(chunkX, chunkZ);
        List<Treasure> treasures = new ArrayList<>(cores.size());
        for (TreasureCore core : cores) {
            Treasure full = getFullTreasure(core.getId());
            if (full != null) {
                treasures.add(full);
            }
        }
        return treasures;
    }

    // ==================== UTILITY METHODS ====================

    private boolean locationsEqual(Location l1, Location l2) {
        return l1.getWorld().getName().equals(l2.getWorld().getName()) &&
                l1.getBlockX() == l2.getBlockX() &&
                l1.getBlockY() == l2.getBlockY() &&
                l1.getBlockZ() == l2.getBlockZ();
    }

    private long getChunkKey(Location loc) {
        return (long) (loc.getBlockX() >> 4) << 32 | ((loc.getBlockZ() >> 4) & 0xFFFFFFFFL);
    }

    /**
     * Gets the collection ID for a treasure without loading the full treasure object.
     * O(1) lookup using the lightweight index.
     * @param treasureId the treasure UUID
     * @return the collection UUID, or null if not found
     */
    public UUID getCollectionIdForTreasure(UUID treasureId) {
        return treasureToCollectionIndex.get(treasureId);
    }

    /**
     * Filters a player's found treasures to only those in a specific collection.
     * Uses the lightweight index for O(n) where n = player's found count.
     * @param foundTreasures the set of treasure IDs the player has found
     * @param collectionId the collection to filter by
     * @return set of treasure IDs found in the specified collection
     */
    public Set<UUID> filterFoundByCollection(Set<UUID> foundTreasures, UUID collectionId) {
        Set<UUID> result = new HashSet<>();
        for (UUID treasureId : foundTreasures) {
            UUID treasureCollectionId = treasureToCollectionIndex.get(treasureId);
            if (collectionId.equals(treasureCollectionId)) {
                result.add(treasureId);
            }
        }
        return result;
    }

    /**
     * Gets the count of treasures a player has found in a specific collection.
     * @param foundTreasures the set of treasure IDs the player has found
     * @param collectionId the collection to count for
     * @return count of found treasures in the collection
     */
    public int countFoundInCollection(Set<UUID> foundTreasures, UUID collectionId) {
        int count = 0;
        for (UUID treasureId : foundTreasures) {
            if (collectionId.equals(treasureToCollectionIndex.get(treasureId))) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets all treasure IDs in a collection (lightweight, no full treasure objects).
     * @param collectionId the collection UUID
     * @return set of treasure UUIDs in the collection
     */
    public Set<UUID> getTreasureIdsInCollection(UUID collectionId) {
        Set<UUID> result = new HashSet<>();
        for (Map.Entry<UUID, UUID> entry : treasureToCollectionIndex.entrySet()) {
            if (collectionId.equals(entry.getValue())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    
    /**
     * Gets the total number of treasures currently tracked.
     */
    public int getTreasureCount() {
        return treasureCoreById.size();
    }
    
    /**
     * Gets the number of full treasures currently cached.
     */
    public long getCachedFullTreasureCount() {
        return fullTreasureCache.estimatedSize();
    }
}
