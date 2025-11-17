package de.theredend2000.advancedhunt.mysql.sqldata;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.data.EggDataStorage;
import de.theredend2000.advancedhunt.mysql.Database;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class EggManagerSQL implements EggDataStorage {

    private Main plugin;

    private Database database;

    public EggManagerSQL(){
        this.plugin = Main.getInstance();
        this.database = plugin.getDatabase();
    }


    @Override
    public void saveEgg(Player player, Location location, String collection) {
        if (location == null || collection == null) return;

        try {
            // --- 1) Sicherstellen, dass die Collection existiert ---
            String checkCollection = "SELECT 1 FROM collections WHERE collection_id = ?";
            PreparedStatement stCheck = database.getConnection().prepareStatement(checkCollection);
            stCheck.setString(1, collection);
            ResultSet rsCheck = stCheck.executeQuery();
            if (!rsCheck.next()) {
                Bukkit.broadcastMessage("No collection found");
            }
            rsCheck.close();
            stCheck.close();

            // --- 2) Nächste egg_id berechnen ---
            String sqlMax = "SELECT MAX(CAST(egg_id AS UNSIGNED)) AS maxIndex FROM placed_eggs WHERE collection_id = ?";
            PreparedStatement stMax = database.getConnection().prepareStatement(sqlMax);
            stMax.setString(1, collection);
            ResultSet rs = stMax.executeQuery();
            int nextIndex = 0;
            if (rs.next()) {
                nextIndex = rs.getInt("maxIndex") + 1;
            }
            rs.close();
            stMax.close();

            // --- 3) Ei speichern ---
            String sql = """
            INSERT INTO placed_eggs
            (collection_id, egg_id, world, x, y, z, date, time)
            VALUES (?, ?, ?, ?, ?, ?, CURDATE(), CURTIME())
        """;
            PreparedStatement st = database.getConnection().prepareStatement(sql);
            st.setString(1, collection);
            st.setString(2, String.valueOf(nextIndex));
            st.setString(3, location.getWorld().getName());
            st.setInt(4, location.getBlockX());
            st.setInt(5, location.getBlockY());
            st.setInt(6, location.getBlockZ());
            st.executeUpdate();
            st.close();

            updateMaxEggs(collection);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    @Override
    public void removeEgg(Player player, Block block, String collection) {
        if (block == null || collection == null) return;

        try {
            // 1. ECHTE egg_id anhand der Position holen
            String sqlGet = """
        SELECT egg_id FROM placed_eggs
        WHERE collection_id = ? AND world = ? AND x = ? AND y = ? AND z = ?
        """;

            PreparedStatement stGet = database.getConnection().prepareStatement(sqlGet);
            stGet.setString(1, collection);
            stGet.setString(2, block.getWorld().getName());
            stGet.setInt(3, block.getX());
            stGet.setInt(4, block.getY());
            stGet.setInt(5, block.getZ());

            ResultSet rs = stGet.executeQuery();
            String eggId = null;
            if (rs.next()) eggId = rs.getString("egg_id");
            rs.close();
            stGet.close();

            if (eggId == null) return;

            // 2. Ei aus placed_eggs löschen
            String sql = """
        DELETE FROM placed_eggs
        WHERE collection_id = ? AND egg_id = ?
        """;
            PreparedStatement st = database.getConnection().prepareStatement(sql);
            st.setString(1, collection);
            st.setString(2, eggId);
            st.executeUpdate();
            st.close();

            // 3. Player-Einträge löschen
            String sqlFound = """
        DELETE FROM player_found_eggs
        WHERE collection_id = ? AND egg_index = ?
        """;
            PreparedStatement stFound = database.getConnection().prepareStatement(sqlFound);
            stFound.setString(1, collection);
            stFound.setString(2, eggId);
            stFound.executeUpdate();
            stFound.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    @Override
    public int getPlayerCount(UUID uuid, String collection) {
        if (uuid == null || collection == null) return 0;

        try {
            String sql = "SELECT COUNT(*) AS cnt FROM player_found_eggs WHERE uuid = ? AND collection_id = ?";
            PreparedStatement st = database.getConnection().prepareStatement(sql);
            st.setString(1, uuid.toString());
            st.setString(2, collection);
            ResultSet rs = st.executeQuery();
            int count = 0;
            if (rs.next()) count = rs.getInt("cnt");
            rs.close();
            st.close();
            return count;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }


    @Override
    public int getRandomNotFoundEgg(UUID uuid, String collection) {
        return 0;
    }

    @Override
    public boolean containsEgg(Block block) {
        if (block == null) return false;

        try {
            String sql = """
        SELECT 1 FROM placed_eggs
        WHERE world = ? AND x = ? AND y = ? AND z = ?
        LIMIT 1
        """;

            PreparedStatement st = database.getConnection().prepareStatement(sql);
            st.setString(1, block.getWorld().getName());
            st.setInt(2, block.getX());
            st.setInt(3, block.getY());
            st.setInt(4, block.getZ());

            ResultSet rs = st.executeQuery();
            boolean exists = rs.next();
            rs.close();
            st.close();
            return exists;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public String getEggID(Block block, String collection) {
        if (block == null || collection == null) return null;

        try {
            String sql = """
        SELECT egg_id FROM placed_eggs
        WHERE collection_id = ? AND world = ? AND x = ? AND y = ? AND z = ?
        LIMIT 1
        """;

            PreparedStatement st = database.getConnection().prepareStatement(sql);
            st.setString(1, collection);
            st.setString(2, block.getWorld().getName());
            st.setInt(3, block.getX());
            st.setInt(4, block.getY());
            st.setInt(5, block.getZ());

            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                String id = rs.getString("egg_id");
                rs.close();
                st.close();
                return id;
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public String getEggCollection(Block block) {
        if (block == null) return null;

        try {
            String sql = """
        SELECT collection_id FROM placed_eggs
        WHERE world = ? AND x = ? AND y = ? AND z = ?
        LIMIT 1
        """;

            PreparedStatement st = database.getConnection().prepareStatement(sql);
            st.setString(1, block.getWorld().getName());
            st.setInt(2, block.getX());
            st.setInt(3, block.getY());
            st.setInt(4, block.getZ());

            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                String col = rs.getString("collection_id");
                rs.close();
                st.close();
                return col;
            }
            rs.close();
            st.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public Location getEggLocation(String eggID, String collection) {
        return null;
    }

    @Override
    public String getEggCollectionFromPlayerData(UUID uuid){
        FileConfiguration config = plugin.getPlayerEggDataManager().getPlayerData(uuid);
        if(config.contains("SelectedSection")){
            return config.getString("SelectedSection");
        }
        return null;
    }

    @Override
    public void saveFoundEggs(UUID uuid, Block block, String id, String collection) {
        if (uuid == null || block == null || id == null || collection == null) return;

        try {
            String sql = """
            INSERT INTO player_found_eggs
            (uuid, collection_id, egg_index, world, x, y, z, date, time)
            VALUES (?, ?, ?, ?, ?, ?, ?, CURDATE(), CURTIME())
            ON DUPLICATE KEY UPDATE date = CURDATE(), time = CURTIME()
        """;
            PreparedStatement st = database.getConnection().prepareStatement(sql);
            st.setString(1, uuid.toString());
            st.setString(2, collection);
            st.setInt(3, Integer.parseInt(id));
            st.setString(4, block.getWorld().getName());
            st.setInt(5, block.getX());
            st.setInt(6, block.getY());
            st.setInt(7, block.getZ());
            st.executeUpdate();
            st.close();

            String updateTimes = """
            UPDATE placed_eggs SET date = CURDATE(), time = CURTIME()
            WHERE collection_id = ? AND egg_id = ?
        """;
            PreparedStatement st2 = database.getConnection().prepareStatement(updateTimes);
            st2.setString(1, collection);
            st2.setString(2, id);
            st2.executeUpdate();
            st2.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public int getTimesFound(String id, String collection) {
        return 0;
    }

    @Override
    public String getEggDatePlaced(String id, String collection) {
        return "";
    }

    @Override
    public String getEggTimePlaced(String id, String collection) {
        return "";
    }

    @Override
    public String getEggDateCollected(UUID uuid, String id, String collection) {
        return "";
    }

    @Override
    public String getEggTimeCollected(UUID uuid, String id, String collection) {
        return "";
    }

    @Override
    public boolean hasFound(UUID uuid, String id, String collection) {
        if (uuid == null || id == null || collection == null) return false;

        try {
            String sql = "SELECT 1 FROM player_found_eggs WHERE uuid = ? AND collection_id = ? AND egg_index = ? LIMIT 1";
            PreparedStatement st = database.getConnection().prepareStatement(sql);
            st.setString(1, uuid.toString());
            st.setString(2, collection);
            st.setInt(3, Integer.parseInt(id));
            ResultSet rs = st.executeQuery();
            boolean found = rs.next();
            rs.close();
            st.close();
            return found;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public int getMaxEggs(String collection) {
        try {
            String sql = """
        SELECT COUNT(*) AS cnt FROM placed_eggs WHERE collection_id = ?
        """;
            PreparedStatement st = database.getConnection().prepareStatement(sql);
            st.setString(1, collection);

            ResultSet rs = st.executeQuery();
            int count = 0;
            if (rs.next()) count = rs.getInt("cnt");
            rs.close();
            st.close();
            return count;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }


    @Override
    public int getEggsFound(UUID uuid, String collection) {
        return 0;
    }

    @Override
    public void updateMaxEggs(String collection) {
        try {
            // 1) Anzahl der Eier zählen
            String countSql = "SELECT COUNT(*) AS cnt FROM placed_eggs WHERE collection_id = ?";
            PreparedStatement stCount = database.getConnection().prepareStatement(countSql);
            stCount.setString(1, collection);

            ResultSet rs = stCount.executeQuery();
            int amount = 0;
            if (rs.next()) {
                amount = rs.getInt("cnt");
            }
            rs.close();
            stCount.close();

            // 2) Wert in der collections-Tabelle speichern
            String updateSql = """
            UPDATE collections
            SET max_eggs = ?
            WHERE collection_id = ?
        """;
            PreparedStatement stUpdate = database.getConnection().prepareStatement(updateSql);
            stUpdate.setInt(1, amount);
            stUpdate.setString(2, collection);
            stUpdate.executeUpdate();
            stUpdate.close();

            // 3) Debug oder console info (optional)
            Bukkit.getLogger().info("Updated MaxEggs for collection '" + collection + "' to " + amount);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean checkFoundAll(UUID uuid, String collection) {
        return false;
    }

    @Override
    public void markEggAsFound(String collection, String eggID, boolean marked) {

    }

    @Override
    public boolean isMarkedAsFound(String collection, String eggID) {
        return false;
    }

    @Override
    public void spawnEggParticle() {

    }

    @Override
    public void resetStatsPlayer(UUID uuid, String collection) {

    }

    @Override
    public void resetStatsPlayerEgg(UUID uuid, String collection, String id) {

    }

    @Override
    public boolean containsPlayer(String name) {
        return false;
    }

    @Override
    public void showAllEggs() {

    }

    @Override
    public String getLeaderboardPositionName(int position, UUID holder, String collection) {
        return "";
    }

    @Override
    public String getLeaderboardPositionCount(int position, UUID holder, String collection) {
        return "";
    }
}
