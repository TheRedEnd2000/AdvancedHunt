package de.theredend2000.advancedhunt.managers.eggmanager;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.configurations.PlayerEggConfig;
import de.theredend2000.advancedhunt.data.EggDataStorage;
import de.theredend2000.advancedhunt.data.PlayerEggDataStorage;
import de.theredend2000.advancedhunt.mysql.sqldata.EggManagerSQL;
import de.theredend2000.advancedhunt.mysql.sqldata.PlayerEggDataManagerSQL;
import de.theredend2000.advancedhunt.mysql.yamldata.EggManagerYAML;
import de.theredend2000.advancedhunt.mysql.yamldata.PlayerEggDataManagerYAML;
import de.theredend2000.advancedhunt.util.enums.DeletionTypes;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerEggDataManager {
    private Main plugin;
    private PlayerEggConfig playerEggConfig;
    private PlayerEggDataStorage dataStorage;

    public PlayerEggDataManager() {
        plugin = Main.getInstance();
        playerEggConfig = new PlayerEggConfig(plugin);

        if(plugin.getMySQLConfig().isEnabled()){
            dataStorage = new PlayerEggDataManagerSQL();
        }else
            dataStorage = new PlayerEggDataManagerYAML();
    }

    public void reload() {
        dataStorage.reload();
    }

    public void unloadPlayerData(UUID uuid) {
        dataStorage.unloadPlayerData(uuid);
    }

    public void initPlayers() {
        dataStorage.initPlayers();
    }

    public FileConfiguration getPlayerData(UUID uuid) {
        return dataStorage.getPlayerData(uuid);
    }

    public FileConfiguration getPlayerData(String uuid) {
        return dataStorage.getPlayerData(uuid);
    }

    public void savePlayerData(UUID uuid, FileConfiguration config) {
        dataStorage.savePlayerData(uuid,config);
    }

    public void savePlayerCollection(UUID uuid, String collection) {
        dataStorage.savePlayerCollection(uuid,collection);
    }

    public void createPlayerFile(UUID uuid) {
        dataStorage.createPlayerFile(uuid);
    }

    public void setDeletionType(DeletionTypes deletionType, UUID uuid) {
        dataStorage.setDeletionType(deletionType,uuid);
    }

    public DeletionTypes getDeletionType(UUID uuid) {
        return dataStorage.getDeletionType(uuid);
    }

    public void setResetTimer(UUID uuid, String collection, String id) {
        dataStorage.setResetTimer(uuid, collection, id);
    }

    public long getResetTimer(UUID uuid, String collection, String id) {
        return dataStorage.getResetTimer(uuid,collection,id);
    }

    public boolean canReset(UUID uuid, String collection, String id) {
        return dataStorage.canReset(uuid,collection,id);
    }

    public void checkReset(){
        dataStorage.checkReset();
    }

    /**
     * Resets all player data for all players.
     */
    public void resetAllPlayerData() {
        dataStorage.resetAllPlayerData();
    }
}
