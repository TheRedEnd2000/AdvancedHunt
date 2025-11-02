package de.theredend2000.advancedhunt.data;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EggDataStorage {

    // Egg Data
    void saveEgg(Player player, Location location, String collection);
    void removeEgg(Player player, Block block, String collection);
    int getPlayerCount(UUID uuid, String collection);
    int getRandomNotFoundEgg(UUID uuid, String collection);
    boolean containsEgg(Block block);
    String getEggID(Block block, String collection);
    String getEggCollection(Block block);
    Location getEggLocation(String eggID, String collection);
    String getEggCollectionFromPlayerData(UUID uuid);
    void saveFoundEggs(UUID uuid, Block block, String id, String collection);
    int getTimesFound(String id, String collection);
    String getEggDatePlaced(String id, String collection);
    String getEggTimePlaced(String id, String collection);
    String getEggDateCollected(UUID uuid, String id, String collection);
    String getEggTimeCollected(UUID uuid, String id, String collection);
    boolean hasFound(UUID uuid, String id, String collection);
    int getMaxEggs(String collection);
    int getEggsFound(UUID uuid, String collection);
    void updateMaxEggs(String collection);
    boolean checkFoundAll(UUID uuid, String collection);
    void markEggAsFound(String collection, String eggID, boolean marked);
    boolean isMarkedAsFound(String collection, String eggID);
    void spawnEggParticle();

    //Player Stuff
    void resetStatsPlayer(UUID uuid, String collection);
    void resetStatsPlayerEgg(UUID uuid, String collection, String id);
    boolean containsPlayer(String name);
    void showAllEggs();

    //Leaderboard
    String getLeaderboardPositionName(int position, UUID holder, String collection);
    String getLeaderboardPositionCount(int position, UUID holder, String collection);
}
