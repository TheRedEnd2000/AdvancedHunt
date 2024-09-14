package de.theredend2000.advancedegghunt.managers.eggmanager;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.configurations.PlayerEggConfig;
import de.theredend2000.advancedegghunt.util.enums.DeletionTypes;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerEggDataManager {
    private Main plugin;
    private PlayerEggConfig playerEggConfig;

    public PlayerEggDataManager() {
        plugin = Main.getInstance();
        playerEggConfig = new PlayerEggConfig(plugin);
    }

    public void reload() {
        playerEggConfig = new PlayerEggConfig(plugin);
    }

    public void unloadPlayerData(UUID uuid) {
        playerEggConfig.unloadConfig(uuid.toString());
    }

    public void initPlayers() {
        List<UUID> savedPlayers = new ArrayList<>(plugin.getEggDataManager().savedPlayers());
        for(UUID uuid : savedPlayers)
            playerEggConfig.getConfig(uuid.toString());
    }

    public FileConfiguration getPlayerData(UUID uuid) {
        return playerEggConfig.getConfig(uuid.toString());
    }

    public void savePlayerData(UUID uuid, FileConfiguration config) {
        playerEggConfig.saveConfig(uuid.toString());
    }

    public void savePlayerCollection(UUID uuid, String collection) {
        playerEggConfig.savePlayerCollection(uuid, collection);
    }

    public void createPlayerFile(UUID uuid) {
        playerEggConfig.createPlayerFile(uuid);
    }

    public void setDeletionType(DeletionTypes deletionType, UUID uuid) {
        playerEggConfig.setDeletionType(deletionType, uuid);
    }

    public DeletionTypes getDeletionType(UUID uuid) {
        return playerEggConfig.getDeletionType(uuid);
    }

    public void setResetTimer(UUID uuid, String collection, String id) {
        playerEggConfig.setResetTimer(uuid, collection, id);
    }

    public long getResetTimer(UUID uuid, String collection, String id) {
        return playerEggConfig.getResetTimer(uuid, collection, id);
    }

    public boolean canReset(UUID uuid, String collection, String id) {
        long current = System.currentTimeMillis();
        long millis = this.getResetTimer(uuid, collection, id);
        return current > millis;
    }

    public void checkReset(){
        new BukkitRunnable() {
            @Override
            public void run() {
                for(UUID uuid : plugin.getEggDataManager().savedPlayers()){
                    FileConfiguration cfg = getPlayerData(uuid);
                    if(cfg == null) continue;
                    if(!cfg.contains("FoundEggs.")) continue;
                    for(String collection : cfg.getConfigurationSection("FoundEggs.").getKeys(false)) {
                        for(String eggId : cfg.getConfigurationSection("FoundEggs." + collection).getKeys(false)) {
                            if (eggId.equals("Count") || eggId.equals("Name")) continue;
                            if (canReset(uuid, collection, eggId))
                                plugin.getEggManager().resetStatsPlayerEgg(uuid, collection, eggId);
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20, 20);
    }
}
