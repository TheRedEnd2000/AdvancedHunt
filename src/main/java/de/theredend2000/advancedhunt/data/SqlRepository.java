package de.theredend2000.advancedhunt.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.theredend2000.advancedhunt.model.*;
import de.theredend2000.advancedhunt.model.Collection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SqlRepository implements DataRepository {

    private static final Type REWARD_LIST_TYPE = new TypeToken<List<Reward>>(){}.getType();
    
    private HikariDataSource dataSource;
    private final JavaPlugin plugin;
    private final String host, database, username, password;
    private final int port;
    private final boolean useSqlite;
    private final Gson gson;

    private volatile ExecutorService sqliteExecutor;
    private volatile Executor asyncExecutor;

    private final Map<Integer, Consumer<Connection>> schemaMigrations = new HashMap<>();
    // Cache the version to avoid repeated DB queries
    private int cachedSchemaVersion = -1;

    public SqlRepository(JavaPlugin plugin, String host, int port, String database, String username, String password, boolean useSqlite) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.useSqlite = useSqlite;
        this.gson = new Gson();

        if (useSqlite) {
            ensureSqliteExecutor();
        } else {
            this.sqliteExecutor = null;
            this.asyncExecutor = null;
        }
        
        registerMigrations();
    }

    private synchronized void ensureSqliteExecutor() {
        if (!useSqlite) {
            return;
        }

        ExecutorService current = this.sqliteExecutor;
        if (current != null && !current.isShutdown() && !current.isTerminated()) {
            return;
        }

        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "AdvancedHunt-SQLite");
            thread.setDaemon(true);
            return thread;
        };
        this.sqliteExecutor = Executors.newSingleThreadExecutor(threadFactory);
        this.asyncExecutor = this.sqliteExecutor;
    }

    private <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        if (asyncExecutor == null) {
            return CompletableFuture.supplyAsync(supplier);
        }
        return CompletableFuture.supplyAsync(supplier, asyncExecutor);
    }

    private CompletableFuture<Void> runAsync(Runnable runnable) {
        if (asyncExecutor == null) {
            return CompletableFuture.runAsync(runnable);
        }
        return CompletableFuture.runAsync(runnable, asyncExecutor);
    }

    private void registerMigrations() {
        schemaMigrations.put(1, conn -> {
            // Initial schema version
        });
        
        schemaMigrations.put(2, conn -> {
            try {
                conn.createStatement().execute("ALTER TABLE ah_treasures ADD COLUMN block_data TEXT");
            } catch (SQLException ignored) {}
        });
        
        schemaMigrations.put(3, conn -> {
            try {
                conn.createStatement().execute("ALTER TABLE ah_treasures ADD COLUMN material VARCHAR(64)");
                conn.createStatement().execute("ALTER TABLE ah_treasures ADD COLUMN block_state TEXT");
            } catch (SQLException ignored) {}
        });
        
        schemaMigrations.put(4, conn -> this.createPerformanceIndexes(conn));
        
        schemaMigrations.put(5, conn -> {
            try {
                conn.createStatement().execute("ALTER TABLE ah_collections ADD COLUMN progress_reset_cron VARCHAR(64)");
            } catch (SQLException ignored) {}
        });

        schemaMigrations.put(6, conn -> {
            try {
                conn.createStatement().execute("ALTER TABLE ah_collections ADD COLUMN default_treasure_reward_preset_id VARCHAR(36)");
            } catch (SQLException ignored) {}

            try {
                conn.createStatement().execute("CREATE TABLE IF NOT EXISTS ah_reward_presets (" +
                        "id VARCHAR(36) PRIMARY KEY, " +
                        "type VARCHAR(16) NOT NULL, " +
                        "name VARCHAR(64) NOT NULL, " +
                        "rewards TEXT)");
            } catch (SQLException ignored) {}
        });

            schemaMigrations.put(7, conn -> {
                try {
                    conn.createStatement().execute("CREATE TABLE IF NOT EXISTS ah_place_presets (" +
                            "id VARCHAR(36) PRIMARY KEY, " +
                            "grp VARCHAR(64) NOT NULL, " +
                            "name VARCHAR(64) NOT NULL, " +
                            "item TEXT)");
                } catch (SQLException ignored) {}
            });

        schemaMigrations.put(8, conn -> {
            try {
                conn.createStatement().execute("CREATE TABLE IF NOT EXISTS ah_place_preset_groups (" +
                        "grp VARCHAR(64) PRIMARY KEY)");
            } catch (SQLException ignored) {}
        });
    }

    @Override
    public void init() {
        if (useSqlite) {
            ensureSqliteExecutor();
        }
        cachedSchemaVersion = -1;

        HikariConfig config = new HikariConfig();
        if (useSqlite) {
            // Ensure the plugin data folder exists
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "database.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());

            // SQLite is single-writer; using a pool > 1 causes frequent SQLITE_BUSY during concurrent writes.
            // Serialize access and let SQLite wait a bit for locks.
            config.setMaximumPoolSize(1);
            config.setMinimumIdle(1);
            config.setConnectionInitSql("PRAGMA foreign_keys=ON; PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL; PRAGMA busy_timeout=5000;");
        } else {
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(10);
        }
        dataSource = new HikariDataSource(config);

        createTables();
        upgradeSchema();
    }

    @Override
    public void reload() {
        shutdown();
        init();
    }

    private void createTables() {
        try (Connection conn = dataSource.getConnection()) {
            // Schema Version Table
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS ah_schema_version (" +
                    "version INT PRIMARY KEY)");

            // Collections Table
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS ah_collections (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(64) UNIQUE, " +
                    "enabled BOOLEAN, " +
                    "reset_cron VARCHAR(64), " +
                    "active_start VARCHAR(32), " +
                    "active_end VARCHAR(32), " +
                    "progress_reset_cron VARCHAR(64), " +
                    "single_player_find BOOLEAN, " +
                    "rewards TEXT, " +
                    "default_treasure_reward_preset_id VARCHAR(36))");

                // Reward Presets Table
                conn.createStatement().execute("CREATE TABLE IF NOT EXISTS ah_reward_presets (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "type VARCHAR(16) NOT NULL, " +
                    "name VARCHAR(64) NOT NULL, " +
                    "rewards TEXT)");

            // ACT Rules Table
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS ah_act_rules (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "collection_id VARCHAR(36) NOT NULL, " +
                    "name VARCHAR(64), " +
                    "date_range VARCHAR(64), " +
                    "duration VARCHAR(16), " +
                    "cron_expression VARCHAR(64), " +
                    "enabled BOOLEAN, " +
                    "FOREIGN KEY (collection_id) REFERENCES ah_collections(id) ON DELETE CASCADE)");

                    // Place Presets Table
                    conn.createStatement().execute("CREATE TABLE IF NOT EXISTS ah_place_presets (" +
                        "id VARCHAR(36) PRIMARY KEY, " +
                        "grp VARCHAR(64) NOT NULL, " +
                        "name VARCHAR(64) NOT NULL, " +
                        "item TEXT)");

                    // Place Preset Groups Table
                    conn.createStatement().execute("CREATE TABLE IF NOT EXISTS ah_place_preset_groups (" +
                        "grp VARCHAR(64) PRIMARY KEY)");


            // Treasures Table
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS ah_treasures (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "collection_id VARCHAR(36), " +
                    "world VARCHAR(64), " +
                    "x INT, y INT, z INT, " +
                    "rewards TEXT, " +
                    "block_data TEXT, " +
                    "material VARCHAR(64), " +
                    "block_state TEXT)");

            // Player Found Data Table
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS ah_player_found (" +
                    "player_uuid VARCHAR(36), " +
                    "treasure_id VARCHAR(36), " +
                    "PRIMARY KEY (player_uuid, treasure_id))");

            // Player Settings Table
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS ah_players (" +
                    "uuid VARCHAR(36) PRIMARY KEY)");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getSchemaVersion() {
        if (cachedSchemaVersion != -1) return cachedSchemaVersion;
        
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.createStatement().executeQuery("SELECT version FROM ah_schema_version")) {
            if (rs.next()) {
                cachedSchemaVersion = rs.getInt("version");
                return cachedSchemaVersion;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void upgradeSchema() {
        try (Connection conn = dataSource.getConnection()) {
            int currentVersion = 0;
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT version FROM ah_schema_version")) {
                if (rs.next()) {
                    currentVersion = rs.getInt("version");
                } else {
                    conn.createStatement().execute("INSERT INTO ah_schema_version (version) VALUES (0)");
                }
            }
            
            int latestVersion = schemaMigrations.keySet().stream().mapToInt(v -> v).max().orElse(0);
            
            for (int i = currentVersion + 1; i <= latestVersion; i++) {
                if (schemaMigrations.containsKey(i)) {
                    try {
                        schemaMigrations.get(i).accept(conn);
                        plugin.getLogger().info("Upgraded database schema to version " + i);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Failed to apply schema migration for version " + i);
                        e.printStackTrace();
                        break; // Stop migration on error
                    }
                }
                // Update version number
                try (PreparedStatement ps = conn.prepareStatement("UPDATE ah_schema_version SET version = ?")) {
                    ps.setInt(1, i);
                    ps.executeUpdate();
                }
                cachedSchemaVersion = i;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Creates database indexes to optimize leaderboard query performance.
     * These indexes significantly improve JOIN, GROUP BY, and ORDER BY operations.
     */
    private void createPerformanceIndexes(Connection conn) {
        String[] indexes = useSqlite ? new String[] {
            // SQLite index syntax
            "CREATE INDEX IF NOT EXISTS idx_player_found_player ON ah_player_found(player_uuid)",
            "CREATE INDEX IF NOT EXISTS idx_player_found_treasure ON ah_player_found(treasure_id)",
            "CREATE INDEX IF NOT EXISTS idx_treasures_collection ON ah_treasures(collection_id)",
            "CREATE INDEX IF NOT EXISTS idx_leaderboard_query ON ah_treasures(collection_id, id)"
        } : new String[] {
            // MySQL index syntax
            "CREATE INDEX idx_player_found_player ON ah_player_found(player_uuid)",
            "CREATE INDEX idx_player_found_treasure ON ah_player_found(treasure_id)",
            "CREATE INDEX idx_treasures_collection ON ah_treasures(collection_id)",
            "CREATE INDEX idx_leaderboard_query ON ah_treasures(collection_id, id)"
        };
        
        for (String indexSql : indexes) {
            try {
                conn.createStatement().execute(indexSql);
                plugin.getLogger().info("Created database index for performance optimization");
            } catch (SQLException e) {
                // Index might already exist, ignore
                if (!e.getMessage().contains("already exists") && !e.getMessage().contains("Duplicate")) {
                    plugin.getLogger().warning("Failed to create index: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Helper method to load ACT rules for a collection
     */
    private List<ActRule> loadActRules(Connection conn, UUID collectionId) throws SQLException {
        List<ActRule> rules = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM ah_act_rules WHERE collection_id = ?")) {
            ps.setString(1, collectionId.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID ruleId = UUID.fromString(rs.getString("id"));
                String name = rs.getString("name");
                ActRule rule = new ActRule(ruleId, collectionId, name);
                rule.setDateRange(rs.getString("date_range"));
                rule.setDuration(rs.getString("duration"));
                rule.setCronExpression(rs.getString("cron_expression"));
                rule.setEnabled(rs.getBoolean("enabled"));
                rules.add(rule);
            }
        }
        return rules;
    }

    @Override
    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }

        if (sqliteExecutor != null) {
            sqliteExecutor.shutdownNow();
        }
    }

    @Override
    public CompletableFuture<PlayerData> loadPlayerData(UUID playerUuid) {
        return supplyAsync(() -> {
            PlayerData data = new PlayerData(playerUuid);
            try (Connection conn = dataSource.getConnection()) {
                // Load found treasures
                try (PreparedStatement ps = conn.prepareStatement("SELECT treasure_id FROM ah_player_found WHERE player_uuid = ?")) {
                    ps.setString(1, playerUuid.toString());
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        data.addFoundTreasure(UUID.fromString(rs.getString("treasure_id")));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return data;
        });
    }

    @Override
    public CompletableFuture<Void> savePlayerData(PlayerData playerData) {
        return runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                // Ensure player row exists (legacy table previously stored player settings)
                String ensurePlayerSql = useSqlite
                        ? "INSERT OR IGNORE INTO ah_players (uuid) VALUES (?)"
                        : "INSERT IGNORE INTO ah_players (uuid) VALUES (?)";

                try (PreparedStatement ps = conn.prepareStatement(ensurePlayerSql)) {
                    ps.setString(1, playerData.getPlayerUuid().toString());
                    ps.executeUpdate();
                }

                // Save found treasures (Batch insert new ones)
                String insertSql = useSqlite 
                    ? "INSERT OR IGNORE INTO ah_player_found (player_uuid, treasure_id) VALUES (?, ?)"
                    : "INSERT IGNORE INTO ah_player_found (player_uuid, treasure_id) VALUES (?, ?)";
                
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    for (UUID treasureId : playerData.getFoundTreasures()) {
                        ps.setString(1, playerData.getPlayerUuid().toString());
                        ps.setString(2, treasureId.toString());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> savePlayerDataBatch(List<PlayerData> playerDataList) {
        return runAsync(() -> {
            if (playerDataList.isEmpty()) return;
            
            try (Connection conn = dataSource.getConnection()) {
                boolean originalAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);
                
                try {
                    // 1. Ensure player rows exist
                    String ensurePlayerSql = useSqlite
                            ? "INSERT OR IGNORE INTO ah_players (uuid) VALUES (?)"
                            : "INSERT IGNORE INTO ah_players (uuid) VALUES (?)";

                    try (PreparedStatement ps = conn.prepareStatement(ensurePlayerSql)) {
                        for (PlayerData pd : playerDataList) {
                            ps.setString(1, pd.getPlayerUuid().toString());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }

                    // 2. Batch save found treasures
                    String insertSql = useSqlite 
                        ? "INSERT OR IGNORE INTO ah_player_found (player_uuid, treasure_id) VALUES (?, ?)"
                        : "INSERT IGNORE INTO ah_player_found (player_uuid, treasure_id) VALUES (?, ?)";
                    
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        for (PlayerData pd : playerDataList) {
                            String uuidStr = pd.getPlayerUuid().toString();
                            for (UUID treasureId : pd.getFoundTreasures()) {
                                ps.setString(1, uuidStr);
                                ps.setString(2, treasureId.toString());
                                ps.addBatch();
                            }
                        }
                        ps.executeBatch();
                    }
                    
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(originalAutoCommit);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<List<Treasure>> loadTreasures() {
        return supplyAsync(() -> {
            List<Treasure> treasures = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM ah_treasures")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    UUID collectionId = UUID.fromString(rs.getString("collection_id"));
                    String world = rs.getString("world");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    Location loc = new Location(Bukkit.getWorld(world), x, y, z);
                    
                    String rewardsJson = rs.getString("rewards");
                    List<Reward> rewards = gson.fromJson(rewardsJson, REWARD_LIST_TYPE);
                    String nbtData = rs.getString("block_data");
                    String material = rs.getString("material");
                    String blockState = rs.getString("block_state");
                    
                    treasures.add(new Treasure(id, collectionId, loc, rewards, nbtData, material, blockState));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return treasures;
        });
    }

    @Override
    public CompletableFuture<List<TreasureCore>> loadTreasureCores() {
        return supplyAsync(() -> {
            List<TreasureCore> cores = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT id, collection_id, world, x, y, z, material, block_state FROM ah_treasures")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    UUID collectionId = UUID.fromString(rs.getString("collection_id"));
                    String world = rs.getString("world");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    Location loc = new Location(Bukkit.getWorld(world), x, y, z);
                    String material = rs.getString("material");
                    String blockState = rs.getString("block_state");
                    cores.add(new TreasureCore(id, collectionId, loc, material, blockState));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return cores;
        });
    }

    @Override
    public CompletableFuture<List<UUID>> getAllTreasureUUIDs() {
        return supplyAsync(() -> {
            List<UUID> ids = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT id FROM ah_treasures")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    try {
                        ids.add(UUID.fromString(rs.getString("id")));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return ids;
        });
    }

    @Override
    public CompletableFuture<Void> saveTreasure(Treasure treasure) {
        return runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("REPLACE INTO ah_treasures (id, collection_id, world, x, y, z, rewards, block_data, material, block_state) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, treasure.getId().toString());
                ps.setString(2, treasure.getCollectionId().toString());
                ps.setString(3, treasure.getLocation().getWorld().getName());
                ps.setInt(4, treasure.getLocation().getBlockX());
                ps.setInt(5, treasure.getLocation().getBlockY());
                ps.setInt(6, treasure.getLocation().getBlockZ());
                ps.setString(7, gson.toJson(treasure.getRewards()));
                ps.setString(8, treasure.getNbtData());
                ps.setString(9, treasure.getMaterial());
                ps.setString(10, treasure.getBlockState());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveTreasuresBatch(List<Treasure> treasures) {
        return runAsync(() -> {
            if (treasures == null || treasures.isEmpty()) {
                return;
            }

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "REPLACE INTO ah_treasures (id, collection_id, world, x, y, z, rewards, block_data, material, block_state) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                boolean originalAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);
                try {
                    for (Treasure treasure : treasures) {
                        ps.setString(1, treasure.getId().toString());
                        ps.setString(2, treasure.getCollectionId().toString());
                        ps.setString(3, treasure.getLocation().getWorld().getName());
                        ps.setInt(4, treasure.getLocation().getBlockX());
                        ps.setInt(5, treasure.getLocation().getBlockY());
                        ps.setInt(6, treasure.getLocation().getBlockZ());
                        ps.setString(7, gson.toJson(treasure.getRewards()));
                        ps.setString(8, treasure.getNbtData());
                        ps.setString(9, treasure.getMaterial());
                        ps.setString(10, treasure.getBlockState());
                        ps.addBatch();
                    }

                    ps.executeBatch();
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(originalAutoCommit);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteTreasure(UUID treasureId) {
        return runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM ah_treasures WHERE id = ?")) {
                ps.setString(1, treasureId.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Integer> deleteTreasuresInCollection(UUID collectionId) {
        return supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM ah_treasures WHERE collection_id = ?")) {
                ps.setString(1, collectionId.toString());
                return ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                return 0;
            }
        });
    }

    @Override
    public CompletableFuture<Treasure> loadTreasure(UUID treasureId) {
        return supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM ah_treasures WHERE id = ?")) {
                ps.setString(1, treasureId.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    UUID collectionId = UUID.fromString(rs.getString("collection_id"));
                    String world = rs.getString("world");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    Location loc = new Location(Bukkit.getWorld(world), x, y, z);
                    
                    String rewardsJson = rs.getString("rewards");
                    List<Reward> rewards = gson.fromJson(rewardsJson, REWARD_LIST_TYPE);
                    String nbtData = rs.getString("block_data");
                    String material = rs.getString("material");
                    String blockState = rs.getString("block_state");
                    
                    return new Treasure(id, collectionId, loc, rewards, nbtData, material, blockState);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> updateTreasureRewards(UUID treasureId, List<Reward> rewards) {
        return runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE ah_treasures SET rewards = ? WHERE id = ?")) {
                ps.setString(1, gson.toJson(rewards));
                ps.setString(2, treasureId.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateTreasureRewardsBatch(List<UUID> treasureIds, List<Reward> rewards) {
        return runAsync(() -> {
            if (treasureIds == null || treasureIds.isEmpty()) {
                return;
            }

            String rewardsJson = gson.toJson(rewards);

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE ah_treasures SET rewards = ? WHERE id = ?")) {
                boolean oldAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);
                try {
                    for (UUID treasureId : treasureIds) {
                        ps.setString(1, rewardsJson);
                        ps.setString(2, treasureId.toString());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    conn.commit();
                } catch (SQLException e) {
                    try {
                        conn.rollback();
                    } catch (SQLException ignored) {
                    }
                    throw e;
                } finally {
                    try {
                        conn.setAutoCommit(oldAutoCommit);
                    } catch (SQLException ignored) {
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<List<Collection>> loadCollections() {
        return supplyAsync(() -> {
            List<Collection> collections = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM ah_collections")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    String name = rs.getString("name");
                    boolean enabled = rs.getBoolean("enabled");
                    Collection c = new Collection(id, name, enabled);
                    c.setProgressResetCron(rs.getString("progress_reset_cron"));
                    c.setSinglePlayerFind(rs.getBoolean("single_player_find"));

                    String defaultPreset = rs.getString("default_treasure_reward_preset_id");
                    if (defaultPreset != null && !defaultPreset.trim().isEmpty()) {
                        try {
                            c.setDefaultTreasureRewardPresetId(UUID.fromString(defaultPreset));
                        } catch (IllegalArgumentException ignored) {
                            c.setDefaultTreasureRewardPresetId(null);
                        }
                    }
                    
                    String rewardsJson = rs.getString("rewards");
                    List<Reward> rewards = gson.fromJson(rewardsJson, REWARD_LIST_TYPE);
                    c.setCompletionRewards(rewards);
                    
                    // Load ACT rules for this collection
                    List<ActRule> actRules = loadActRules(conn, id);
                    c.setActRules(actRules);
                    
                    collections.add(c);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return collections;
        });
    }

    @Override
    public CompletableFuture<Void> saveCollection(Collection collection) {
        return runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                // Save collection
                try (PreparedStatement ps = conn.prepareStatement("REPLACE INTO ah_collections (id, name, enabled, progress_reset_cron, single_player_find, rewards, default_treasure_reward_preset_id) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, collection.getId().toString());
                    ps.setString(2, collection.getName());
                    ps.setBoolean(3, collection.isEnabled());
                    ps.setString(4, collection.getProgressResetCron());
                    ps.setBoolean(5, collection.isSinglePlayerFind());
                    ps.setString(6, gson.toJson(collection.getCompletionRewards()));
                    ps.setString(7, collection.getDefaultTreasureRewardPresetId() != null ? collection.getDefaultTreasureRewardPresetId().toString() : null);
                    ps.executeUpdate();
                }
                
                // Delete existing ACT rules for this collection
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ah_act_rules WHERE collection_id = ?")) {
                    ps.setString(1, collection.getId().toString());
                    ps.executeUpdate();
                }
                
                // Save ACT rules
                for (ActRule rule : collection.getActRules()) {
                    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO ah_act_rules (id, collection_id, name, date_range, duration, cron_expression, enabled) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                        ps.setString(1, rule.getId().toString());
                        ps.setString(2, rule.getCollectionId().toString());
                        ps.setString(3, rule.getName());
                        ps.setString(4, rule.getDateRange());
                        ps.setString(5, rule.getDuration());
                        ps.setString(6, rule.getCronExpression());
                        ps.setBoolean(7, rule.isEnabled());
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<List<RewardPreset>> loadRewardPresets(RewardPresetType type) {
        return supplyAsync(() -> {
            List<RewardPreset> presets = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM ah_reward_presets WHERE type = ?")) {
                ps.setString(1, type.name());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    String name = rs.getString("name");
                    String rewardsJson = rs.getString("rewards");
                    List<Reward> rewards = gson.fromJson(rewardsJson, REWARD_LIST_TYPE);
                    presets.add(new RewardPreset(id, type, name, rewards));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return presets;
        });
    }

    @Override
    public CompletableFuture<Void> saveRewardPreset(RewardPreset preset) {
        return runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("REPLACE INTO ah_reward_presets (id, type, name, rewards) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, preset.getId().toString());
                ps.setString(2, preset.getType().name());
                ps.setString(3, preset.getName());
                ps.setString(4, gson.toJson(preset.getRewards()));
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveRewardPresetsBatch(List<RewardPreset> presets) {
        return runAsync(() -> {
            if (presets == null || presets.isEmpty()) {
                return;
            }

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "REPLACE INTO ah_reward_presets (id, type, name, rewards) VALUES (?, ?, ?, ?)")) {

                boolean originalAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);
                try {
                    for (RewardPreset preset : presets) {
                        ps.setString(1, preset.getId().toString());
                        ps.setString(2, preset.getType().name());
                        ps.setString(3, preset.getName());
                        ps.setString(4, gson.toJson(preset.getRewards()));
                        ps.addBatch();
                    }

                    ps.executeBatch();
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(originalAutoCommit);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteRewardPreset(RewardPresetType type, UUID presetId) {
        return runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM ah_reward_presets WHERE id = ? AND type = ?")) {
                ps.setString(1, presetId.toString());
                ps.setString(2, type.name());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<List<PlacePreset>> loadPlacePresets() {
        return supplyAsync(() -> {
            List<PlacePreset> presets = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM ah_place_presets")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    String group = rs.getString("grp");
                    String name = rs.getString("name");
                    String item = rs.getString("item");
                    presets.add(new PlacePreset(id, group, name, item));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            presets.sort(Comparator
                    .comparing(PlacePreset::getGroup, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(PlacePreset::getName, String.CASE_INSENSITIVE_ORDER));
            return presets;
        });
    }

    @Override
    public CompletableFuture<Void> savePlacePreset(PlacePreset preset) {
        return runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "REPLACE INTO ah_place_presets (id, grp, name, item) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, preset.getId().toString());
                ps.setString(2, preset.getGroup());
                ps.setString(3, preset.getName());
                ps.setString(4, preset.getItemData());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deletePlacePreset(UUID presetId) {
        return runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM ah_place_presets WHERE id = ?")) {
                ps.setString(1, presetId.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Set<String>> loadPlacePresetGroups() {
        return supplyAsync(() -> {
            LinkedHashSet<String> groups = new LinkedHashSet<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT grp FROM ah_place_preset_groups");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String group = rs.getString("grp");
                    if (group != null && !group.trim().isEmpty()) {
                        groups.add(group.trim());
                    }
                }
            } catch (SQLException e) {
                // Table may not exist yet on older schemas; treat as no persisted groups.
            }
            return groups;
        });
    }

    @Override
    public CompletableFuture<Void> createPlacePresetGroup(String group) {
        return runAsync(() -> {
            if (group == null || group.trim().isEmpty()) {
                return;
            }
            String sql = useSqlite
                    ? "INSERT OR IGNORE INTO ah_place_preset_groups (grp) VALUES (?)"
                    : "INSERT IGNORE INTO ah_place_preset_groups (grp) VALUES (?)";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, group.trim());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> renamePlacePresetGroup(String oldGroup, String newGroup) {
        return runAsync(() -> {
            if (oldGroup == null || oldGroup.trim().isEmpty() || newGroup == null || newGroup.trim().isEmpty()) {
                return;
            }
            String oldTrimmed = oldGroup.trim();
            String newTrimmed = newGroup.trim();

            String insertSql = useSqlite
                    ? "INSERT OR IGNORE INTO ah_place_preset_groups (grp) VALUES (?)"
                    : "INSERT IGNORE INTO ah_place_preset_groups (grp) VALUES (?)";

            try (Connection conn = dataSource.getConnection()) {
                try {
                    conn.setAutoCommit(false);

                    // Ensure new group exists
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        ps.setString(1, newTrimmed);
                        ps.executeUpdate();
                    }

                    // Best-effort: update any remaining presets (manager usually handles this too)
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE ah_place_presets SET grp = ? WHERE LOWER(grp) = LOWER(?)")) {
                        ps.setString(1, newTrimmed);
                        ps.setString(2, oldTrimmed);
                        ps.executeUpdate();
                    }

                    // Remove old group
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ah_place_preset_groups WHERE LOWER(grp) = LOWER(?)")) {
                        ps.setString(1, oldTrimmed);
                        ps.executeUpdate();
                    }

                    conn.commit();
                } catch (SQLException e) {
                    try {
                        conn.rollback();
                    } catch (SQLException ignored) {
                    }
                    throw e;
                } finally {
                    try {
                        conn.setAutoCommit(true);
                    } catch (SQLException ignored) {
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deletePlacePresetGroup(String group) {
        return runAsync(() -> {
            if (group == null || group.trim().isEmpty()) {
                return;
            }
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM ah_place_preset_groups WHERE LOWER(grp) = LOWER(?)")) {
                ps.setString(1, group.trim());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteCollection(UUID id) {
        return runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                // Delete collection
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ah_collections WHERE id = ?")) {
                    ps.setString(1, id.toString());
                    ps.executeUpdate();
                }
                // Delete associated treasures
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ah_treasures WHERE collection_id = ?")) {
                    ps.setString(1, id.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> renameCollection(UUID id, String newName) {
        return supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                // Check if new name exists
                try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM ah_collections WHERE name = ?")) {
                    ps.setString(1, newName);
                    if (ps.executeQuery().next()) return false;
                }

                try (PreparedStatement ps = conn.prepareStatement("UPDATE ah_collections SET name = ? WHERE id = ?")) {
                    ps.setString(1, newName);
                    ps.setString(2, id.toString());
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<List<PlayerData>> loadAllPlayerData() {
        return supplyAsync(() -> {
            List<PlayerData> allData = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT player_uuid, treasure_id FROM ah_player_found")) {
                ResultSet rs = ps.executeQuery();
                Map<UUID, PlayerData> map = new HashMap<>();
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    UUID treasureId = UUID.fromString(rs.getString("treasure_id"));
                    map.computeIfAbsent(uuid, PlayerData::new).addFoundTreasure(treasureId);
                }
                allData.addAll(map.values());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return allData;
        });
    }

    @Override
    public CompletableFuture<List<UUID>> getAllPlayerUUIDs() {
        return supplyAsync(() -> {
            List<UUID> uuids = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 ResultSet rs = conn.createStatement().executeQuery("SELECT uuid FROM ah_players")) {
                while (rs.next()) {
                    uuids.add(UUID.fromString(rs.getString("uuid")));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return uuids;
        });
    }

    @Override
    public CompletableFuture<Integer> resetAllProgress() {
        return supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM ah_player_found")) {
                return ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                return 0;
            }
        });
    }

    @Override
    public CompletableFuture<Integer> resetCollectionProgress(UUID collectionId) {
        return supplyAsync(() -> {
            String sql = "DELETE FROM ah_player_found WHERE treasure_id IN (SELECT id FROM ah_treasures WHERE collection_id = ?)";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, collectionId.toString());
                return ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                return 0;
            }
        });
    }

    @Override
    public CompletableFuture<Integer> resetPlayerProgress(UUID playerUuid) {
        return supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM ah_player_found WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                return ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                return 0;
            }
        });
    }

    @Override
    public CompletableFuture<Integer> resetPlayerCollectionProgress(UUID playerUuid, UUID collectionId) {
        return supplyAsync(() -> {
            String sql = "DELETE FROM ah_player_found WHERE player_uuid = ? AND treasure_id IN (SELECT id FROM ah_treasures WHERE collection_id = ?)";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, collectionId.toString());
                return ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                return 0;
            }
        });
    }

    @Override
    public CompletableFuture<Map<UUID, Integer>> getLeaderboard(UUID collectionId, int limit) {
        return supplyAsync(() -> {
            Map<UUID, Integer> leaderboard = new LinkedHashMap<>();
            String sql = "SELECT player_uuid, COUNT(*) as count FROM ah_player_found " +
                    "JOIN ah_treasures ON ah_player_found.treasure_id = ah_treasures.id " +
                    "WHERE ah_treasures.collection_id = ? " +
                    "GROUP BY player_uuid ORDER BY count DESC LIMIT ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, collectionId.toString());
                ps.setInt(2, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    leaderboard.put(UUID.fromString(rs.getString("player_uuid")), rs.getInt("count"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return leaderboard;
        });
    }

    @Override
    public CompletableFuture<List<UUID>> getPlayersWhoFound(UUID treasureId) {
        return supplyAsync(() -> {
            List<UUID> players = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT player_uuid FROM ah_player_found WHERE treasure_id = ?")) {
                ps.setString(1, treasureId.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    players.add(UUID.fromString(rs.getString("player_uuid")));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return players;
        });
    }

    @Override
    public CompletableFuture<Integer> getFoundPlayerCount(UUID treasureId) {
        return supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS cnt FROM ah_player_found WHERE treasure_id = ?")) {
                ps.setString(1, treasureId.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        });
    }

    @Override
    public CompletableFuture<Set<UUID>> getPlayerFoundInCollection(UUID playerUuid, UUID collectionId) {
        return supplyAsync(() -> {
            Set<UUID> treasures = new HashSet<>();
            String sql = "SELECT pf.treasure_id FROM ah_player_found pf " +
                    "JOIN ah_treasures t ON pf.treasure_id = t.id " +
                    "WHERE pf.player_uuid = ? AND t.collection_id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, collectionId.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    treasures.add(UUID.fromString(rs.getString("treasure_id")));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return treasures;
        });
    }
}
