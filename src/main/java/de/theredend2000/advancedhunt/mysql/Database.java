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

    public Database(){
        this.plugin = Main.getInstance();
        this.mySQLConfig = plugin.getMySQLConfig();
        this.isEnabled = mySQLConfig.isEnabled();

        this.host = mySQLConfig.getHost();
        this.port = mySQLConfig.getPort();
        this.database = mySQLConfig.getDatabase();
        this.user = mySQLConfig.getUser();
        this.password = mySQLConfig.getPassword();
    }

    public Connection getConnection(){

        if(connection != null){
            return connection;
        }

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";

        Connection connection;
        try {
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        this.connection = connection;

        System.out.println("Connected to database.");

        return connection;
    }

    public void initializeDatabase() throws SQLException {
        Statement statement = getConnection().createStatement();

        // Collection
        statement.execute(
                "CREATE TABLE IF NOT EXISTS collections (" +
                        "id VARCHAR(255) PRIMARY KEY, " +
                        "max_eggs INT DEFAULT 0, " +
                        "requirements_order VARCHAR(4) DEFAULT OR, " +
                        "enabled BOOLEAN DEFAULT TRUE)"
        );

        // Player
        statement.execute(
                "CREATE TABLE IF NOT EXISTS player_eggs (" +
                        "uuid VARCHAR(36) NOT NULL, " +
                        "collection VARCHAR(255) NOT NULL, " +
                        "count INT DEFAULT 0, " +
                        "player_name VARCHAR(255), " +
                        "PRIMARY KEY (uuid, collection))"
        );

        // Found Eggs
        statement.execute(
                "CREATE TABLE IF NOT EXISTS found_eggs (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "uuid VARCHAR(36) NOT NULL, " +
                        "collection VARCHAR(255) NOT NULL, " +
                        "egg_id VARCHAR(255) NOT NULL, " +
                        "world VARCHAR(255), " +
                        "x INT, " +
                        "y INT, " +
                        "z INT, " +
                        "found_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "UNIQUE KEY unique_find (uuid, collection, egg_id))"
        );

        // Placed Eggs
        statement.execute(
                "CREATE TABLE IF NOT EXISTS placed_eggs (" +
                        "collection VARCHAR(255) NOT NULL, " +
                        "egg_id VARCHAR(255) NOT NULL, " +
                        "world VARCHAR(255), " +
                        "x INT, " +
                        "y INT, " +
                        "z INT, " +
                        "times_found INT DEFAULT 0, " +
                        "marked_as_found BOOLEAN DEFAULT FALSE, " +
                        "placed_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "PRIMARY KEY (collection, egg_id))"
        );

        statement.close();
    }

    //Stuff from other plugin to maybe change up

    /*public PlayerStats findPlayerStatsByUUID(String uuid) throws SQLException {

        PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM player_stats WHERE uuid = ?");
        statement.setString(1, uuid);

        ResultSet resultSet = statement.executeQuery();

        PlayerStats playerStats;

        if(resultSet.next()){

            playerStats = new PlayerStats(resultSet.getString("uuid"), resultSet.getInt("deaths"), resultSet.getInt("kills"), resultSet.getLong("blocks_broken"), resultSet.getDouble("balance"), resultSet.getDate("last_login"), resultSet.getDate("last_logout"));

            statement.close();

            return playerStats;
        }

        statement.close();

        return null;
    }

    public void createPlayerStats(PlayerStats playerStats) throws SQLException {

        PreparedStatement statement = getConnection()
                .prepareStatement("INSERT INTO player_stats(uuid, deaths, kills, blocks_broken, balance, last_login, last_logout) VALUES (?, ?, ?, ?, ?, ?, ?)");
        statement.setString(1, playerStats.getPlayerUUID());
        statement.setInt(2, playerStats.getDeaths());
        statement.setInt(3, playerStats.getKills());
        statement.setLong(4, playerStats.getBlocksBroken());
        statement.setDouble(5, playerStats.getBalance());
        statement.setDate(6, new Date(playerStats.getLastLogin().getTime()));
        statement.setDate(7, new Date(playerStats.getLastLogout().getTime()));

        statement.executeUpdate();

        statement.close();

    }

    public void updatePlayerStats(PlayerStats playerStats) throws SQLException {

        PreparedStatement statement = getConnection().prepareStatement("UPDATE player_stats SET deaths = ?, kills = ?, blocks_broken = ?, balance = ?, last_login = ?, last_logout = ? WHERE uuid = ?");
        statement.setInt(1, playerStats.getDeaths());
        statement.setInt(2, playerStats.getKills());
        statement.setLong(3, playerStats.getBlocksBroken());
        statement.setDouble(4, playerStats.getBalance());
        statement.setDate(5, new Date(playerStats.getLastLogin().getTime()));
        statement.setDate(6, new Date(playerStats.getLastLogout().getTime()));
        statement.setString(7, playerStats.getPlayerUUID());

        statement.executeUpdate();

        statement.close();

    }

    public void deletePlayerStats(PlayerStats playerStats) throws SQLException {

        PreparedStatement statement = getConnection().prepareStatement("DELETE FROM player_stats WHERE uuid = ?");
        statement.setString(1, playerStats.getPlayerUUID());

        statement.executeUpdate();

        statement.close();

    }*/

    public boolean isEnabled() {
        return isEnabled;
    }
}
