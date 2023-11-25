package de.theredend2000.advancedegghunt.versions.managers.eggmanager;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.versions.VersionManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface EggManager {

    public String getRandomEggTexture(int id);

    public ItemStack giveFinishedEggToPlayer(int id);
    public void finishEggPlacing(Player player);
    public void startEggPlacing(Player player);
    public void removeEgg(Player player, Block block);
    public void saveEgg(Player player, Location location);
    public boolean containsEgg(Block block);
    public String getEggID(Block block);
    public void saveFoundEggs(Player player, Block block, String id);
    public boolean hasFound(Player player, String id);
    public int getMaxEggs();
    public int getEggsFound(Player player);
    public void updateMaxEggs();
    public boolean checkFoundAll(Player player);
    public void spawnEggParticle();
    public Particle getParticle(Player p, String key);
    public int getTimesFound(String id);
    public int getRandomNotFoundEgg(Player player);
    public String getEggDatePlaced(String id);
    public String getEggTimePlaced(String id);
    public String getEggDateCollected(String uuid,String id);
    public String getEggTimeCollected(String uuid,String id);
    public void showAllEggs();
    public String getTopPlayerName();
    public int getTopPlayerEggsFound();
    public String getSecondPlayerName();
    public int getSecondPlayerEggsFound();
    public String getThirdPlayerName();
    public int getThirdPlayerEggsFound();
    public void resetStatsPlayer(String name);
    public void resetStatsAll();
    public boolean containsPlayer(String name);
}
