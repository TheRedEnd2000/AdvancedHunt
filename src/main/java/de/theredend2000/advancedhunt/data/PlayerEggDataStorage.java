package de.theredend2000.advancedhunt.data;

import de.theredend2000.advancedhunt.util.enums.DeletionTypes;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.UUID;

public interface PlayerEggDataStorage {

    void reload();

    void unloadPlayerData(UUID uuid);

    void initPlayers();

    FileConfiguration getPlayerData(UUID uuid);

    FileConfiguration getPlayerData(String uuid);

    void savePlayerData(UUID uuid, FileConfiguration config);

    void savePlayerCollection(UUID uuid, String collection);

    void createPlayerFile(UUID uuid);

    void setDeletionType(DeletionTypes deletionType, UUID uuid);

    DeletionTypes getDeletionType(UUID uuid);

    void setResetTimer(UUID uuid, String collection, String id);

    long getResetTimer(UUID uuid, String collection, String id);

    boolean canReset(UUID uuid, String collection, String id);
    void savePlayerData(UUID uuid, YamlConfiguration config);

    void checkReset();

    void resetAllPlayerData();
}
