package de.theredend2000.advancedhunt.data;

import de.theredend2000.advancedhunt.model.*;
import de.theredend2000.advancedhunt.model.Collection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class YamlRepository implements DataRepository {

    private static final String YML_EXTENSION = ".yml";
    
    private final JavaPlugin plugin;
    private final File collectionsFolder;
    private final File playerDataFolder;
    private File leaderboardFile;
    private File finderIndexFile;
    private File systemFile;
    private File legacyRewardPresetsFile;
    private File rewardPresetsFolder;
    private File treasureRewardPresetsFolder;
    private File collectionRewardPresetsFolder;
    private BukkitTask flushTask;
    
    // In-memory indexes for efficient lookups (rebuilt on init, updated on save)
    // Treasure ID -> Collection ID (lightweight, only UUIDs)
    private final Map<UUID, UUID> treasureToCollectionIndex = new ConcurrentHashMap<>();
    // Treasure ID -> Set of Player UUIDs who found it (reverse index for getPlayersWhoFound)
    private final Map<UUID, Set<UUID>> treasureToFindersIndex = new ConcurrentHashMap<>();
    // Flag to track if finder index needs saving
    private volatile boolean finderIndexDirty = false;

    private final Map<Integer, Runnable> schemaMigrations = new HashMap<>();
    private int cachedSchemaVersion = -1;

    public YamlRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.collectionsFolder = new File(plugin.getDataFolder(), "collections");
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        
        registerMigrations();
    }

    private void registerMigrations() {
        schemaMigrations.put(1, () -> {
            // Initial schema version
            // No specific actions needed for v1 as it's the baseline
        });

        schemaMigrations.put(2, () -> {
            // Migrate reward presets from legacy reward-presets.yml into per-preset files
            try {
                migrateLegacyRewardPresetsIfNeeded();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to migrate legacy reward presets", e);
                throw e;
            }
        });
    }

    @Override
    public void init() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!collectionsFolder.exists()) {
            collectionsFolder.mkdirs();
        }
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
        leaderboardFile = new File(plugin.getDataFolder(), "leaderboard.yml");
        if (!leaderboardFile.exists()) {
            try {
                leaderboardFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        finderIndexFile = new File(plugin.getDataFolder(), "finder-index.yml");
        systemFile = new File(plugin.getDataFolder(), "system.yml");

        // Reward presets are stored as individual files (v2+)
        rewardPresetsFolder = new File(plugin.getDataFolder(), "reward-presets");
        treasureRewardPresetsFolder = new File(rewardPresetsFolder, "treasure");
        collectionRewardPresetsFolder = new File(rewardPresetsFolder, "collection");
        if (!treasureRewardPresetsFolder.exists()) {
            treasureRewardPresetsFolder.mkdirs();
        }
        if (!collectionRewardPresetsFolder.exists()) {
            collectionRewardPresetsFolder.mkdirs();
        }

        // Keep legacy file reference for migration/back-compat reads
        legacyRewardPresetsFile = new File(plugin.getDataFolder(), "reward-presets.yml");
        
        // Build in-memory indexes
        buildTreasureToCollectionIndex();
        loadFinderIndex();
        upgradeSchema();
    }

    private File getPresetFolder(RewardPresetType type) {
        return type == RewardPresetType.COLLECTION ? collectionRewardPresetsFolder : treasureRewardPresetsFolder;
    }

    private File getPresetFile(RewardPresetType type, UUID presetId) {
        return new File(getPresetFolder(type), presetId.toString() + YML_EXTENSION);
    }

    private void saveConfigAtomic(FileConfiguration config, File file) {
        // Bukkit's YamlConfiguration writes directly to the file.
        // We reduce corruption risk by writing to a temp file and then replacing.
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            config.save(tmp);
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception atomicMoveNotSupported) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + file.getName(), e);
        } finally {
            if (tmp.exists()) {
                // Best-effort cleanup
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
        }
    }

    private boolean isPresetFolderEmpty(File folder) {
        File[] files = folder.listFiles((dir, name) -> name.endsWith(YML_EXTENSION));
        return files == null || files.length == 0;
    }

    private void migrateLegacyRewardPresetsIfNeeded() {
        // Only migrate if legacy file exists and new folders are currently empty.
        if (legacyRewardPresetsFile == null || !legacyRewardPresetsFile.exists()) {
            return;
        }
        if (rewardPresetsFolder == null) {
            rewardPresetsFolder = new File(plugin.getDataFolder(), "reward-presets");
        }
        if (treasureRewardPresetsFolder == null) {
            treasureRewardPresetsFolder = new File(rewardPresetsFolder, "treasure");
        }
        if (collectionRewardPresetsFolder == null) {
            collectionRewardPresetsFolder = new File(rewardPresetsFolder, "collection");
        }
        treasureRewardPresetsFolder.mkdirs();
        collectionRewardPresetsFolder.mkdirs();

        if (!isPresetFolderEmpty(treasureRewardPresetsFolder) || !isPresetFolderEmpty(collectionRewardPresetsFolder)) {
            // Already migrated or user already has new-format presets.
            return;
        }

        FileConfiguration legacy = YamlConfiguration.loadConfiguration(legacyRewardPresetsFile);
        int migrated = 0;

        migrated += migrateLegacySection(legacy, "treasure", RewardPresetType.TREASURE);
        migrated += migrateLegacySection(legacy, "collection", RewardPresetType.COLLECTION);

        if (migrated > 0) {
            // Keep legacy file as a backup to be safe; rename it to avoid future confusion.
            File backup = new File(plugin.getDataFolder(), "reward-presets.legacy.yml");
            if (!backup.exists()) {
                try {
                    Files.copy(legacyRewardPresetsFile.toPath(), backup.toPath());
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to create reward-presets.legacy.yml backup", e);
                }
            }
            plugin.getLogger().info("Migrated " + migrated + " reward preset(s) to per-file YAML storage");
        }
    }

    private int migrateLegacySection(FileConfiguration legacy, String rootKey, RewardPresetType type) {
        var section = legacy.getConfigurationSection(rootKey);
        if (section == null) {
            return 0;
        }

        int migrated = 0;
        for (String idStr : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idStr);
                String name = section.getString(idStr + ".name");
                List<Map<?, ?>> rewardList = section.getMapList(idStr + ".rewards");
                List<Reward> rewards = deserializeRewards(rewardList);
                if (name == null || name.isBlank()) {
                    continue;
                }

                FileConfiguration out = new YamlConfiguration();
                out.set("name", name);
                out.set("rewards", serializeRewards(rewards));
                saveConfigAtomic(out, getPresetFile(type, id));
                migrated++;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to migrate legacy preset " + rootKey + "/" + idStr + ": " + e.getMessage());
            }
        }
        return migrated;
    }

    @Override
    public void reload() {
        init();
    }

    @Override
    public int getSchemaVersion() {
        if (cachedSchemaVersion != -1) return cachedSchemaVersion;
        if (systemFile == null || !systemFile.exists()) return 0;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(systemFile);
        cachedSchemaVersion = config.getInt("schema_version", 0);
        return cachedSchemaVersion;
    }

    @Override
    public void upgradeSchema() {
        if (systemFile == null) systemFile = new File(plugin.getDataFolder(), "system.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(systemFile);
        int currentVersion = config.getInt("schema_version", 0);
        
        int latestVersion = schemaMigrations.keySet().stream().mapToInt(v -> v).max().orElse(0);
        
        for (int i = currentVersion + 1; i <= latestVersion; i++) {
            Runnable migration = schemaMigrations.get(i);
            if (migration == null) {
                plugin.getLogger().severe("Missing YAML schema migration for version " + i + "; aborting upgrade");
                break;
            }

            try {
                migration.run();
                plugin.getLogger().info("Upgraded YAML data schema to version " + i);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to apply YAML schema migration for version " + i);
                e.printStackTrace();
                break;
            }

            config.set("schema_version", i);
            try {
                config.save(systemFile);
                cachedSchemaVersion = i;
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to persist YAML schema version " + i + " to system.yml; aborting upgrade");
                e.printStackTrace();
                break;
            }
        }
    }

    @Override
    public CompletableFuture<List<UUID>> getAllPlayerUUIDs() {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> uuids = new ArrayList<>();
            File[] files = playerDataFolder.listFiles((dir, name) -> name.endsWith(YML_EXTENSION));
            if (files != null) {
                for (File file : files) {
                    try {
                        uuids.add(UUID.fromString(file.getName().replace(YML_EXTENSION, "")));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            return uuids;
        });
    }
    
    /**
     * Builds the lightweight treasure-to-collection index by scanning collection folders.
     * Only stores UUIDs, no heavy treasure data.
     */
    private void buildTreasureToCollectionIndex() {
        treasureToCollectionIndex.clear();
        File[] collectionDirs = collectionsFolder.listFiles(File::isDirectory);
        if (collectionDirs != null) {
            for (File collectionDir : collectionDirs) {
                try {
                    UUID collectionId = UUID.fromString(collectionDir.getName());
                    File treasuresDir = new File(collectionDir, "treasures");
                    if (treasuresDir.exists() && treasuresDir.isDirectory()) {
                        File[] files = treasuresDir.listFiles((dir, name) -> name.endsWith(YML_EXTENSION));
                        if (files != null) {
                            for (File file : files) {
                                try {
                                    UUID treasureId = UUID.fromString(file.getName().replace(YML_EXTENSION, ""));
                                    treasureToCollectionIndex.put(treasureId, collectionId);
                                } catch (IllegalArgumentException ignored) {}
                            }
                        }
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
        plugin.getLogger().info("Built treasure-to-collection index with " + treasureToCollectionIndex.size() + " entries");
    }
    
    /**
     * Loads the finder index from disk, or rebuilds it if not present/corrupted.
     */
    private void loadFinderIndex() {
        treasureToFindersIndex.clear();
        if (finderIndexFile != null && finderIndexFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(finderIndexFile);
            var treasuresSection = config.getConfigurationSection("treasures");
            if (treasuresSection != null) {
                for (String treasureIdStr : treasuresSection.getKeys(false)) {
                    try {
                        UUID treasureId = UUID.fromString(treasureIdStr);
                        List<String> finderStrs = config.getStringList("treasures." + treasureIdStr);
                        Set<UUID> finders = ConcurrentHashMap.newKeySet();
                        for (String finderStr : finderStrs) {
                            try {
                                finders.add(UUID.fromString(finderStr));
                            } catch (IllegalArgumentException ignored) {}
                        }
                        treasureToFindersIndex.put(treasureId, finders);
                    } catch (IllegalArgumentException ignored) {}
                }
                plugin.getLogger().info("Loaded finder index with " + treasureToFindersIndex.size() + " treasure entries");
                return;
            }
        }
        // Index doesn't exist or is invalid - rebuild from player data
        rebuildFinderIndex();
    }
    
    /**
     * Rebuilds the finder index by scanning all player data files.
     * Called once on startup if index file is missing/corrupt.
     */
    private void rebuildFinderIndex() {
        plugin.getLogger().info("Rebuilding finder index from player data...");
        treasureToFindersIndex.clear();
        
        if (playerDataFolder.exists()) {
            File[] files = playerDataFolder.listFiles((dir, name) -> name.endsWith(YML_EXTENSION));
            if (files != null) {
                for (File file : files) {
                    try {
                        UUID playerUuid = UUID.fromString(file.getName().replace(YML_EXTENSION, ""));
                        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                        List<String> found = config.getStringList("found-treasures");
                        for (String treasureIdStr : found) {
                            try {
                                UUID treasureId = UUID.fromString(treasureIdStr);
                                treasureToFindersIndex
                                    .computeIfAbsent(treasureId, k -> ConcurrentHashMap.newKeySet())
                                    .add(playerUuid);
                            } catch (IllegalArgumentException ignored) {}
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
        
        // Save the rebuilt index
        saveFinderIndex();
        plugin.getLogger().info("Rebuilt finder index with " + treasureToFindersIndex.size() + " treasure entries");
    }
    
    /**
     * Saves the finder index to disk.
     */
    private void saveFinderIndex() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Set<UUID>> entry : treasureToFindersIndex.entrySet()) {
            List<String> finderStrs = new ArrayList<>();
            for (UUID finder : entry.getValue()) {
                finderStrs.add(finder.toString());
            }
            config.set("treasures." + entry.getKey().toString(), finderStrs);
        }
        saveConfigAtomic(config, finderIndexFile);
        finderIndexDirty = false;
    }

    @Override
    public void shutdown() {
        if (flushTask != null && !flushTask.isCancelled()) {
            flushTask.cancel();
        }
        // Save finder index if dirty
        if (finderIndexDirty) {
            saveFinderIndex();
        }
    }

    private void saveConfig(FileConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + file.getName(), e);
        }
    }

    @Override
    public CompletableFuture<PlayerData> loadPlayerData(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(playerDataFolder, playerUuid.toString() + YML_EXTENSION);
            PlayerData data = new PlayerData(playerUuid);
            if (file.exists()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                List<String> found = config.getStringList("found-treasures");
                for (String s : found) {
                    try {
                        data.addFoundTreasure(UUID.fromString(s));
                    } catch (IllegalArgumentException ignored) {}
                }
                String selId = config.getString("selected-collection-id");
                if (selId != null) {
                    try {
                        data.setSelectedCollectionId(UUID.fromString(selId));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            return data;
        });
    }

    @Override
    public CompletableFuture<Void> savePlayerData(PlayerData playerData) {
        return CompletableFuture.runAsync(() -> {
            File file = new File(playerDataFolder, playerData.getPlayerUuid().toString() + YML_EXTENSION);
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            List<String> found = new ArrayList<>();
            for (UUID uuid : playerData.getFoundTreasures()) {
                found.add(uuid.toString());
            }
            config.set("found-treasures", found);
            config.set("selected-collection-id", playerData.getSelectedCollectionId() != null ? playerData.getSelectedCollectionId().toString() : null);
            saveConfig(config, file);
            
            // Update finder index (fast in-memory update)
            updateFinderIndex(playerData);
            
            // Note: Leaderboard is now updated by LeaderboardManager on a schedule,
            // not on every save, to avoid I/O storms on high-traffic servers
        });
    }

    @Override
    public CompletableFuture<Void> savePlayerDataBatch(List<PlayerData> playerDataList) {
        return CompletableFuture.runAsync(() -> {
            for (PlayerData pd : playerDataList) {
                File file = new File(playerDataFolder, pd.getPlayerUuid() + YML_EXTENSION);
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                
                List<String> found = new ArrayList<>();
                for (UUID uuid : pd.getFoundTreasures()) {
                    found.add(uuid.toString());
                }
                config.set("found-treasures", found);
                config.set("selected-collection-id", pd.getSelectedCollectionId() != null ? pd.getSelectedCollectionId().toString() : null);
                saveConfig(config, file);
                
                updateFinderIndex(pd);
            }
        });
    }
    
    /**
     * Updates the finder index with this player's found treasures.
     * Fast in-memory operation - index is persisted on shutdown or periodically.
     */
    private void updateFinderIndex(PlayerData playerData) {
        UUID playerUuid = playerData.getPlayerUuid();
        
        // Add player to all treasures they've found
        for (UUID treasureId : playerData.getFoundTreasures()) {
            treasureToFindersIndex
                .computeIfAbsent(treasureId, k -> ConcurrentHashMap.newKeySet())
                .add(playerUuid);
        }
        
        finderIndexDirty = true;
    }

    @Override
    public CompletableFuture<List<Treasure>> loadTreasures() {
        return CompletableFuture.supplyAsync(() -> {
            List<Treasure> treasures = new ArrayList<>();
            File[] collectionDirs = collectionsFolder.listFiles(File::isDirectory);
            if (collectionDirs != null) {
                for (File collectionDir : collectionDirs) {
                    File treasuresDir = new File(collectionDir, "treasures");
                    if (treasuresDir.exists() && treasuresDir.isDirectory()) {
                        File[] files = treasuresDir.listFiles((dir, name) -> name.endsWith(YML_EXTENSION));
                        if (files != null) {
                            for (File file : files) {
                                try {
                                    String fileName = file.getName().replace(YML_EXTENSION, "");
                                    UUID id = UUID.fromString(fileName);
                                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                                    
                                    String collectionIdStr = config.getString("collection-id");
                                    if (collectionIdStr == null) continue;
                                    
                                    UUID collectionId = UUID.fromString(collectionIdStr);
                                    String worldName = config.getString("world");
                                    int x = config.getInt("x");
                                    int y = config.getInt("y");
                                    int z = config.getInt("z");
                                    Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);
                                    
                                    List<Map<?, ?>> rewardList = config.getMapList("rewards");
                                    List<Reward> rewards = deserializeRewards(rewardList);
                                    String nbtData = config.getString("nbt-data");
                                    String material = config.getString("material");
                                    String blockState = config.getString("block-state");
                                    
                                    treasures.add(new Treasure(id, collectionId, loc, rewards, nbtData, material, blockState));
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Failed to load treasure " + file.getName() + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
            return treasures;
        });
    }

    @Override
    public CompletableFuture<List<TreasureCore>> loadTreasureCores() {
        return CompletableFuture.supplyAsync(() -> {
            List<TreasureCore> cores = new ArrayList<>();
            File[] collectionDirs = collectionsFolder.listFiles(File::isDirectory);
            if (collectionDirs != null) {
                for (File collectionDir : collectionDirs) {
                    File treasuresDir = new File(collectionDir, "treasures");
                    if (treasuresDir.exists() && treasuresDir.isDirectory()) {
                        File[] files = treasuresDir.listFiles((dir, name) -> name.endsWith(YML_EXTENSION));
                        if (files != null) {
                            for (File file : files) {
                                try {
                                    String fileName = file.getName().replace(YML_EXTENSION, "");
                                    UUID id = UUID.fromString(fileName);
                                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);

                                    String collectionIdStr = config.getString("collection-id");
                                    if (collectionIdStr == null) continue;

                                    UUID collectionId = UUID.fromString(collectionIdStr);
                                    String worldName = config.getString("world");
                                    int x = config.getInt("x");
                                    int y = config.getInt("y");
                                    int z = config.getInt("z");
                                    Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);

                                    String material = config.getString("material");
                                    cores.add(new TreasureCore(id, collectionId, loc, material));
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Failed to load treasure core " + file.getName() + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
            return cores;
        });
    }

    @Override
    public CompletableFuture<Void> saveTreasure(Treasure treasure) {
        return CompletableFuture.runAsync(() -> {
            File collectionDir = new File(collectionsFolder, treasure.getCollectionId().toString());
            if (!collectionDir.exists()) {
                collectionDir.mkdirs();
            }
            File treasuresDir = new File(collectionDir, "treasures");
            if (!treasuresDir.exists()) {
                treasuresDir.mkdirs();
            }
            
            File file = new File(treasuresDir, treasure.getId().toString() + YML_EXTENSION);
            // For new saves, avoid reading an existing file from disk.
            FileConfiguration config = new YamlConfiguration();
            
            config.set("collection-id", treasure.getCollectionId().toString());
            config.set("world", treasure.getLocation().getWorld().getName());
            config.set("x", treasure.getLocation().getBlockX());
            config.set("y", treasure.getLocation().getBlockY());
            config.set("z", treasure.getLocation().getBlockZ());
            config.set("rewards", serializeRewards(treasure.getRewards()));
            config.set("nbt-data", treasure.getNbtData());
            config.set("material", treasure.getMaterial());
            config.set("block-state", treasure.getBlockState());
            
            saveConfig(config, file);
            
            // Update treasure-to-collection index
            treasureToCollectionIndex.put(treasure.getId(), treasure.getCollectionId());
        });
    }

    @Override
    public CompletableFuture<Void> saveTreasuresBatch(List<Treasure> treasures) {
        return CompletableFuture.runAsync(() -> {
            if (treasures == null || treasures.isEmpty()) {
                return;
            }

            // Group by collection to reduce repeated directory checks.
            Map<UUID, File> treasuresDirByCollection = new HashMap<>();

            for (Treasure treasure : treasures) {
                File treasuresDir = treasuresDirByCollection.computeIfAbsent(treasure.getCollectionId(), collectionId -> {
                    File collectionDir = new File(collectionsFolder, collectionId.toString());
                    if (!collectionDir.exists()) {
                        collectionDir.mkdirs();
                    }
                    File dir = new File(collectionDir, "treasures");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    return dir;
                });

                File file = new File(treasuresDir, treasure.getId().toString() + YML_EXTENSION);
                FileConfiguration config = new YamlConfiguration();

                config.set("collection-id", treasure.getCollectionId().toString());
                config.set("world", treasure.getLocation().getWorld().getName());
                config.set("x", treasure.getLocation().getBlockX());
                config.set("y", treasure.getLocation().getBlockY());
                config.set("z", treasure.getLocation().getBlockZ());
                config.set("rewards", serializeRewards(treasure.getRewards()));
                config.set("nbt-data", treasure.getNbtData());
                config.set("material", treasure.getMaterial());
                config.set("block-state", treasure.getBlockState());

                saveConfig(config, file);

                treasureToCollectionIndex.put(treasure.getId(), treasure.getCollectionId());
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateTreasureRewards(UUID treasureId, List<Reward> rewards) {
        return CompletableFuture.runAsync(() -> {
            File treasureFile = resolveTreasureFile(treasureId);
            if (treasureFile == null) {
                return;
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(treasureFile);
            config.set("rewards", serializeRewards(rewards));
            saveConfig(config, treasureFile);
        });
    }

    @Override
    public CompletableFuture<Void> updateTreasureRewardsBatch(List<UUID> treasureIds, List<Reward> rewards) {
        return CompletableFuture.runAsync(() -> {
            if (treasureIds == null || treasureIds.isEmpty()) {
                return;
            }

            // Serialize once for the whole batch to reduce allocations.
            Object serializedRewards = serializeRewards(rewards);

            for (UUID treasureId : treasureIds) {
                File treasureFile = resolveTreasureFile(treasureId);
                if (treasureFile == null) {
                    continue;
                }

                FileConfiguration config = YamlConfiguration.loadConfiguration(treasureFile);
                config.set("rewards", serializedRewards);
                saveConfig(config, treasureFile);
            }
        });
    }

    private File resolveTreasureFile(UUID treasureId) {
        UUID collectionId = treasureToCollectionIndex.get(treasureId);
        File treasureFile = null;

        if (collectionId != null) {
            File treasuresDir = new File(new File(collectionsFolder, collectionId.toString()), "treasures");
            treasureFile = new File(treasuresDir, treasureId.toString() + YML_EXTENSION);
            if (!treasureFile.exists()) {
                treasureFile = null;
            }
        }

        // Fallback scan if index is missing/outdated
        if (treasureFile == null) {
            File[] collectionDirs = collectionsFolder.listFiles(File::isDirectory);
            if (collectionDirs != null) {
                for (File collectionDir : collectionDirs) {
                    File treasuresDir = new File(collectionDir, "treasures");
                    if (treasuresDir.exists()) {
                        File candidate = new File(treasuresDir, treasureId.toString() + YML_EXTENSION);
                        if (candidate.exists()) {
                            treasureFile = candidate;
                            break;
                        }
                    }
                }
            }
        }

        return treasureFile;
    }

    @Override
    public CompletableFuture<Void> deleteTreasure(UUID treasureId) {
        return CompletableFuture.runAsync(() -> {
            // Search for the treasure file in all collection folders
            File[] collectionDirs = collectionsFolder.listFiles(File::isDirectory);
            if (collectionDirs != null) {
                for (File collectionDir : collectionDirs) {
                    File treasuresDir = new File(collectionDir, "treasures");
                    if (treasuresDir.exists()) {
                        File file = new File(treasuresDir, treasureId.toString() + YML_EXTENSION);
                        if (file.exists()) {
                            file.delete();
                            break; // Found and deleted
                        }
                    }
                }
            }
            
            // Clean up indexes
            treasureToCollectionIndex.remove(treasureId);
            treasureToFindersIndex.remove(treasureId);
            finderIndexDirty = true;
        });
    }

    @Override
    public CompletableFuture<Integer> deleteTreasuresInCollection(UUID collectionId) {
        return CompletableFuture.supplyAsync(() -> {
            int deleted = 0;
            File collectionDir = new File(collectionsFolder, collectionId.toString());
            File treasuresDir = new File(collectionDir, "treasures");

            // Collect ids from files (authoritative) so we can clean indexes reliably.
            if (treasuresDir.exists() && treasuresDir.isDirectory()) {
                File[] files = treasuresDir.listFiles((dir, name) -> name.endsWith(YML_EXTENSION));
                if (files != null) {
                    for (File file : files) {
                        try {
                            String fileName = file.getName();
                            if (!fileName.endsWith(YML_EXTENSION)) continue;
                            UUID treasureId = UUID.fromString(fileName.substring(0, fileName.length() - YML_EXTENSION.length()));

                            if (file.delete()) {
                                deleted++;
                            }

                            treasureToCollectionIndex.remove(treasureId);
                            treasureToFindersIndex.remove(treasureId);
                        } catch (Exception ignored) {
                            // Ignore malformed filenames; best-effort cleanup.
                        }
                    }
                }
            }

            // Also purge any remaining index entries pointing to this collection.
            treasureToCollectionIndex.entrySet().removeIf(e -> collectionId.equals(e.getValue()));
            finderIndexDirty = true;
            return deleted;
        });
    }

    @Override
    public CompletableFuture<Treasure> loadTreasure(UUID treasureId) {
        return CompletableFuture.supplyAsync(() -> {
            // Use the collection index to find the treasure quickly
            UUID collectionId = treasureToCollectionIndex.get(treasureId);
            if (collectionId == null) {
                return null;
            }
            
            File collectionDir = new File(collectionsFolder, collectionId.toString());
            File treasuresDir = new File(collectionDir, "treasures");
            File file = new File(treasuresDir, treasureId.toString() + YML_EXTENSION);
            
            if (!file.exists()) {
                return null;
            }
            
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                
                String worldName = config.getString("world");
                int x = config.getInt("x");
                int y = config.getInt("y");
                int z = config.getInt("z");
                Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);
                
                List<Map<?, ?>> rewardList = config.getMapList("rewards");
                List<Reward> rewards = deserializeRewards(rewardList);
                String nbtData = config.getString("nbt-data");
                String material = config.getString("material");
                String blockState = config.getString("block-state");
                
                return new Treasure(treasureId, collectionId, loc, rewards, nbtData, material, blockState);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load treasure " + treasureId + ": " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<List<Collection>> loadCollections() {
        return CompletableFuture.supplyAsync(() -> {
            List<Collection> collections = new ArrayList<>();
            File[] collectionDirs = collectionsFolder.listFiles(File::isDirectory);
            if (collectionDirs != null) {
                for (File dir : collectionDirs) {
                    File configFile = new File(dir, "config.yml");
                    if (configFile.exists()) {
                        try {
                            UUID id = UUID.fromString(dir.getName());
                            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                            
                            String name = config.getString("name");
                            boolean enabled = config.getBoolean("enabled");
                            Collection c = new Collection(id, name, enabled);
                            c.setProgressResetCron(config.getString("progress-reset-cron"));
                            c.setSinglePlayerFind(config.getBoolean("single-player-find"));

                            String defaultPresetId = config.getString("default-treasure-reward-preset-id");
                            if (defaultPresetId != null && !defaultPresetId.isBlank()) {
                                try {
                                    c.setDefaultTreasureRewardPresetId(UUID.fromString(defaultPresetId));
                                } catch (IllegalArgumentException ignored) {
                                    c.setDefaultTreasureRewardPresetId(null);
                                }
                            }
                            
                            // Load ACT rules
                            List<Map<?, ?>> rulesList = config.getMapList("act-rules");
                            List<ActRule> actRules = new ArrayList<>();
                            for (Map<?, ?> ruleMap : rulesList) {
                                try {
                                    UUID ruleId = UUID.fromString((String) ruleMap.get("id"));
                                    String ruleName = (String) ruleMap.get("name");
                                    ActRule rule = new ActRule(ruleId, id, ruleName);
                                    rule.setDateRange((String) ruleMap.get("date-range"));
                                    rule.setDuration((String) ruleMap.get("duration"));
                                    rule.setCronExpression((String) ruleMap.get("cron"));
                                    
                                    Object enabledObj = ruleMap.get("enabled");
                                    rule.setEnabled(enabledObj != null ? (Boolean) enabledObj : true);
                                    
                                    actRules.add(rule);
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Failed to load ACT rule: " + e.getMessage());
                                }
                            }
                            c.setActRules(actRules);
                            
                            List<Map<?, ?>> rewardList = config.getMapList("rewards");
                            c.setCompletionRewards(deserializeRewards(rewardList));
                            
                            collections.add(c);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to load collection from " + dir.getName() + ": " + e.getMessage());
                        }
                    }
                }
            }
            return collections;
        });
    }

    @Override
    public CompletableFuture<Void> saveCollection(Collection collection) {
        return CompletableFuture.runAsync(() -> {
            File dir = new File(collectionsFolder, collection.getId().toString());
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            config.set("name", collection.getName());
            config.set("enabled", collection.isEnabled());
            config.set("progress-reset-cron", collection.getProgressResetCron());
            config.set("single-player-find", collection.isSinglePlayerFind());
            config.set("default-treasure-reward-preset-id",
                collection.getDefaultTreasureRewardPresetId() != null ? collection.getDefaultTreasureRewardPresetId().toString() : null);
            
            // Save ACT rules
            List<Map<String, Object>> rulesList = new ArrayList<>();
            for (ActRule rule : collection.getActRules()) {
                Map<String, Object> ruleMap = new HashMap<>();
                ruleMap.put("id", rule.getId().toString());
                ruleMap.put("name", rule.getName());
                ruleMap.put("date-range", rule.getDateRange());
                ruleMap.put("duration", rule.getDuration());
                ruleMap.put("cron", rule.getCronExpression());
                ruleMap.put("enabled", rule.isEnabled());
                rulesList.add(ruleMap);
            }
            config.set("act-rules", rulesList);
            
            config.set("rewards", serializeRewards(collection.getCompletionRewards()));
            
            saveConfig(config, file);
        });
    }

    @Override
    public CompletableFuture<List<RewardPreset>> loadRewardPresets(RewardPresetType type) {
        return CompletableFuture.supplyAsync(() -> {
            List<RewardPreset> presets = new ArrayList<>();

            File folder = getPresetFolder(type);
            if (folder != null && folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles((dir, name) -> name.endsWith(YML_EXTENSION));
                if (files != null) {
                    for (File file : files) {
                        String idStr = file.getName().replace(YML_EXTENSION, "");
                        try {
                            UUID id = UUID.fromString(idStr);
                            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                            String name = config.getString("name");
                            List<Map<?, ?>> rewardList = config.getMapList("rewards");
                            List<Reward> rewards = deserializeRewards(rewardList);
                            if (name != null && !name.isBlank()) {
                                presets.add(new RewardPreset(id, type, name, rewards));
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to load reward preset file " + file.getName() + ": " + e.getMessage());
                        }
                    }
                }
            }

            // Back-compat: if folder is empty and legacy file exists, try reading legacy format.
            if (presets.isEmpty() && legacyRewardPresetsFile != null && legacyRewardPresetsFile.exists()) {
                FileConfiguration legacy = YamlConfiguration.loadConfiguration(legacyRewardPresetsFile);
                String rootKey = type == RewardPresetType.COLLECTION ? "collection" : "treasure";
                var section = legacy.getConfigurationSection(rootKey);
                if (section != null) {
                    for (String idStr : section.getKeys(false)) {
                        try {
                            UUID id = UUID.fromString(idStr);
                            String name = section.getString(idStr + ".name");
                            List<Map<?, ?>> rewardList = section.getMapList(idStr + ".rewards");
                            List<Reward> rewards = deserializeRewards(rewardList);
                            if (name != null && !name.isBlank()) {
                                presets.add(new RewardPreset(id, type, name, rewards));
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to load legacy reward preset " + idStr + ": " + e.getMessage());
                        }
                    }
                }
            }

            // Stable ordering for menus
            presets.sort(Comparator.comparing(RewardPreset::getName, String.CASE_INSENSITIVE_ORDER));
            return presets;
        });
    }

    @Override
    public CompletableFuture<Void> saveRewardPreset(RewardPreset preset) {
        return CompletableFuture.runAsync(() -> {
            File file = getPresetFile(preset.getType(), preset.getId());
            FileConfiguration config = new YamlConfiguration();
            config.set("name", preset.getName());
            config.set("rewards", serializeRewards(preset.getRewards()));
            saveConfigAtomic(config, file);
        });
    }

    @Override
    public CompletableFuture<Void> deleteRewardPreset(RewardPresetType type, UUID presetId) {
        return CompletableFuture.runAsync(() -> {
            File file = getPresetFile(type, presetId);
            if (file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteCollection(UUID id) {
        return CompletableFuture.runAsync(() -> {
            // Get treasure IDs before deleting files to clean up indexes
            List<String> treasureIds = getTreasureIdsForCollection(id);

            File dir = new File(collectionsFolder, id.toString());
            if (dir.exists()) {
                deleteDirectoryRecursively(dir);
            }

            // Clean up indexes
            for (String tidStr : treasureIds) {
                try {
                    UUID tid = UUID.fromString(tidStr);
                    treasureToCollectionIndex.remove(tid);
                    treasureToFindersIndex.remove(tid);
                } catch (IllegalArgumentException ignored) {}
            }
            if (!treasureIds.isEmpty()) {
                finderIndexDirty = true;
            }
        });
    }

    private void deleteDirectoryRecursively(File file) {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteDirectoryRecursively(entry);
                }
            }
        }
        file.delete();
    }

    @Override
    public CompletableFuture<Boolean> renameCollection(UUID id, String newName) {
        return CompletableFuture.supplyAsync(() -> {
            File dir = new File(collectionsFolder, id.toString());
            File file = new File(dir, "config.yml");
            if (!file.exists()) {
                return false;
            }
            
            // Check uniqueness
            File[] collectionDirs = collectionsFolder.listFiles(File::isDirectory);
            if (collectionDirs != null) {
                for (File d : collectionDirs) {
                    if (d.getName().equals(id.toString())) continue; // Skip self
                    File configFile = new File(d, "config.yml");
                    if (configFile.exists()) {
                        FileConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
                        if (newName.equals(cfg.getString("name"))) {
                            return false;
                        }
                    }
                }
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set("name", newName);
            saveConfig(config, file);
            return true;
        });
    }

    @Override
    public CompletableFuture<List<PlayerData>> loadAllPlayerData() {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerData> allData = new ArrayList<>();
            if (playerDataFolder.exists()) {
                File[] files = playerDataFolder.listFiles((dir, name) -> name.endsWith(YML_EXTENSION));
                if (files != null) {
                    for (File file : files) {
                        String uuidStr = file.getName().replace(YML_EXTENSION, "");
                        try {
                            UUID uuid = UUID.fromString(uuidStr);
                            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                            PlayerData pd = new PlayerData(uuid);
                            List<String> found = config.getStringList("found-treasures");
                            for (String s : found) {
                                try {
                                    pd.addFoundTreasure(UUID.fromString(s));
                                } catch (IllegalArgumentException ignored) {}
                            }
                            allData.add(pd);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid UUID in filename: " + file.getName());
                        }
                    }
                }
            }
            return allData;
        });
    }

    @Override
    public CompletableFuture<Integer> resetAllProgress() {
        return CompletableFuture.supplyAsync(() -> {
            int count = 0;
            if (playerDataFolder.exists()) {
                File[] files = playerDataFolder.listFiles((dir, name) -> name.endsWith(YML_EXTENSION));
                if (files != null) {
                    for (File file : files) {
                        // Only load config if we need accurate count
                        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                        count += config.getStringList("found-treasures").size();
                        file.delete();
                    }
                }
            }
            
            // Clear finder index
            treasureToFindersIndex.clear();
            finderIndexDirty = true;
            
            return count;
        });
    }

    @Override
    public CompletableFuture<Integer> resetCollectionProgress(UUID collectionId) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> collectionTreasureIds = getTreasureIdsForCollection(collectionId);
            int count = 0;

            if (playerDataFolder.exists()) {
                File[] files = playerDataFolder.listFiles((dir, name) -> name.endsWith(YML_EXTENSION));
                if (files != null) {
                    for (File file : files) {
                        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                        List<String> found = config.getStringList("found-treasures");
                        int sizeBefore = found.size();
                        found.removeAll(collectionTreasureIds);
                        int removed = sizeBefore - found.size();
                        if (removed > 0) {
                            count += removed;
                            config.set("found-treasures", found);
                            saveConfig(config, file);
                        }
                    }
                }
            }
            
            // Clean finder index for treasures in this collection
            for (String treasureIdStr : collectionTreasureIds) {
                try {
                    UUID treasureId = UUID.fromString(treasureIdStr);
                    treasureToFindersIndex.remove(treasureId);
                } catch (IllegalArgumentException ignored) {}
            }
            finderIndexDirty = true;
            
            return count;
        });
    }

    @Override
    public CompletableFuture<Integer> resetPlayerProgress(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(playerDataFolder, playerUuid.toString() + YML_EXTENSION);
            if (!file.exists()) {
                return 0;
            }
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            List<String> found = config.getStringList("found-treasures");
            int count = found.size();
            if (count > 0) {
                // Remove player from finder index for all their treasures
                for (String treasureIdStr : found) {
                    try {
                        UUID treasureId = UUID.fromString(treasureIdStr);
                        Set<UUID> finders = treasureToFindersIndex.get(treasureId);
                        if (finders != null) {
                            finders.remove(playerUuid);
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
                finderIndexDirty = true;
                
                file.delete();
            }
            return count;
        });
    }

    @Override
    public CompletableFuture<Integer> resetPlayerCollectionProgress(UUID playerUuid, UUID collectionId) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(playerDataFolder, playerUuid.toString() + YML_EXTENSION);
            if (!file.exists()) {
                return 0;
            }
            
            List<String> collectionTreasureIds = getTreasureIdsForCollection(collectionId);
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            List<String> found = config.getStringList("found-treasures");
            int sizeBefore = found.size();
            
            // Track removed treasures for index update
            List<String> removedTreasures = new ArrayList<>();
            for (String tid : found) {
                if (collectionTreasureIds.contains(tid)) {
                    removedTreasures.add(tid);
                }
            }
            
            found.removeAll(collectionTreasureIds);
            int removed = sizeBefore - found.size();
            
            if (removed > 0) {
                config.set("found-treasures", found);
                saveConfig(config, file);
                
                // Update finder index
                for (String treasureIdStr : removedTreasures) {
                    try {
                        UUID treasureId = UUID.fromString(treasureIdStr);
                        Set<UUID> finders = treasureToFindersIndex.get(treasureId);
                        if (finders != null) {
                            finders.remove(playerUuid);
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
                finderIndexDirty = true;
            }
            return removed;
        });
    }

    @Override
    public CompletableFuture<Map<UUID, Integer>> getLeaderboard(UUID collectionId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, Integer> scores = new HashMap<>();
            FileConfiguration config = YamlConfiguration.loadConfiguration(leaderboardFile);
            String path = "collections." + collectionId;
            if (config.contains(path) && config.getConfigurationSection(path) != null) {
                for (String uuidStr : config.getConfigurationSection(path).getKeys(false)) {
                    int score = config.getInt(path + "." + uuidStr);
                    scores.put(UUID.fromString(uuidStr), score);
                }
            }

            // Sort and limit
            return scores.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                    .limit(limit)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                    ));
        });
    }

    /**
     * Updates the leaderboard for a specific player across all collections.
     * Uses in-memory index for fast collection filtering.
     * Called by LeaderboardManager on a schedule, not on every save.
     */
    public void updatePlayerLeaderboard(PlayerData playerData) {
        if (leaderboardFile == null) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(leaderboardFile);
        
        // Get all unique collection IDs from the index
        Set<UUID> collectionIds = new HashSet<>(treasureToCollectionIndex.values());
        
        for (UUID collectionId : collectionIds) {
            int count = 0;
            for (UUID foundId : playerData.getFoundTreasures()) {
                if (collectionId.equals(treasureToCollectionIndex.get(foundId))) {
                    count++;
                }
            }
            
            String path = "collections." + collectionId + "." + playerData.getPlayerUuid();
            if (count > 0) {
                config.set(path, count);
            } else {
                config.set(path, null);
            }
        }
        saveConfig(config, leaderboardFile);
    }
    
    /**
     * Saves the finder index to disk. Called periodically by a background task.
     */
    public void flushFinderIndex() {
        if (finderIndexDirty) {
            saveFinderIndex();
        }
    }

    private List<String> getTreasureIdsForCollection(UUID collectionId) {
        List<String> ids = new ArrayList<>();
        File collectionDir = new File(collectionsFolder, collectionId.toString());
        File treasuresDir = new File(collectionDir, "treasures");
        
        if (treasuresDir.exists() && treasuresDir.isDirectory()) {
            File[] files = treasuresDir.listFiles((dir, name) -> name.endsWith(YML_EXTENSION));
            if (files != null) {
                for (File file : files) {
                    ids.add(file.getName().replace(YML_EXTENSION, ""));
                }
            }
        }
        return ids;
    }

    @Override
    public CompletableFuture<List<UUID>> getPlayersWhoFound(UUID treasureId) {
        return CompletableFuture.supplyAsync(() -> {
            // Fast O(1) lookup using in-memory index
            Set<UUID> finders = treasureToFindersIndex.get(treasureId);
            if (finders == null) {
                return new ArrayList<>();
            }
            return new ArrayList<>(finders);
        });
    }

    @Override
    public CompletableFuture<Integer> getFoundPlayerCount(UUID treasureId) {
        return CompletableFuture.supplyAsync(() -> {
            Set<UUID> finders = treasureToFindersIndex.get(treasureId);
            return finders == null ? 0 : finders.size();
        });
    }
    
    @Override
    public CompletableFuture<Set<UUID>> getPlayerFoundInCollection(UUID playerUuid, UUID collectionId) {
        return CompletableFuture.supplyAsync(() -> {
            Set<UUID> result = new HashSet<>();
            
            // Load player's found treasures
            File file = new File(playerDataFolder, playerUuid.toString() + YML_EXTENSION);
            if (!file.exists()) {
                return result;
            }
            
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            List<String> found = config.getStringList("found-treasures");
            
            // Filter by collection using in-memory index (O(n) where n = player's found count)
            for (String treasureIdStr : found) {
                try {
                    UUID treasureId = UUID.fromString(treasureIdStr);
                    UUID treasureCollection = treasureToCollectionIndex.get(treasureId);
                    if (collectionId.equals(treasureCollection)) {
                        result.add(treasureId);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
            
            return result;
        });
    }

    private List<Reward> deserializeRewards(List<Map<?, ?>> list) {
        List<Reward> rewards = new ArrayList<>();
        if (list == null) return rewards;
        for (Map<?, ?> map : list) {
            try {
                RewardType type = RewardType.valueOf((String) map.get("type"));
                // Handle potential Integer to Double conversion issues from YAML
                Object chanceObj = map.get("chance");
                double chance = 0;
                if (chanceObj instanceof Number) {
                    chance = ((Number) chanceObj).doubleValue();
                }
                String message = (String) map.get("message");
                String broadcast = (String) map.get("broadcast");
                String value = (String) map.get("value");
                rewards.add(new Reward(type, chance,message,broadcast, value));
            } catch (Exception e) {
                plugin.getLogger().warning("Error deserializing reward: " + e.getMessage());
            }
        }
        return rewards;
    }

    private List<Map<String, Object>> serializeRewards(List<Reward> rewards) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (rewards == null) return list;
        for (Reward r : rewards) {
            Map<String, Object> map = new HashMap<>();
            map.put("type", r.getType().name());
            map.put("chance", r.getChance());
            map.put("message",r.getMessage());
            map.put("broadcast",r.getBroadcast());
            map.put("value", r.getValue());
            list.add(map);
        }
        return list;
    }
    
    @Override
    public void flushIndexes() {
        flushFinderIndex();
    }
    
    @Override
    public void rebuildLeaderboardCache() {
        // Rebuild leaderboard.yml by scanning all player data
        CompletableFuture.runAsync(() -> {
            plugin.getLogger().info("Rebuilding YAML leaderboard cache...");
            
            FileConfiguration config = new YamlConfiguration();
            
            // Get all unique collection IDs
            Set<UUID> collectionIds = new HashSet<>(treasureToCollectionIndex.values());
            
            // For each player file, calculate their scores per collection
            if (playerDataFolder.exists()) {
                File[] files = playerDataFolder.listFiles((dir, name) -> name.endsWith(YML_EXTENSION));
                if (files != null) {
                    for (File file : files) {
                        try {
                            UUID playerUuid = UUID.fromString(file.getName().replace(YML_EXTENSION, ""));
                            FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(file);
                            List<String> found = playerConfig.getStringList("found-treasures");
                            
                            // Count treasures per collection
                            Map<UUID, Integer> counts = new HashMap<>();
                            for (String treasureIdStr : found) {
                                try {
                                    UUID treasureId = UUID.fromString(treasureIdStr);
                                    UUID collectionId = treasureToCollectionIndex.get(treasureId);
                                    if (collectionId != null) {
                                        counts.merge(collectionId, 1, Integer::sum);
                                    }
                                } catch (IllegalArgumentException ignored) {}
                            }
                            
                            // Write to leaderboard config
                            for (Map.Entry<UUID, Integer> entry : counts.entrySet()) {
                                config.set("collections." + entry.getKey() + "." + playerUuid, entry.getValue());
                            }
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            }
            
            saveConfig(config, leaderboardFile);
            plugin.getLogger().info("YAML leaderboard cache rebuilt successfully");
        });
    }
    
    /**
     * Starts the periodic index flush scheduler.
     * Should be called after init() to begin background maintenance.
     * @param intervalSeconds how often to flush indexes (in seconds)
     */
    public void startPeriodicFlush(int intervalSeconds) {
        int safeIntervalSeconds = Math.max(1, intervalSeconds);
        long intervalTicks = 20L * safeIntervalSeconds;
        if (flushTask != null && !flushTask.isCancelled()) {
            flushTask.cancel();
        }
        flushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (finderIndexDirty) {
                saveFinderIndex();
                plugin.getLogger().fine("Flushed finder index to disk");
            }
        }, intervalTicks, intervalTicks);
    }
}