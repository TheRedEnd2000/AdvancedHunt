package de.theredend2000.advancedHunt.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.theredend2000.advancedHunt.model.Collection;
import de.theredend2000.advancedHunt.model.PlayerData;
import de.theredend2000.advancedHunt.model.Reward;
import de.theredend2000.advancedHunt.model.Treasure;
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
import java.util.concurrent.CompletableFuture;

public class SqlRepository implements DataRepository {

    private static final Type REWARD_LIST_TYPE = new TypeToken<List<Reward>>(){}.getType();
    
    private HikariDataSource dataSource;
    private final JavaPlugin plugin;
    private final String host, database, username, password;
    private final int port;
    private final boolean useSqlite;
    private final Gson gson;

    public SqlRepository(JavaPlugin plugin, String host, int port, String database, String username, String password, boolean useSqlite) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.useSqlite = useSqlite;
        this.gson = new Gson();
    }

    @Override
    public void init() {
        HikariConfig config = new HikariConfig();
        if (useSqlite) {
            // Ensure the plugin data folder exists
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "database.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } else {
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            config.setUsername(username);
            config.setPassword(password);
        }
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);

        createTables();
        checkSchema();
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
                    "priority INT, " +
                    "FOREIGN KEY (collection_id) REFERENCES ah_collections(id) ON DELETE CASCADE)");

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
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "selected_collection_id VARCHAR(36))");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void checkSchema() {
        try (Connection conn = dataSource.getConnection()) {
            int currentVersion = 0;
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT version FROM ah_schema_version")) {
                if (rs.next()) {
                    currentVersion = rs.getInt("version");
                } else {
                    conn.createStatement().execute("INSERT INTO ah_schema_version (version) VALUES (0)");
                }
            }
            
            if (currentVersion < 1) {
                 conn.createStatement().execute("UPDATE ah_schema_version SET version = 1");
            }
            if (currentVersion < 2) {
                try {
                    conn.createStatement().execute("ALTER TABLE ah_treasures ADD COLUMN block_data TEXT");
                } catch (SQLException ignored) {} // Column might exist if created fresh
                conn.createStatement().execute("UPDATE ah_schema_version SET version = 2");
            }
            if (currentVersion < 3) {
                try {
                    conn.createStatement().execute("ALTER TABLE ah_treasures ADD COLUMN material VARCHAR(64)");
                    conn.createStatement().execute("ALTER TABLE ah_treasures ADD COLUMN block_state TEXT");
                } catch (SQLException ignored) {}
                conn.createStatement().execute("UPDATE ah_schema_version SET version = 3");
            }
            if (currentVersion < 4) {
                // Create performance indexes for leaderboard queries
                createPerformanceIndexes(conn);
                conn.createStatement().execute("UPDATE ah_schema_version SET version = 4");
            }
            if (currentVersion < 5) {
                // Add ACT rules support
                try {
                    conn.createStatement().execute("ALTER TABLE ah_collections ADD COLUMN progress_reset_cron VARCHAR(64)");
                } catch (SQLException ignored) {}
                // Create ACT rules table (already handled in createTables)
                conn.createStatement().execute("UPDATE ah_schema_version SET version = 5");
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
    private List<de.theredend2000.advancedHunt.model.ActRule> loadActRules(Connection conn, UUID collectionId) throws SQLException {
        List<de.theredend2000.advancedHunt.model.ActRule> rules = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM ah_act_rules WHERE collection_id = ? ORDER BY priority")) {
            ps.setString(1, collectionId.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID ruleId = UUID.fromString(rs.getString("id"));
                String name = rs.getString("name");
                de.theredend2000.advancedHunt.model.ActRule rule = new de.theredend2000.advancedHunt.model.ActRule(ruleId, collectionId, name);
                rule.setDateRange(rs.getString("date_range"));
                rule.setDuration(rs.getString("duration"));
                rule.setCronExpression(rs.getString("cron_expression"));
                rule.setEnabled(rs.getBoolean("enabled"));
                rule.setPriority(rs.getInt("priority"));
                rules.add(rule);
            }
        }
        return rules;
    }

    @Override
    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public CompletableFuture<PlayerData> loadPlayerData(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerData data = new PlayerData(playerUuid);
            try (Connection conn = dataSource.getConnection()) {
                // Load found treasures
                try (PreparedStatement ps = conn.prepareStatement("SELECT treasure_id FROM ah_player_found WHERE player_uuid = ?")) {
                    ps.setString(1, playerUuid.toString());
                    var rs = ps.executeQuery();
                    while (rs.next()) {
                        data.addFoundTreasure(UUID.fromString(rs.getString("treasure_id")));
                    }
                }
                // Load selected collection
                try (PreparedStatement ps = conn.prepareStatement("SELECT selected_collection_id FROM ah_players WHERE uuid = ?")) {
                    ps.setString(1, playerUuid.toString());
                    var rs = ps.executeQuery();
                    if (rs.next()) {
                        String idStr = rs.getString("selected_collection_id");
                        if (idStr != null) {
                            data.setSelectedCollectionId(UUID.fromString(idStr));
                        }
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
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                // Save selected collection
                try (PreparedStatement ps = conn.prepareStatement("REPLACE INTO ah_players (uuid, selected_collection_id) VALUES (?, ?)")) {
                    ps.setString(1, playerData.getPlayerUuid().toString());
                    ps.setString(2, playerData.getSelectedCollectionId() != null ? playerData.getSelectedCollectionId().toString() : null);
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
    public CompletableFuture<List<Treasure>> loadTreasures() {
        return CompletableFuture.supplyAsync(() -> {
            List<Treasure> treasures = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM ah_treasures")) {
                var rs = ps.executeQuery();
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
    public CompletableFuture<Void> saveTreasure(Treasure treasure) {
        return CompletableFuture.runAsync(() -> {
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
    public CompletableFuture<Void> deleteTreasure(UUID treasureId) {
        return CompletableFuture.runAsync(() -> {
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
    public CompletableFuture<Treasure> loadTreasure(UUID treasureId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM ah_treasures WHERE id = ?")) {
                ps.setString(1, treasureId.toString());
                var rs = ps.executeQuery();
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
    public CompletableFuture<List<Collection>> loadCollections() {
        return CompletableFuture.supplyAsync(() -> {
            List<Collection> collections = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM ah_collections")) {
                var rs = ps.executeQuery();
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    String name = rs.getString("name");
                    boolean enabled = rs.getBoolean("enabled");
                    Collection c = new Collection(id, name, enabled);
                    c.setProgressResetCron(rs.getString("progress_reset_cron"));
                    c.setSinglePlayerFind(rs.getBoolean("single_player_find"));
                    
                    String rewardsJson = rs.getString("rewards");
                    List<Reward> rewards = gson.fromJson(rewardsJson, REWARD_LIST_TYPE);
                    c.setCompletionRewards(rewards);
                    
                    // Load ACT rules for this collection
                    List<de.theredend2000.advancedHunt.model.ActRule> actRules = loadActRules(conn, id);
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
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                // Save collection
                try (PreparedStatement ps = conn.prepareStatement("REPLACE INTO ah_collections (id, name, enabled, progress_reset_cron, single_player_find, rewards) VALUES (?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, collection.getId().toString());
                    ps.setString(2, collection.getName());
                    ps.setBoolean(3, collection.isEnabled());
                    ps.setString(4, collection.getProgressResetCron());
                    ps.setBoolean(5, collection.isSinglePlayerFind());
                    ps.setString(6, gson.toJson(collection.getCompletionRewards()));
                    ps.executeUpdate();
                }
                
                // Delete existing ACT rules for this collection
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ah_act_rules WHERE collection_id = ?")) {
                    ps.setString(1, collection.getId().toString());
                    ps.executeUpdate();
                }
                
                // Save ACT rules
                for (de.theredend2000.advancedHunt.model.ActRule rule : collection.getActRules()) {
                    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO ah_act_rules (id, collection_id, name, date_range, duration, cron_expression, enabled, priority) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                        ps.setString(1, rule.getId().toString());
                        ps.setString(2, rule.getCollectionId().toString());
                        ps.setString(3, rule.getName());
                        ps.setString(4, rule.getDateRange());
                        ps.setString(5, rule.getDuration());
                        ps.setString(6, rule.getCronExpression());
                        ps.setBoolean(7, rule.isEnabled());
                        ps.setInt(8, rule.getPriority());
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteCollection(UUID id) {
        return CompletableFuture.runAsync(() -> {
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
        return CompletableFuture.supplyAsync(() -> {
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
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerData> allData = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT player_uuid, treasure_id FROM ah_player_found")) {
                var rs = ps.executeQuery();
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
    public CompletableFuture<Integer> resetAllProgress() {
        return CompletableFuture.supplyAsync(() -> {
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
        return CompletableFuture.supplyAsync(() -> {
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
        return CompletableFuture.supplyAsync(() -> {
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
        return CompletableFuture.supplyAsync(() -> {
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
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, Integer> leaderboard = new LinkedHashMap<>();
            String sql = "SELECT player_uuid, COUNT(*) as count FROM ah_player_found " +
                    "JOIN ah_treasures ON ah_player_found.treasure_id = ah_treasures.id " +
                    "WHERE ah_treasures.collection_id = ? " +
                    "GROUP BY player_uuid ORDER BY count DESC LIMIT ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, collectionId.toString());
                ps.setInt(2, limit);
                var rs = ps.executeQuery();
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
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> players = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT player_uuid FROM ah_player_found WHERE treasure_id = ?")) {
                ps.setString(1, treasureId.toString());
                var rs = ps.executeQuery();
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
    public CompletableFuture<Set<UUID>> getPlayerFoundInCollection(UUID playerUuid, UUID collectionId) {
        return CompletableFuture.supplyAsync(() -> {
            Set<UUID> treasures = new HashSet<>();
            String sql = "SELECT pf.treasure_id FROM ah_player_found pf " +
                    "JOIN ah_treasures t ON pf.treasure_id = t.id " +
                    "WHERE pf.player_uuid = ? AND t.collection_id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, collectionId.toString());
                var rs = ps.executeQuery();
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
