package de.theredend2000.advancedhunt.mysql.sqldata;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.data.EggDataStorage;
import de.theredend2000.advancedhunt.mysql.Database;
import org.bukkit.Location;
import org.bukkit.block.Block;
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

    }

    @Override
    public void removeEgg(Player player, Block block, String collection) {

    }

    @Override
    public int getPlayerCount(UUID uuid, String collection) {
        return 0;
    }

    @Override
    public int getRandomNotFoundEgg(UUID uuid, String collection) {
        return 0;
    }

    @Override
    public boolean containsEgg(Block block) {
        return false;
    }

    @Override
    public String getEggID(Block block, String collection) {
        return "";
    }

    @Override
    public String getEggCollection(Block block) {
        return "";
    }

    @Override
    public Location getEggLocation(String eggID, String collection) {
        return null;
    }

    @Override
    public String getEggCollectionFromPlayerData(UUID uuid) {
        return "";
    }

    @Override
    public void saveFoundEggs(UUID uuid, Block block, String id, String collection) {

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
        return false;
    }

    @Override
    public int getMaxEggs(String collection) {
        return 0;
    }

    @Override
    public int getEggsFound(UUID uuid, String collection) {
        return 0;
    }

    @Override
    public void updateMaxEggs(String collection) {

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
