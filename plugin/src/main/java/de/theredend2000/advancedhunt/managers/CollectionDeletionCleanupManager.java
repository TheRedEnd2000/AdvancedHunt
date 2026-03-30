package de.theredend2000.advancedhunt.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.ItemsAdderAdapter;
import de.theredend2000.advancedhunt.util.MaterialUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Best-effort in-memory world-edit queue used when deleting collections.
 *
 * Important: edits are NOT persisted across restarts.
 */
public class CollectionDeletionCleanupManager implements Listener {

    private static final Material DEFAULT_FALLBACK_MATERIAL = Material.STONE;
    private static final int MAX_QUEUED_EDITS = 200_000;

    private final Main plugin;

    private final Map<ChunkKey, List<TreasureWorldEdit>> pendingByChunk = new ConcurrentHashMap<>();

    private final AtomicInteger queuedEdits = new AtomicInteger(0);

    // Avoid unbounded growth for chunks that never load.
    private final Cache<ChunkKey, Boolean> chunkTouched = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(100_000)
        .build();

    private BukkitTask cleanupTask;

    public CollectionDeletionCleanupManager(Main plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Periodically drop entries for chunks that haven't loaded in a while.
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            // Remove pending entries for keys that have expired from chunkTouched.
            // (Caffeine doesn't support direct iteration of expired entries; we do a cheap sweep.)
            int removedEdits = 0;
            for (ChunkKey key : pendingByChunk.keySet()) {
                if (chunkTouched.getIfPresent(key) == null) {
                    List<TreasureWorldEdit> removed = pendingByChunk.remove(key);
                    if (removed != null) {
                        removedEdits += removed.size();
                    }
                }
            }
            if (removedEdits > 0) {
                queuedEdits.addAndGet(-removedEdits);
            }
        }, 20L * 60L, 20L * 60L);
    }

    public void stop() {
        HandlerList.unregisterAll(this);
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        pendingByChunk.clear();
        chunkTouched.invalidateAll();
        queuedEdits.set(0);
    }

    /**
     * Schedule edits. Loaded chunks are applied immediately; unloaded chunks are queued
     * and applied when the chunk loads.
     */
    public void scheduleEdits(List<TreasureWorldEdit> edits) {
        if (edits == null || edits.isEmpty()) {
            return;
        }

        // This method touches Bukkit world/chunk state; ensure it always runs on the primary thread.
        if (!Bukkit.isPrimaryThread()) {
            List<TreasureWorldEdit> snapshot = new ArrayList<>(edits);
            Bukkit.getScheduler().runTask(plugin, () -> scheduleEdits(snapshot));
            return;
        }

        // Filter invalid edits early to keep accounting accurate.
        List<TreasureWorldEdit> validEdits = new ArrayList<>(edits.size());
        for (TreasureWorldEdit edit : edits) {
            if (edit == null) continue;
            String worldName = edit.getWorldName();
            if (worldName == null || worldName.trim().isEmpty()) continue;
            validEdits.add(edit);
        }
        if (validEdits.isEmpty()) {
            return;
        }

        int currentQueued = queuedEdits.get();
        int budget = MAX_QUEUED_EDITS - currentQueued;
        if (budget <= 0) {
            plugin.getLogger().warning("Collection deletion cleanup queue is full (" + currentQueued + " edits). Dropping " + validEdits.size() + " scheduled edits.");
            return;
        }

        if (validEdits.size() > budget) {
            plugin.getLogger().warning("Collection deletion cleanup queue nearing limit (" + currentQueued + "/" + MAX_QUEUED_EDITS + " edits). Truncating scheduled edits from " + validEdits.size() + " to " + budget + ".");
            validEdits = new ArrayList<>(validEdits.subList(0, budget));
        }

        queuedEdits.addAndGet(validEdits.size());

        // Group by chunk
        Map<ChunkKey, List<TreasureWorldEdit>> grouped = new HashMap<>();
        for (TreasureWorldEdit edit : validEdits) {
            ChunkKey key = new ChunkKey(edit.getWorldName(), edit.getChunkX(), edit.getChunkZ());
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(edit);
        }

        for (Map.Entry<ChunkKey, List<TreasureWorldEdit>> entry : grouped.entrySet()) {
            ChunkKey key = entry.getKey();
            List<TreasureWorldEdit> chunkEdits = entry.getValue();
            if (chunkEdits == null || chunkEdits.isEmpty()) continue;

            chunkTouched.put(key, Boolean.TRUE);

            World world = Bukkit.getWorld(key.worldName);
            if (world != null && world.isChunkLoaded(key.chunkX, key.chunkZ)) {
                Bukkit.getScheduler().runTask(plugin, () -> applyEdits(world, new ArrayList<>(chunkEdits)));
            } else {
                pendingByChunk.compute(key, (k, existing) -> {
                    if (existing == null) {
                        existing = Collections.synchronizedList(new ArrayList<>());
                    }
                    existing.addAll(chunkEdits);
                    return existing;
                });
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event == null || event.getChunk() == null || event.getWorld() == null) return;

        ChunkKey key = new ChunkKey(event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ());
        List<TreasureWorldEdit> edits = pendingByChunk.remove(key);
        if (edits == null || edits.isEmpty()) return;

        chunkTouched.put(key, Boolean.TRUE);
        applyEdits(event.getWorld(), new ArrayList<>(edits));
    }

    private void applyEdits(World world, List<TreasureWorldEdit> edits) {
        if (world == null || edits == null || edits.isEmpty()) return;

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int processed = 0;
                while (index < edits.size() && processed < 200) {
                    TreasureWorldEdit edit = edits.get(index++);
                    processed++;
                    try {
                        applySingleEdit(world, edit);
                    } catch (Throwable ignored) {
                    }
                }

                if (index >= edits.size()) {
                    queuedEdits.addAndGet(-edits.size());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void applySingleEdit(World world, TreasureWorldEdit edit) {
        if (world == null || edit == null) return;
        if (edit.getWorldName() == null || !world.getName().equalsIgnoreCase(edit.getWorldName())) return;

        Location loc = new Location(world, edit.getX(), edit.getY(), edit.getZ());

        if (edit.getAction() == TreasureWorldEdit.Action.REMOVE) {
            removeTreasureBlock(edit, loc);
            return;
        }

        restoreTreasureBlock(edit, loc);
    }

    private boolean isItemsAdder(TreasureWorldEdit edit) {
        return edit != null && edit.getMaterial() != null && "ITEMS_ADDER".equalsIgnoreCase(edit.getMaterial());
    }

    private boolean isItemsAdderFurniture(TreasureWorldEdit edit) {
        if (edit == null) return false;
        String blockState = edit.getBlockState();
        if (blockState == null || blockState.isEmpty()) return false;
        return ItemsAdderAdapter.isCustomFurnitureId(blockState);
    }

    private void removeTreasureBlock(TreasureWorldEdit edit, Location loc) {
        if (edit == null || loc == null || loc.getWorld() == null) return;

        World world = loc.getWorld();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        if (!world.isChunkLoaded(cx, cz)) return;

        if (isItemsAdder(edit)) {
            if (isItemsAdderFurniture(edit)) {
                ItemsAdderAdapter.removeCustomFurniture(loc);
            } else {
                if (!ItemsAdderAdapter.removeCustomBlock(loc)) {
                    world.getBlockAt(loc).setType(Material.AIR, false);
                }
            }
            return;
        }

        Block block = world.getBlockAt(loc);
        if (!MaterialUtils.isAir(block.getType())) {
            block.setType(Material.AIR, false);
        }
    }

    private void restoreTreasureBlock(TreasureWorldEdit edit, Location loc) {
        if (edit == null || loc == null || loc.getWorld() == null) return;

        World world = loc.getWorld();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        if (!world.isChunkLoaded(cx, cz)) return;

        if (isItemsAdder(edit)) {
            if (isItemsAdderFurniture(edit)) {
                ItemsAdderAdapter.spawnCustomFurniture(edit.getBlockState(), loc);
            } else {
                ItemsAdderAdapter.placeCustomBlock(edit.getBlockState(), loc);
            }
            return;
        }

        Block block = world.getBlockAt(loc);
        if (!MaterialUtils.isAir(block.getType())) {
            return;
        }

        Material type = resolveMaterial(edit.getMaterial());
        if (type == null) return;

        BlockData data = resolveBlockData(edit.getBlockState(), type);
        try {
            if (data != null) {
                block.setBlockData(data, false);
            } else {
                block.setType(type, false);
            }
        } catch (Throwable ignored) {
            block.setType(type, false);
        }

        applyNbtIfPresent(block, edit.getNbtData());
    }

    private void applyNbtIfPresent(Block block, String nbtData) {
        if (block == null) return;
        if (nbtData == null || nbtData.isEmpty()) return;

        try {
            BlockState state = block.getState();
            NBT.modify(state, nbt -> {
                try {
                    ReadWriteNBT data = NBT.parseNBT(nbtData);
                    nbt.mergeCompound(data);
                } catch (Throwable ignored) {
                }
            });
            state.update(true, false);
        } catch (Throwable ignored) {
        }
    }

    private BlockData resolveBlockData(String blockState, Material fallbackMaterial) {
        if (blockState != null && !blockState.isEmpty() && !isLegacyBlockData(blockState)) {
            try {
                return Bukkit.createBlockData(blockState);
            } catch (Throwable ignored) {
            }
        }

        if (fallbackMaterial != null) {
            try {
                return fallbackMaterial.createBlockData();
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private Material resolveMaterial(String materialName) {
        if (materialName == null || materialName.isEmpty()) return null;
        String key = materialName.toUpperCase(Locale.ROOT);
        try {
            Material material = Material.getMaterial(key);
            if (material != null) {
                return material;
            }
        } catch (Throwable ignored) {
        }
        return DEFAULT_FALLBACK_MATERIAL;
    }

    private boolean isLegacyBlockData(String blockState) {
        if (blockState == null || blockState.isEmpty()) return false;
        for (int i = 0; i < blockState.length(); i++) {
            if (!Character.isDigit(blockState.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static final class ChunkKey {
        private final String worldName;
        private final int chunkX;
        private final int chunkZ;

        private ChunkKey(String worldName, int chunkX, int chunkZ) {
            this.worldName = worldName == null ? "" : worldName.toLowerCase(java.util.Locale.ROOT);
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChunkKey)) return false;
            ChunkKey that = (ChunkKey) o;
            if (chunkX != that.chunkX) return false;
            if (chunkZ != that.chunkZ) return false;
            return worldName.equals(that.worldName);
        }

        @Override
        public int hashCode() {
            int result = worldName.hashCode();
            result = 31 * result + chunkX;
            result = 31 * result + chunkZ;
            return result;
        }
    }
}
