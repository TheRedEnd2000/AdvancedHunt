package de.theredend2000.advancedhunt.mysql;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.configurations.MySQLConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private Connection connection;

    private Main plugin;
    private String host;
    private int port;
    private String database;
    private String user;
    private String password;
    private MySQLConfig mySQLConfig;
    private boolean isEnabled;

    public Database() {
        this.plugin = Main.getInstance();
        this.mySQLConfig = plugin.getMySQLConfig();
        this.isEnabled = mySQLConfig.isEnabled();

        this.host = mySQLConfig.getHost();
        this.port = mySQLConfig.getPort();
        this.database = mySQLConfig.getDatabase();
        this.user = mySQLConfig.getUser();
        this.password = mySQLConfig.getPassword();
    }

    public Connection getConnection() {

        if (connection != null) {
            return connection;
        }

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=false&autoReconnect=true&characterEncoding=utf8";

        try {
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to database.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return connection;
    }

    public void initializeDatabase() throws SQLException {
        Statement statement = getConnection().createStatement();

        //------------------------------------------------------------
        // 1) COLLECTIONS (BASIS)
        //------------------------------------------------------------
        statement.execute(
                "CREATE TABLE IF NOT EXISTS collections (" +
                        "collection_id VARCHAR(255) PRIMARY KEY, " +
                        "max_eggs INT DEFAULT 0, " +
                        "requirements_order VARCHAR(4) DEFAULT 'OR', " +
                        "enabled BOOLEAN DEFAULT TRUE)"
        );

        //------------------------------------------------------------
        // 2) COLLECTION REQUIREMENTS
        //------------------------------------------------------------
        statement.execute(
                "CREATE TABLE IF NOT EXISTS collection_requirements (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "collection_id VARCHAR(255) NOT NULL, " +
                        "type VARCHAR(50) NOT NULL, " +          // Hours, Month, Year, Weekday...
                        "requirement_key VARCHAR(255) NOT NULL, " +
                        "value BOOLEAN DEFAULT TRUE, " +
                        "FOREIGN KEY (collection_id) REFERENCES collections(collection_id) ON DELETE CASCADE)"
        );

        //------------------------------------------------------------
        // 3) COLLECTION RESET
        //------------------------------------------------------------
        statement.execute(
                "CREATE TABLE IF NOT EXISTS collection_reset (" +
                        "collection_id VARCHAR(255) PRIMARY KEY, " +
                        "reset_year INT, " +
                        "reset_month INT, " +
                        "reset_day INT, " +
                        "reset_hour INT, " +
                        "reset_minute INT, " +
                        "reset_second INT, " +
                        "FOREIGN KEY (collection_id) REFERENCES collections(collection_id) ON DELETE CASCADE)"
        );

        //------------------------------------------------------------
        // 4) PLACED EGGS (ALLE GESETZTEN EIER)
        //------------------------------------------------------------
        statement.execute(
                "CREATE TABLE IF NOT EXISTS placed_eggs (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "collection_id VARCHAR(255) NOT NULL, " +
                        "egg_id VARCHAR(255) NOT NULL, " +
                        "world VARCHAR(255), " +
                        "x INT, y INT, z INT, " +
                        "date VARCHAR(20), " +
                        "time VARCHAR(20), " +
                        "FOREIGN KEY (collection_id) REFERENCES collections(collection_id) ON DELETE CASCADE)"
        );

        //------------------------------------------------------------
        // 5) EGG REWARDS (REWARDS PRO EI)
        //------------------------------------------------------------
        statement.execute(
                "CREATE TABLE IF NOT EXISTS egg_rewards (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "collection_id VARCHAR(255) NOT NULL, " +
                        "egg_id VARCHAR(255) NOT NULL, " +
                        "reward_index INT, " +
                        "command TEXT, " +
                        "enabled BOOLEAN DEFAULT TRUE, " +
                        "chance DOUBLE DEFAULT 100, " +
                        "FOREIGN KEY (collection_id) REFERENCES collections(collection_id) ON DELETE CASCADE)"
        );

        //------------------------------------------------------------
        // 6) GLOBAL REWARDS (FÜR DIE GESAMTE COLLECTION)
        //------------------------------------------------------------
        statement.execute(
                "CREATE TABLE IF NOT EXISTS global_rewards (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "collection_id VARCHAR(255) NOT NULL, " +
                        "reward_index INT, " +
                        "command TEXT, " +
                        "enabled BOOLEAN DEFAULT TRUE, " +
                        "chance DOUBLE DEFAULT 100, " +
                        "FOREIGN KEY (collection_id) REFERENCES collections(collection_id) ON DELETE CASCADE)"
        );

        //------------------------------------------------------------
        // 7) PLAYER DATA (YML → SQL ERSETZT)
        //------------------------------------------------------------
        statement.execute(
                "CREATE TABLE IF NOT EXISTS player_data (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "player_name VARCHAR(255), " +
                        "deletion_type VARCHAR(50) DEFAULT 'Everything', " +
                        "selected_section VARCHAR(255), " +
                        "config_version VARCHAR(10) DEFAULT '1.1')"
        );

        //------------------------------------------------------------
        // 8) PLAYER FOUND EGGS ("FoundEggs" aus deiner YML)
        //------------------------------------------------------------
        statement.execute(
                "CREATE TABLE IF NOT EXISTS player_found_eggs (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "uuid VARCHAR(36) NOT NULL, " +
                        "collection_id VARCHAR(255) NOT NULL, " +
                        "egg_id INT NOT NULL, " +
                        "world VARCHAR(255), " +
                        "x INT, y INT, z INT, " +
                        "date VARCHAR(20), " +
                        "time VARCHAR(20), " +
                        "FOREIGN KEY (uuid) REFERENCES player_data(uuid) ON DELETE CASCADE)"
        );

        statement.close();
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}
