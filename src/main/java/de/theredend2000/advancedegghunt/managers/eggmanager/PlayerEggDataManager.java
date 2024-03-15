package de.theredend2000.advancedegghunt.managers.eggmanager;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.enums.DeletionTypes;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

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
    private HashMap<UUID, File> playerFiles;

    public PlayerEggDataManager() {
        plugin = Main.getInstance();
        dataFolder = plugin.getDataFolder();
        playerConfigs = new HashMap<>();
        playerFiles = new HashMap<>();
    }

    public void reload() {
        playerConfigs = new HashMap<>();
    }

    public void unloadPlayerData(UUID uuid) {
        if (!playerConfigs.containsKey(uuid)) {
            return;
        }
        playerConfigs.remove(uuid);
    }

    public void initPlayers() {
        List<UUID> savedPlayers = new ArrayList<>(plugin.getEggDataManager().savedPlayers());
        for(UUID uuid : savedPlayers)
            loadPlayerData(uuid);
    }

    private void loadPlayerData(UUID uuid) {
        FileConfiguration config = getPlayerData(uuid);
        if(!playerConfigs.containsKey(uuid))
            this.playerConfigs.put(uuid, config);
    }

    private File getFile(UUID uuid) {
        if(!playerFiles.containsKey(uuid))
            playerFiles.put(uuid, new File(this.dataFolder + "/playerdata/", uuid + ".yml"));
        return playerFiles.get(uuid);
    }

    public FileConfiguration getPlayerData(UUID uuid) {
        File playerFile = this.getFile(uuid);
        if(!playerConfigs.containsKey(uuid)) {
            this.playerConfigs.put(uuid, YamlConfiguration.loadConfiguration(playerFile));
        }
        return playerConfigs.get(uuid);
    }

    public void savePlayerData(UUID uuid, FileConfiguration config) {
        try {
            config.save(this.getFile(uuid));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void savePlayerCollection(UUID uuid, String collection) {
        FileConfiguration config = this.getPlayerData(uuid);
        config.set("SelectedSection", collection);
        this.savePlayerData(uuid, config);
    }

    public void createPlayerFile(UUID uuid) {
        FileConfiguration config = this.getPlayerData(uuid);
        File playerFile = this.getFile(uuid);
        if (!playerFile.exists()) {
            try {
                playerFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.playerConfigs.put(uuid, config);
        this.loadPlayerData(uuid);
        this.savePlayerData(uuid, config);
        this.setDeletionType(DeletionTypes.Noting, uuid);
        this.savePlayerCollection(uuid, "default");
    }

    public void setDeletionType(DeletionTypes deletionType, UUID uuid) {
        FileConfiguration config = this.getPlayerData(uuid);
        config.set("DeletionType", deletionType.name());
        this.savePlayerData(uuid, config);
    }

    public DeletionTypes getDeletionType(UUID uuid) {
        FileConfiguration config = this.getPlayerData(uuid);
        return DeletionTypes.valueOf(config.getString("DeletionType"));
    }

    public void setResetTimer(UUID uuid, String collection, String id) {
        FileConfiguration cfg = getPlayerData(uuid);
        int currentSeconds = Main.getInstance().getRequirementsManager().getOverallTime(collection);
        if (currentSeconds != 0) {
            long toSet = System.currentTimeMillis() + (long)currentSeconds * 1000L;
            cfg.set("FoundEggs." + collection + "." + id + ".ResetCooldown", toSet);

            try {
                cfg.save(this.getFile(uuid));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public long getResetTimer(UUID uuid, String collection, String id) {
        FileConfiguration cfg = getPlayerData(uuid);
        return !cfg.contains("FoundEggs." + collection + "." + id + ".ResetCooldown") ? System.currentTimeMillis() + 1000000L : cfg.getLong("FoundEggs." + collection + "." + id + ".ResetCooldown");
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
                for(UUID uuids : plugin.getEggDataManager().savedPlayers()){
                    FileConfiguration cfg = playerConfigs.get(uuids);
                    if(cfg == null) continue;
                    if(!cfg.contains("FoundEggs.")) return;
                    for(String collection : cfg.getConfigurationSection("FoundEggs.").getKeys(false)) {
                        for(String eggId : cfg.getConfigurationSection("FoundEggs." + collection).getKeys(false)) {
                            if (eggId.equals("Count") || eggId.equals("Name")) continue;
                            if (canReset(uuids, collection, eggId))
                                plugin.getEggManager().resetStatsPlayerEgg(uuids, collection, eggId);
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20, 20);
    }
}