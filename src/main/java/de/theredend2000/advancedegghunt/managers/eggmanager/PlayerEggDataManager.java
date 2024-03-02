package de.theredend2000.advancedegghunt.managers.eggmanager;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.enums.DeletionTypes;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PlayerEggDataManager {
    private Main plugin;
    private File dataFolder;
    private HashMap<UUID, FileConfiguration> playerConfigs;

    public PlayerEggDataManager() {
        plugin = Main.getInstance();
        dataFolder = plugin.getDataFolder();
        playerConfigs = new HashMap<>();
    }

    public void initPlayers() {
        List<UUID> savedPlayers = new ArrayList<>(plugin.getEggDataManager().savedPlayers());
        for(UUID uuid : savedPlayers)
            loadPlayerData(uuid);
    }

    private void loadPlayerData(UUID uuid) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(getFile(uuid));
        playerConfigs.put(uuid, config);
    }

    private File getFile(UUID uuid) {
        return new File(dataFolder + "/playerdata/", uuid + ".yml");
    }

    public FileConfiguration getPlayerData(UUID uuid) {
        return playerConfigs.get(uuid);
    }

    public void savePlayerData(UUID uuid,FileConfiguration config) {
        try {
            config.save(getFile(uuid));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void savePlayerSection(UUID uuid,String section){
        FileConfiguration config = getPlayerData(uuid);
        config.set("SelectedSection",section);
        savePlayerData(uuid,config);
    }

    public void createPlayerFile(UUID uuid) {
        FileConfiguration config = getPlayerData(uuid);
        File playerFile = getFile(uuid);

        if (!playerFile.exists()) {
            try {
                playerFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        loadPlayerData(uuid);
        savePlayerData(uuid,config);
        setDeletionType(DeletionTypes.Noting,uuid);
        savePlayerSection(uuid,"default");
    }

    public void setDeletionType(DeletionTypes deletionType,UUID uuid){
        FileConfiguration config = getPlayerData(uuid);
        config.set("DeletionType",deletionType.name());
        savePlayerData(uuid,config);
    }

    public DeletionTypes getDeletionType(UUID uuid){
        FileConfiguration config = getPlayerData(uuid);
        return DeletionTypes.valueOf(config.getString("DeletionType"));
    }

    public void setResetTimer(UUID uuid,String section,String id) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(getFile(uuid));
        int currentSeconds = Main.getInstance().getRequirementsManager().getOverallTime(section);
        if(currentSeconds != 0) {
            long toSet = System.currentTimeMillis() + (currentSeconds * 1000L);
            cfg.set("FoundEggs." + section + "." + id + ".ResetCooldown", toSet);
            try {
                cfg.save(getFile(uuid));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public long getResetTimer(UUID uuid, String section,String id) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(getFile(uuid));
        if(!cfg.contains("FoundEggs."+section+"."+id+".ResetCooldown")) return System.currentTimeMillis()+1000000;
        return cfg.getLong("FoundEggs."+section+"."+id+".ResetCooldown");
    }


    public boolean canReset(UUID uuid,String section,String id) {
        long current = System.currentTimeMillis();
        long millis = getResetTimer(uuid,section,id);
        return current > millis;
    }

    public void checkReset(){
        new BukkitRunnable() {
            @Override
            public void run() {
                for(UUID uuids : plugin.getEggDataManager().savedPlayers()){
                    FileConfiguration cfg = YamlConfiguration.loadConfiguration(getFile(uuids));
                    if(!cfg.contains("FoundEggs.")) return;
                    for(String sections : cfg.getConfigurationSection("FoundEggs.").getKeys(false)) {
                        for(String id : cfg.getConfigurationSection("FoundEggs."+sections).getKeys(false)) {
                            if (id.equals("Count") || id.equals("Name")) continue;
                            if (canReset(uuids, sections, id))
                                plugin.getEggManager().resetStatsPlayerEgg(uuids,sections,id);
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin,20,20);
    }
}