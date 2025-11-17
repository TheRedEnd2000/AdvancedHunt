package de.theredend2000.advancedhunt.mysql.sqldata;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.data.PlayerEggDataStorage;
import de.theredend2000.advancedhunt.util.DateTimeUtil;
import de.theredend2000.advancedhunt.util.enums.DeletionTypes;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.*;

public class PlayerEggDataManagerSQL implements PlayerEggDataStorage {

    private final Main plugin;

    public PlayerEggDataManagerSQL() {
        this.plugin = Main.getInstance();
    }

    private Connection getConn() {
        return plugin.getDatabase().getConnection();
    }

    @Override
    public void reload() {
        // optional: reconnect logic könnte hier stehen
        plugin.getDatabase().getConnection();
    }

    @Override
    public void unloadPlayerData(UUID uuid) {
        // SQL speichert zentral in DB — kein lokales Unload nötig
    }

    @Override
    public void initPlayers() {
        // optional: pre-load players if required (currently no-op)
    }

    // ------------------------------------------------------------
    // GET PLAYER DATA (like YML)
    // ------------------------------------------------------------
    @Override
    public YamlConfiguration getPlayerData(UUID uuid) {
        return getPlayerData(uuid.toString());
    }

    @Override
    public YamlConfiguration getPlayerData(String uuid) {
        try (PreparedStatement st = getConn().prepareStatement(
                "SELECT player_name, deletion_type, selected_section, config_version FROM player_data WHERE uuid = ?"
        )) {
            st.setString(1, uuid);
            ResultSet rs = st.executeQuery();

            YamlConfiguration config = new YamlConfiguration();

            if (rs.next()) {
                config.set("DeletionType", rs.getString("deletion_type"));
                config.set("SelectedSection", rs.getString("selected_section"));
                config.set("config-version", rs.getString("config_version"));
                config.set("FoundEggs", null); // ensure structure exists
                // set Name in FoundEggs.*.Name later when iterating eggs
            } else {
                // no player_data row -> still return empty config with defaults
                config.set("DeletionType", DeletionTypes.Everything.name());
                config.set("config-version", "1.1");
            }
            rs.close();

            // ----------------------------------------------------
            // GET FOUND EGGS FROM SQL and compute counts & name
            // ----------------------------------------------------
            try (PreparedStatement stEggs = getConn().prepareStatement(
                    "SELECT collection_id, egg_index, world, x, y, z, date, time FROM player_found_eggs WHERE uuid = ?"
            )) {
                stEggs.setString(1, uuid);
                ResultSet eggRS = stEggs.executeQuery();

                // map collection -> count
                Map<String, Integer> counts = new HashMap<>();
                while (eggRS.next()) {
                    String collection = eggRS.getString("collection_id");
                    int eggIndex = eggRS.getInt("egg_index");

                    String base = "FoundEggs." + collection + "." + eggIndex;

                    config.set(base + ".World", eggRS.getString("world"));
                    config.set(base + ".X", eggRS.getInt("x"));
                    config.set(base + ".Y", eggRS.getInt("y"));
                    config.set(base + ".Z", eggRS.getInt("z"));
                    config.set(base + ".Date", eggRS.getString("date"));
                    config.set(base + ".Time", eggRS.getString("time"));

                    counts.put(collection, counts.getOrDefault(collection, 0) + 1);
                }
                eggRS.close();

                // set Count and Name for each collection found
                for (Map.Entry<String, Integer> e : counts.entrySet()) {
                    String collection = e.getKey();
                    config.set("FoundEggs." + collection + ".Count", e.getValue());
                    // Name: read from player_data table if exists
                    try (PreparedStatement stName = getConn().prepareStatement(
                            "SELECT player_name FROM player_data WHERE uuid = ?"
                    )) {
                        stName.setString(1, uuid);
                        ResultSet rsName = stName.executeQuery();
                        if (rsName.next()) {
                            config.set("FoundEggs." + collection + ".Name", rsName.getString("player_name"));
                        } else {
                            config.set("FoundEggs." + collection + ".Name", Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName());
                        }
                        rsName.close();
                    }
                }
            }

            return config;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // ------------------------------------------------------------
    // SAVE PLAYER DATA (similar to YML save)
    // This writes the meta info (deletion type, selected section, version).
    // FoundEggs (individual found eggs) are expected to be handled by the
    // code that records found eggs (saveFoundEggs). We only upsert the player_data row here.
    // ------------------------------------------------------------
    @Override
    public void savePlayerData(UUID uuid, FileConfiguration config) {
        try (PreparedStatement st = getConn().prepareStatement(
                "REPLACE INTO player_data (uuid, player_name, deletion_type, selected_section, config_version) VALUES (?,?,?,?,?)"
        )) {
            st.setString(1, uuid.toString());
            st.setString(2, Bukkit.getOfflinePlayer(uuid).getName());
            st.setString(3, config.getString("DeletionType", "Everything"));
            st.setString(4, config.getString("SelectedSection", null));
            st.setString(5, config.getString("config-version", "1.1"));
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------
    // SAVE SELECTED COLLECTION
    // ------------------------------------------------------------
    @Override
    public void savePlayerCollection(UUID uuid, String collection) {
        try (PreparedStatement st = getConn().prepareStatement(
                "INSERT INTO player_data (uuid, player_name, selected_section) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE selected_section = VALUES(selected_section), player_name = VALUES(player_name)"
        )) {
            st.setString(1, uuid.toString());
            st.setString(2, Bukkit.getOfflinePlayer(uuid).getName());
            st.setString(3, collection);
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------
    // CREATE PLAYER FILE (SQL)
    // ------------------------------------------------------------
    @Override
    public void createPlayerFile(UUID uuid) {
        // Beim Erstellen: uuid, player_name, deletion_type und selected_section setzen.
        // Falls der Datensatz schon existiert, wird nur der player_name aktualisiert.
        final String sql = "INSERT INTO player_data (uuid, player_name, deletion_type, selected_section) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name)";

        try (PreparedStatement st = getConn().prepareStatement(sql)) {
            st.setString(1, uuid.toString());
            st.setString(2, Bukkit.getOfflinePlayer(uuid).getName());
            // Default-Werte analog zur YAML-Variante:
            st.setString(3, DeletionTypes.Noting.name()); // falls dein enum anders heißt, anpassen
            st.setString(4, "default");                   // Standard-Collection
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void setDeletionType(DeletionTypes deletionType, UUID uuid) {
        try (PreparedStatement st = getConn().prepareStatement(
                "INSERT INTO player_data (uuid, player_name, deletion_type) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE deletion_type = VALUES(deletion_type), player_name = VALUES(player_name)"
        )) {
            st.setString(1, uuid.toString());
            st.setString(2, Bukkit.getOfflinePlayer(uuid).getName());
            st.setString(3, deletionType.name());
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public DeletionTypes getDeletionType(UUID uuid) {
        try (PreparedStatement st = getConn().prepareStatement(
                "SELECT deletion_type FROM player_data WHERE uuid = ?"
        )) {
            st.setString(1, uuid.toString());
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                String val = rs.getString("deletion_type");
                if (val != null) {
                    return DeletionTypes.valueOf(val);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return DeletionTypes.Everything;
    }

    // ------------------------------------------------------------
    // RESET TIMER
    // ------------------------------------------------------------
    @Override
    public void setResetTimer(UUID uuid, String collection, String id) {
        try (PreparedStatement st = getConn().prepareStatement(
                "UPDATE player_found_eggs SET date = ?, time = ? WHERE uuid = ? AND collection_id = ? AND egg_index = ?"
        )) {
            st.setString(1, DateTimeUtil.getCurrentDateString());
            st.setString(2, DateTimeUtil.getCurrentDateTimeString());
            st.setString(3, uuid.toString());
            st.setString(4, collection);
            st.setInt(5, Integer.parseInt(id));
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public long getResetTimer(UUID uuid, String collection, String id) {
        try (PreparedStatement st = getConn().prepareStatement(
                "SELECT date, time FROM player_found_eggs WHERE uuid = ? AND collection_id = ? AND egg_index = ?"
        )) {
            st.setString(1, uuid.toString());
            st.setString(2, collection);
            st.setInt(3, Integer.parseInt(id));
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                String date = rs.getString("date");
                String time = rs.getString("time");
                if (date != null && time != null) {
                    return 0;//DateTimeUtil.getTimeFromSQL(date, time); // returns millis
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // if no timer found -> return big future so canReset == false
        return Long.MAX_VALUE;
    }

    @Override
    public boolean canReset(UUID uuid, String collection, String id) {
        long current = System.currentTimeMillis();
        long millis = getResetTimer(uuid, collection, id);
        return current > millis;
    }

    @Override
    public void savePlayerData(UUID uuid, YamlConfiguration config) {

    }

    // ------------------------------------------------------------
    // Runs periodically and checks reset timers for all players' found eggs.
    // Equivalent to YAML version which iterated files.
    // ------------------------------------------------------------
    @Override
    public void checkReset() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // iterate through all entries in player_found_eggs
                    try (PreparedStatement st = getConn().prepareStatement(
                            "SELECT uuid, collection_id, egg_index FROM player_found_eggs"
                    )) {
                        ResultSet rs = st.executeQuery();
                        while (rs.next()) {
                            String uuidStr = rs.getString("uuid");
                            String collection = rs.getString("collection_id");
                            int eggIndex = rs.getInt("egg_index");
                            UUID uuid;
                            try {
                                uuid = UUID.fromString(uuidStr);
                            } catch (IllegalArgumentException ex) {
                                continue;
                            }
                            if (canReset(uuid, collection, String.valueOf(eggIndex))) {
                                plugin.getEggManager().resetStatsPlayerEgg(uuid, collection, String.valueOf(eggIndex));
                            }
                        }
                        rs.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L);
    }

    // ------------------------------------------------------------
    // RESET ALL PLAYER DATA
    // ------------------------------------------------------------
    @Override
    public void resetAllPlayerData() {
        try (Statement st = getConn().createStatement()) {
            st.executeUpdate("DELETE FROM player_found_eggs");
            st.executeUpdate("UPDATE player_data SET selected_section = NULL");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
