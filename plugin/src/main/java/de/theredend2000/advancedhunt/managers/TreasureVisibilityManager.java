package de.theredend2000.advancedhunt.managers;

import com.cryptomorin.xseries.XMaterial;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.model.Treasure;
import de.theredend2000.advancedhunt.model.TreasureCore;
import de.theredend2000.advancedhunt.platform.PlatformAccess;
import de.theredend2000.advancedhunt.util.HeadHelper;
import de.theredend2000.advancedhunt.util.ItemsAdderAdapter;
import de.theredend2000.advancedhunt.util.MaterialUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TreasureVisibilityManager implements Listener {

    private static final Material DEFAULT_FURNITURE_BLOCK = Optional.ofNullable(XMaterial.GRAY_STAINED_GLASS.get()).orElse(Material.STONE);
    private static final String FURNITURE_MARKER_NAME = "Furniture";

    private final Main plugin;
    private final TreasureManager treasureManager;
    private final CollectionManager collectionManager;

    private final Set<UUID> bypassPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Boolean> availabilityCache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Long, Integer>> furnitureMarkers = new ConcurrentHashMap<>();
    private final Cache<UUID, String> headTextureCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build();
    private final Set<UUID> headTextureLoading = ConcurrentHashMap.newKeySet();
    private final Cache<String, BlockData> blockDataCache = Caffeine.newBuilder()
        .maximumSize(4_000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build();
    private final Cache<Material, BlockData> materialDataCache = Caffeine.newBuilder()
        .maximumSize(256)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build();
    private final Cache<WrappedStateKey, WrappedBlockState> wrappedStateCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build();
    private final Map<String, Material> materialNameCache = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> worldEntityIdCounters = new ConcurrentHashMap<>();

    private BukkitTask availabilityTask;
    // Stored as Object so that TreasureVisibilityManager can be loaded without PacketEvents
    // on the classpath. The actual value is always a PacketListenerAbstract when non-null.
    private Object packetListener;

    public TreasureVisibilityManager(Main plugin, TreasureManager treasureManager, CollectionManager collectionManager) {
        this.plugin = plugin;
        this.treasureManager = treasureManager;
        this.collectionManager = collectionManager;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        registerPacketListener();

        availabilityTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            this::refreshAvailabilityStates,
            20L * 10,
            20L * 60
        );

        Bukkit.getScheduler().runTaskLater(plugin, this::refreshAvailabilityStates, 40L);
    }

    public void stop() {
        HandlerList.unregisterAll(this);
        if (availabilityTask != null) {
            availabilityTask.cancel();
            availabilityTask = null;
        }
        unregisterPacketListener();
        availabilityCache.clear();
        bypassPlayers.clear();
        headTextureCache.invalidateAll();
        headTextureLoading.clear();
        blockDataCache.invalidateAll();
        materialDataCache.invalidateAll();
        wrappedStateCache.invalidateAll();
        materialNameCache.clear();
        worldEntityIdCounters.clear();
        clearAllFurnitureMarkers();
    }

    public boolean isBypassEnabled(Player player) {
        if (player == null) return false;
        if (!player.hasPermission("advancedhunt.treasure.bypass")) return false;
        return bypassPlayers.contains(player.getUniqueId());
    }

    public void setBypass(Player player, boolean enabled) {
        if (player == null) return;
        if (enabled) {
            bypassPlayers.add(player.getUniqueId());
            sendVirtualTreasuresInView(player, true);
            return;
        }

        bypassPlayers.remove(player.getUniqueId());
        clearFurnitureMarkers(player.getUniqueId());
    }

    private void sendVirtualTreasuresInView(Player player, boolean show) {
        if (player == null || player.getWorld() == null) return;
        if (!isPacketEventsReady()) return;

        int viewDistance = getServerViewDistance();
        int cx = player.getLocation().getBlockX() >> 4;
        int cz = player.getLocation().getBlockZ() >> 4;
        World world = player.getWorld();

        for (int x = cx - viewDistance; x <= cx + viewDistance; x++) {
            for (int z = cz - viewDistance; z <= cz + viewDistance; z++) {
                List<TreasureCore> cores = treasureManager.getTreasureCoresInChunk(x, z);
                if (cores.isEmpty()) continue;

                for (TreasureCore core : cores) {
                    if (core == null || !isCollectionHidden(core.getCollectionId())) continue;
                    Location loc = core.getLocation();
                    if (loc == null || loc.getWorld() == null) continue;
                    if (!loc.getWorld().equals(world)) continue;

                    if (!show) {
                        Block block = world.getBlockAt(loc);
                        WrappedBlockState state = null;
                        try {
                            if (block != null && block.getBlockData() != null) {
                                state = SpigotConversionUtil.fromBukkitBlockData(block.getBlockData());
                            }
                        } catch (Throwable ignored) {
                        }
                        if (state != null) {
                            sendBlockChangeToPlayer(player, loc, state);
                        }
                        continue;
                    }

                    WrappedBlockState state = resolveWrappedBlockState(core, player);
                    if (state == null) continue;

                    sendBlockChangeToPlayer(player, loc, state);
                    scheduleVirtualExtras(player, core, loc);
                }
            }
        }
    }


    private void sendBlockChangeToPlayer(Player player, Location loc, WrappedBlockState state) {
        if (player == null || loc == null || state == null) return;
        try {
            WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(
                new Vector3i(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()),
                state
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Refreshes the availability states of all collections and updates treasure visibility accordingly.
     * This method is called periodically by the scheduler to check ACT rule changes.
     * For manual collection setting changes, use {@link #refreshCollectionVisibility(UUID)} instead.
     */
    public void refreshAvailabilityStates() {
        List<Collection> collections = collectionManager.getAllCollections();
        for (Collection collection : collections) {
            boolean actAvailable = collectionManager.isCollectionAvailable(collection);
            Boolean previous = availabilityCache.put(collection.getId(), actAvailable);

            // Determine if collection should be hidden (considering both ACT and hideWhenNotAvailable)
            boolean shouldBeHidden = shouldHideCollection(collection, actAvailable);
            boolean wasHidden = previous != null && !previous;

            if (previous == null) {
                if (shouldBeHidden) {
                    hideCollectionTreasures(collection.getId());
                } else {
                    restoreCollectionTreasures(collection.getId());
                }
                continue;
            }

            // Only act on ACT availability changes, not hideWhenNotAvailable changes
            // (hideWhenNotAvailable changes are handled by refreshCollectionVisibility)
            if (previous && !actAvailable) {
                hideCollectionTreasures(collection.getId());
            } else if (!previous && actAvailable) {
                // Only restore if hideWhenNotAvailable doesn't require hiding
                if (!shouldBeHidden) {
                    restoreCollectionTreasures(collection.getId());
                }
            }
        }
    }

    /**
     * Refreshes visibility for a specific collection when its settings change.
     * This should be called when collection.enabled or collection.hideWhenNotAvailable changes.
     * 
     * @param collectionId the collection whose visibility should be refreshed
     */
    public void refreshCollectionVisibility(UUID collectionId) {
        Optional<Collection> collectionOpt = collectionManager.getCollectionById(collectionId);
        if (!collectionOpt.isPresent()) {
            return;
        }

        refreshCollectionVisibility(collectionOpt.get());
    }

    /**
     * Refreshes visibility for a specific collection using the provided collection object.
     * This ensures we use the most up-to-date collection state.
     * 
     * @param collection the collection whose visibility should be refreshed
     */
    public void refreshCollectionVisibility(Collection collection) {
        if (collection == null) {
            return;
        }

        boolean actAvailable = collectionManager.isCollectionAvailable(collection);
        availabilityCache.put(collection.getId(), actAvailable);

        boolean shouldBeHidden = shouldHideCollection(collection, actAvailable);

        if (shouldBeHidden) {
            hideCollectionTreasures(collection.getId());
        } else {
            restoreCollectionTreasures(collection.getId());
        }
    }

    /**
     * Determines if a collection should be hidden based on both hideWhenNotAvailable flag and ACT availability.
     * 
     * @param collection the collection to check
     * @param actAvailable whether ACT rules say the collection is available
     * @return true if collection should be hidden
     */
    private boolean shouldHideCollection(Collection collection, boolean actAvailable) {
        // Only hide if the hideWhenNotAvailable feature is enabled
        if (!collection.isHideWhenNotAvailable()) {
            return false; // Feature disabled - don't hide treasures
        }
        
        // Hide if collection is disabled OR ACT says not available
        return !collection.isEnabled() || !actAvailable;
    }

    private boolean isCollectionHidden(UUID collectionId) {
        Optional<Collection> collectionOpt = collectionManager.getCollectionById(collectionId);
        if (!collectionOpt.isPresent()) {
            return false;
        }
        
        Collection collection = collectionOpt.get();
        
        // Get ACT availability (from cache or calculate)
        Boolean actAvailable = availabilityCache.get(collectionId);
        if (actAvailable == null) {
            actAvailable = collectionManager.isCollectionAvailable(collection);
        }
        
        return shouldHideCollection(collection, actAvailable);
    }

    private void hideCollectionTreasures(UUID collectionId) {
        List<TreasureCore> cores = new ArrayList<>(treasureManager.getTreasureCoresInCollection(collectionId));
        if (cores.isEmpty()) return;

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int processed = 0;
                while (index < cores.size() && processed < 400) {
                    TreasureCore core = cores.get(index++);
                    processed++;
                    hideTreasureBlock(core);
                }

                if (index >= cores.size()) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void restoreCollectionTreasures(UUID collectionId) {
        List<TreasureCore> cores = new ArrayList<>(treasureManager.getTreasureCoresInCollection(collectionId));
        if (cores.isEmpty()) return;

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int processed = 0;
                while (index < cores.size() && processed < 200) {
                    TreasureCore core = cores.get(index++);
                    processed++;
                    restoreTreasureBlock(core);
                }

                if (index >= cores.size()) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void hideTreasureBlock(TreasureCore core) {
        if (core == null || core.getLocation() == null) return;
        Location loc = core.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        if (!world.isChunkLoaded(cx, cz)) return;

        if (isItemsAdder(core)) {
            if (isItemsAdderFurniture(core)) {
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

    private void restoreTreasureBlock(TreasureCore core) {
        if (core == null || core.getLocation() == null) return;
        Location loc = core.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        if (!world.isChunkLoaded(cx, cz)) return;

        if (isItemsAdder(core)) {
            if (isItemsAdderFurniture(core)) {
                ItemsAdderAdapter.spawnCustomFurniture(core.getBlockState(), loc);
            } else {
                ItemsAdderAdapter.placeCustomBlock(core.getBlockState(), loc);
            }
            return;
        }

        Block block = world.getBlockAt(loc);
        if (!MaterialUtils.isAir(block.getType())) {
            return;
        }

        Material type = resolveMaterial(core.getMaterial());
        if (type == null) return;

        BlockData data = resolveBlockData(core, type);
        try {
            if (data != null) {
                block.setBlockData(data, false);
            } else {
                block.setType(type, false);
            }
        } catch (Throwable ignored) {
            block.setType(type, false);
        }

        treasureManager.getFullTreasureAsync(core.getId()).thenAccept(treasure -> {
            if (treasure == null) return;
            applyTreasureNbt(loc, treasure);
        });
    }

    private void applyTreasureNbt(Location location, Treasure treasure) {
        if (location == null || treasure == null) return;
        String nbtData = treasure.getNbtData();
        if (nbtData == null || nbtData.isEmpty()) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Block block = location.getBlock();
                if (block == null) return;
                BlockState state = block.getState();
                if (HeadHelper.applySkullProfile(state, nbtData)) {
                    state.update(true, false);
                    return;
                }
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
        });
    }

    private void registerPacketListener() {
        if (!isPacketEventsReady()) {
            plugin.getLogger().info("[TreasureVisibility] PacketEvents not available "
                + "— treasure-visibility bypass and virtual block injection disabled.");
            return;
        }

        try {
            PacketListenerAbstract listener = new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
                @Override
                public void onPacketSend(PacketSendEvent event) {
                    handlePacketSend(event);
                }
            };
            PacketEvents.getAPI().getEventManager().registerListener(listener);
            packetListener = listener;
        } catch (Throwable ignored) {
        }
    }

    private void unregisterPacketListener() {
        if (packetListener == null) return;
        try {
            // Cast is safe: packetListener is only ever set to a PacketListenerAbstract
            // instance inside registerPacketListener(), which only runs when PacketEvents
            // is confirmed available. The cast is in a method body and thus resolved lazily.
            PacketEvents.getAPI().getEventManager().unregisterListener(
                (PacketListenerAbstract) packetListener);
        } catch (Throwable ignored) {
        }
        packetListener = null;
    }

    private void handlePacketSend(PacketSendEvent event) {
        if (event == null) return;
        Object playerObj = event.getPlayer();
        if (!(playerObj instanceof Player)) return;
        Player player = (Player) playerObj;
        if (!isBypassEnabled(player)) return;

        PacketTypeCommon packetType = event.getPacketType();
        if (packetType == PacketType.Play.Server.BLOCK_CHANGE) {
            handleBlockChange(event, player);
            return;
        }
        if (packetType == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            handleMultiBlockChange(event, player);
            return;
        }
        if (packetType == PacketType.Play.Server.CHUNK_DATA) {
            handleChunkData(event, player);
        }
    }

    private void handleBlockChange(PacketSendEvent event, Player player) {
        WrapperPlayServerBlockChange wrapper = new WrapperPlayServerBlockChange(event);
        Vector3i pos = wrapper.getBlockPosition();
        if (pos == null) return;

        Location loc = new Location(player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
        TreasureCore core = treasureManager.getTreasureCoreAt(loc);
        if (core == null || !isCollectionHidden(core.getCollectionId())) return;

        WrappedBlockState blockState = resolveWrappedBlockState(core, player);
        if (blockState == null) return;

        wrapper.setBlockState(blockState);
        wrapper.write();
        event.markForReEncode(true);

        scheduleVirtualExtras(player, core, loc);
    }

    private void handleMultiBlockChange(PacketSendEvent event, Player player) {
        WrapperPlayServerMultiBlockChange wrapper = new WrapperPlayServerMultiBlockChange(event);

        Vector3i chunkPos = wrapper.getChunkPosition();
        if (chunkPos == null) return;

        WrapperPlayServerMultiBlockChange.EncodedBlock[] blocks = wrapper.getBlocks();
        if (blocks == null || blocks.length == 0) return;

        boolean changed = false;
        int baseX = chunkPos.getX() << 4;
        int baseZ = chunkPos.getZ() << 4;

        for (WrapperPlayServerMultiBlockChange.EncodedBlock block : blocks) {
            int x = baseX + block.getX();
            int y = block.getY();
            int z = baseZ + block.getZ();
            Location loc = new Location(player.getWorld(), x, y, z);

            TreasureCore core = treasureManager.getTreasureCoreAt(loc);
            if (core == null || !isCollectionHidden(core.getCollectionId())) {
                continue;
            }

            WrappedBlockState state = resolveWrappedBlockState(core, player);
            if (state == null) continue;

            block.setBlockState(state);
            changed = true;
            scheduleVirtualExtras(player, core, loc);
        }

        if (changed) {
            wrapper.setBlocks(blocks);
            wrapper.write();
            event.markForReEncode(true);
        }
    }

    private void handleChunkData(PacketSendEvent event, Player player) {
        WrapperPlayServerChunkData wrapper = new WrapperPlayServerChunkData(event);

        Column column = wrapper.getColumn();
        if (column == null) return;

        int chunkX = column.getX();
        int chunkZ = column.getZ();
        List<TreasureCore> cores = treasureManager.getTreasureCoresInChunk(chunkX, chunkZ);
        if (cores.isEmpty()) return;

        BaseChunk[] sections = column.getChunks();
        if (sections == null || sections.length == 0) return;

        int minY = player.getWorld().getMinHeight();
        boolean changed = false;

        for (TreasureCore core : cores) {
            if (core == null || !isCollectionHidden(core.getCollectionId())) continue;
            Location loc = core.getLocation();
            if (loc == null || loc.getWorld() == null) continue;
            if (!loc.getWorld().equals(player.getWorld())) continue;

            int y = loc.getBlockY();
            int sectionIndex = (y - minY) >> 4;
            if (sectionIndex < 0 || sectionIndex >= sections.length) continue;

            BaseChunk section = sections[sectionIndex];
            if (section == null) continue;

            WrappedBlockState state = resolveWrappedBlockState(core, player);
            if (state == null) continue;

            int localX = loc.getBlockX() & 0xF;
            int localY = (y - minY) & 0xF;
            int localZ = loc.getBlockZ() & 0xF;

            section.set(localX, localY, localZ, state);
            changed = true;

            scheduleVirtualExtras(player, core, loc);
        }

        if (changed) {
            wrapper.setColumn(column);
            wrapper.write();
            event.markForReEncode(true);
        }
    }

    private void scheduleVirtualExtras(Player player, TreasureCore core, Location loc) {
        if (HeadHelper.isHeadMaterialName(core.getMaterial())) {
            String texture = headTextureCache.getIfPresent(core.getId());
            if (texture != null && !texture.isEmpty()) {
                sendHeadBlockEntityData(player, loc, texture);
            } else {
                loadHeadTextureAsync(player, core, loc);
            }
        }

        if (isItemsAdder(core) && isItemsAdderFurniture(core)) {
            ensureFurnitureMarker(player, loc);
        }
    }

    private void loadHeadTextureAsync(Player player, TreasureCore core, Location loc) {
        if (!headTextureLoading.add(core.getId())) return;

        treasureManager.getFullTreasureAsync(core.getId()).thenAccept(treasure -> {
            if (treasure == null) {
                headTextureLoading.remove(core.getId());
                return;
            }

            String texture = HeadHelper.getTextureFromNbt(treasure.getNbtData());
            if (texture != null && !texture.isEmpty()) {
                headTextureCache.put(core.getId(), texture);
                Bukkit.getScheduler().runTask(plugin, () -> sendHeadBlockEntityData(player, loc, texture));
            }
            headTextureLoading.remove(core.getId());
        });
    }

    private void sendHeadBlockEntityData(Player player, Location loc, String texture) {
        if (player == null || loc == null || texture == null || texture.isEmpty()) return;
        PlatformAccess.get().sendSkullUpdatePacket(player, loc, texture);
    }

    private void ensureFurnitureMarker(Player player, Location loc) {
        if (player == null || loc == null) return;

        Map<Long, Integer> playerMarkers = furnitureMarkers.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        long key = locationKey(loc);
        if (playerMarkers.containsKey(key)) return;

        int entityId = allocateEntityId(loc.getWorld());
        if (entityId == -1) return;

        boolean spawned = PlatformAccess.get().spawnHologramArmorStandForPlayer(player, entityId, UUID.randomUUID(), loc.clone().add(0.5, 1.2, 0.5), FURNITURE_MARKER_NAME);
        if (!spawned) return;

        playerMarkers.put(key, entityId);
    }

    private void clearFurnitureMarkers(UUID playerId) {
        Map<Long, Integer> markers = furnitureMarkers.remove(playerId);
        if (markers == null || markers.isEmpty()) return;

        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;

        int[] ids = markers.values().stream().mapToInt(Integer::intValue).toArray();
        PlatformAccess.get().destroyEntitiesForPlayer(player, ids);
    }

    private void clearAllFurnitureMarkers() {
        for (UUID playerId : new ArrayList<>(furnitureMarkers.keySet())) {
            clearFurnitureMarkers(playerId);
        }
    }

    private boolean isItemsAdder(TreasureCore core) {
        return core != null && "ITEMS_ADDER".equalsIgnoreCase(core.getMaterial());
    }

    private boolean isItemsAdderFurniture(TreasureCore core) {
        if (core == null) return false;
        String blockState = core.getBlockState();
        if (blockState == null || blockState.isEmpty()) return false;
        return ItemsAdderAdapter.isCustomFurnitureId(blockState);
    }

    private WrappedBlockState resolveWrappedBlockState(TreasureCore core, Player player) {
        if (core == null) return null;

        ClientVersion clientVersion = null;
        try {
            if (player != null) {
                clientVersion = PacketEvents.getAPI().getPlayerManager().getClientVersion(player);
            }
        } catch (Throwable ignored) {
        }

        String blockState = core.getBlockState();
        String cacheKey = null;
        if (blockState != null && !blockState.isEmpty() && !isLegacyBlockData(blockState)) {
            cacheKey = blockState;
        } else if (core.getMaterial() != null && !core.getMaterial().isEmpty()) {
            cacheKey = "material:" + core.getMaterial();
        }

        if (cacheKey != null) {
            WrappedBlockState cached = wrappedStateCache.getIfPresent(new WrappedStateKey(cacheKey, clientVersion));
            if (cached != null) return cached;
        }

        BlockData data = resolveBlockData(core, resolveMaterial(core.getMaterial()));
        if (data != null) {
            try {
                WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData(data);
                if (state != null) {
                    if (cacheKey != null) {
                        wrappedStateCache.put(new WrappedStateKey(cacheKey, clientVersion), state);
                    }
                    return state;
                }
            } catch (Throwable ignored) {
            }
        }

        if (blockState != null && !blockState.isEmpty() && !isLegacyBlockData(blockState)) {
            try {
                WrappedBlockState state;
                if (clientVersion != null) {
                    state = WrappedBlockState.getByString(clientVersion, blockState);
                } else {
                    state = WrappedBlockState.getByString(blockState);
                }
                if (state != null && cacheKey != null) {
                    wrappedStateCache.put(new WrappedStateKey(cacheKey, clientVersion), state);
                }
                return state;
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private BlockData resolveBlockData(TreasureCore core, Material fallbackMaterial) {
        if (core == null) return null;

        if (isItemsAdder(core)) {
            if (isItemsAdderFurniture(core)) {
                return materialDataCache.get(DEFAULT_FURNITURE_BLOCK, Material::createBlockData);
            }
            String blockState = core.getBlockState();
            if (blockState != null && !blockState.isEmpty()) {
                BlockData cached = blockDataCache.getIfPresent(blockState);
                if (cached != null) return cached;
            }
            BlockData data = ItemsAdderAdapter.getCustomBlockData(blockState);
            if (data != null) {
                if (blockState != null && !blockState.isEmpty()) {
                    blockDataCache.put(blockState, data);
                }
                return data;
            }
            return materialDataCache.get(DEFAULT_FURNITURE_BLOCK, Material::createBlockData);
        }

        String blockState = core.getBlockState();
        if (blockState != null && !blockState.isEmpty() && !isLegacyBlockData(blockState)) {
            BlockData cached = blockDataCache.getIfPresent(blockState);
            if (cached != null) return cached;
            try {
                BlockData data = Bukkit.createBlockData(blockState);
                if (data != null) {
                    blockDataCache.put(blockState, data);
                }
                return data;
            } catch (Throwable ignored) {
            }
        }

        if (fallbackMaterial != null) {
            try {
                return materialDataCache.get(fallbackMaterial, Material::createBlockData);
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private Material resolveMaterial(String materialName) {
        if (materialName == null || materialName.isEmpty()) return null;
        String key = materialName.toUpperCase(Locale.ROOT);
        Material cached = materialNameCache.get(key);
        if (cached != null) return cached;
        try {
            Material material = Material.getMaterial(key);
            if (material != null) {
                materialNameCache.put(key, material);
                return material;
            }
        } catch (Throwable ignored) {
        }
        return DEFAULT_FURNITURE_BLOCK;
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

    @SuppressWarnings("deprecation")
    private void refreshChunksAroundPlayer(Player player) {
        if (player == null || player.getWorld() == null) return;
        int viewDistance = getServerViewDistance();
        int cx = player.getLocation().getBlockX() >> 4;
        int cz = player.getLocation().getBlockZ() >> 4;

        for (int x = cx - viewDistance; x <= cx + viewDistance; x++) {
            for (int z = cz - viewDistance; z <= cz + viewDistance; z++) {
                player.getWorld().refreshChunk(x, z);
            }
        }
    }

    private int getServerViewDistance() {
        try {
            int dist = Bukkit.getViewDistance();
            return dist > 0 ? dist : 8;
        } catch (Throwable ignored) {
            return 8;
        }
    }

    private int allocateEntityId(World world) {
        if (world == null) return -1;
        final int min = 1_000_000_000;
        final int max = Integer.MAX_VALUE;
        AtomicInteger counter = worldEntityIdCounters.computeIfAbsent(world.getUID(),
            k -> new AtomicInteger(min + plugin.getRandom().nextInt(1_000_000)));

        int id = counter.updateAndGet(current -> current >= max ? min : current + 1);
        return id > 0 ? id : -1;
    }

    private static final class WrappedStateKey {
        private final String key;
        private final ClientVersion clientVersion;

        private WrappedStateKey(String key, ClientVersion clientVersion) {
            this.key = key;
            this.clientVersion = clientVersion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WrappedStateKey)) return false;
            WrappedStateKey that = (WrappedStateKey) o;
            return Objects.equals(key, that.key) && clientVersion == that.clientVersion;
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, clientVersion);
        }
    }

    private boolean isPacketEventsReady() {
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("packetevents")
                && !Bukkit.getPluginManager().isPluginEnabled("PacketEvents")) {
                return false;
            }
        } catch (Throwable ignored) {
            return false;
        }

        try {
            return PacketEvents.getAPI().isInitialized();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private long locationKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return 0L;
        long posKey = packBlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        UUID worldId = loc.getWorld().getUID();
        long worldHash = worldId.getMostSignificantBits() ^ worldId.getLeastSignificantBits();
        return posKey ^ worldHash;
    }

    private long packBlockPos(int x, int y, int z) {
        long lx = (long) (x & 0x3FFFFFF);
        long ly = (long) (y & 0xFFF);
        long lz = (long) (z & 0x3FFFFFF);
        return (lx << 38) | (lz << 12) | ly;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event == null || event.getChunk() == null) return;

        int cx = event.getChunk().getX();
        int cz = event.getChunk().getZ();
        List<TreasureCore> cores = treasureManager.getTreasureCoresInChunk(cx, cz);
        if (cores.isEmpty()) return;

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int processed = 0;
                while (index < cores.size() && processed < 200) {
                    TreasureCore core = cores.get(index++);
                    processed++;
                    if (core == null) continue;

                    if (isCollectionHidden(core.getCollectionId())) {
                        hideTreasureBlock(core);
                    } else {
                        restoreTreasureBlock(core);
                    }
                }

                if (index >= cores.size()) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event == null || event.getPlayer() == null) return;
        UUID playerId = event.getPlayer().getUniqueId();
        bypassPlayers.remove(playerId);
        clearFurnitureMarkers(playerId);
    }
}
