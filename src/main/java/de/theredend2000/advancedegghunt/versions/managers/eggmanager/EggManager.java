package de.theredend2000.advancedegghunt.versions.managers.eggmanager;

import org.bukkit.Location;
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
}
