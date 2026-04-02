package de.theredend2000.advancedhunt.migration.legacy;

import de.theredend2000.advancedhunt.util.BlockUtils;
import de.theredend2000.advancedhunt.util.ItemsAdderAdapter;
import de.theredend2000.advancedhunt.util.MaterialUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Snapshots live blocks into (material, block-state, nbt-data) using the same
 * logic as place mode, so migrated treasures render correctly.
 */
public final class LegacyBlockSnapshotter {

    public static final class Snapshot {
        public final String material;
        public final String blockState;
        public final String nbtData;

        private Snapshot(String material, String blockState, String nbtData) {
            this.material = material;
            this.blockState = blockState;
            this.nbtData = nbtData;
        }

        static Snapshot of(String material, String blockState, String nbtData) {
            return new Snapshot(material, blockState, nbtData);
        }
    }

    private LegacyBlockSnapshotter() {
    }

    public static CompletableFuture<Map<LegacyLocationKey, Snapshot>> snapshotBlocks(Plugin plugin,
                                                                                    List<LegacyLocationKey> locations,
                                                                                    int perTick) {
        CompletableFuture<Map<LegacyLocationKey, Snapshot>> future = new CompletableFuture<>();
        Map<LegacyLocationKey, Snapshot> out = new HashMap<>();

        if (locations == null || locations.isEmpty()) {
            future.complete(out);
            return future;
        }

        int batch = Math.max(1, perTick);

        new BukkitRunnable() {
            int idx = 0;

            @Override
            public void run() {
                try {
                    int end = Math.min(idx + batch, locations.size());
                    for (; idx < end; idx++) {
                        LegacyLocationKey key = locations.get(idx);
                        World world = Bukkit.getWorld(key.world());
                        if (world == null) {
                            continue;
                        }

                        Location loc = new Location(world, key.x(), key.y(), key.z());
                        Block block = loc.getBlock();
                        if (block == null || MaterialUtils.isAir(block.getType())) {
                            continue;
                        }

                        String nbtData;
                        try {
                            nbtData = NBT.get(block.getState(), ReadableNBT::toString);
                        } catch (Exception ignored) {
                            nbtData = null;
                        }

                        String material;
                        String blockState;
                        if (ItemsAdderAdapter.isItemsAdderBlock(block)) {
                            material = "ITEMS_ADDER";
                            blockState = ItemsAdderAdapter.getCustomBlockId(block);
                            if (blockState == null || blockState.isEmpty()) {
                                // Invalid snapshot, skip.
                                continue;
                            }
                        } else {
                            material = block.getType().name();
                            blockState = BlockUtils.getBlockStateString(block);
                        }

                        out.put(key, new Snapshot(material, blockState, nbtData));
                    }

                    if (idx >= locations.size()) {
                        cancel();
                        future.complete(out);
                    }
                } catch (Throwable t) {
                    cancel();
                    plugin.getLogger().log(Level.SEVERE, "Legacy block snapshot failed", t);
                    future.completeExceptionally(t);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        return future;
    }
}
