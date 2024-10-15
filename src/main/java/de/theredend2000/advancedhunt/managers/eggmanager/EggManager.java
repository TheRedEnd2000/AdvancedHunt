package de.theredend2000.advancedhunt.managers.eggmanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.configurations.InventoryConfig;
import de.theredend2000.advancedhunt.util.ConfigLocationUtil;
import de.theredend2000.advancedhunt.util.ItemHelper;
import de.theredend2000.advancedhunt.util.VersionComparator;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageManager;
import de.tr7zw.nbtapi.NBT;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EggManager {

    private Main plugin;
    private MessageManager messageManager;
    private Particle eggNotFoundParticle;
    private Particle eggFoundParticle;
    private BukkitTask spawnEggParticleTask;

    public EggManager(){
        this.plugin = Main.getInstance();
        messageManager = Main.getInstance().getMessageManager();

        eggNotFoundParticle = Main.getInstance().getPluginConfig().getEggNotFoundParticle();
        eggFoundParticle = Main.getInstance().getPluginConfig().getEggFoundParticle();
    }


    public String getRandomEggTexture(int id){
        String texture = "";
        switch (id){
            case 0:
                texture = Main.getTexture("ZWNlZGRjMjNmOWQ5NmJhYWEwZDJkN2I5ZWMxODBjZDdiZWE1NDQ3ZDM5YzQyNWNhOWU0NGQ4ODA4ZWExMWVhMCJ9fX0=");
                break;
            case 1:
                texture = Main.getTexture("ODYyMWE1MjY5ODY5ODQ3NTMxMDE1NjYzMDBhMzU2YjVmMzBkNjk3NWExZWZlNjI5YWJmMjY5NDc2NWQ5NmNjIn19fQ==");
                break;
            case 2:
                texture = Main.getTexture("NmUzMmE3ZGU3YTY3MmNjNjhmYTdhMjcyYmFhNmE4OWViZDQ0MGMzMmRjZjQ0ZTc3MDU3MDY4OTg5MDQyZjdjNiJ9fX0=");
                break;
            case 3:
                texture = Main.getTexture("NmI3NDQ2NTUwZjBmOTU3NmI3MzE3MjhiNWNiZWIyYmNlYTI1ZmQxYTU1NjBhMTdiMjM1N2U2MTZmYmM2NTYyMSJ9fX0=");
                break;
            case 4:
                texture = Main.getTexture("ZmU2ZmFiMDkxZTQ5NmMwOTY5MTA0ODBkYTBkODVlZTkxOWJjNDlhYTMxNzc1Y2FkYmJmNTA1ZWY0MTFiNWY5NCJ9fX0=");
                break;
            case 5:
                texture = Main.getTexture("ODUzMWNjMjY5YzhlNDcwNmU4OTJmOGEwZmIzNTFiMTA5MDE1NmIzZjYyNjFkODE2MzVkMDdhY2FkYmU2Y2UwZSJ9fX0=");
                break;
            case 6:
                texture = Main.getTexture("YTZhNjA1MWY3ZjZmNDM5ZDhmMjE0YzIzNGU4ZTJjNDc3NjMwMDUyNDMyZTQyNjA3ZjA0MDRiODQwYjUzY2VhYiJ9fX0=");
                break;
            case 7:
                texture = Main.getTexture("MjEzYjJlMjhlMDM3MTAwNzM5MTMwYjJlYjkwZWY0OTFjNmMzZGZmNWRlNTYxNWQyZjZkZmQxZTQ2YzljMmY3YyJ9fX0=");
                break;
        }
        return texture;
    }

    public void finishEggPlacing(Player player){
        InventoryConfig cfg = new InventoryConfig(Main.getInstance(), player.getUniqueId());
        ItemStack[][] items = cfg.getInventory();
        player.getInventory().clear();
        player.getInventory().setContents(items[0]);
        player.getInventory().setArmorContents(items[1]);
    }

    public void startEggPlacing(Player player){
        InventoryConfig cfg = new InventoryConfig(Main.getInstance(), player.getUniqueId());
        cfg.setInventory(player.getInventory());
        cfg.saveData();
        player.getInventory().clear();
    }

    public void saveEgg(Player player, Location location, String collection){
        if(plugin.getEggDataManager().getPlacedEggs(collection).contains("PlacedEggs.")){
            ConfigurationSection placedEggs = plugin.getEggDataManager().getPlacedEggs(collection).getConfigurationSection("PlacedEggs.");
            int nextNumber = 0;
            Set<String> eggIds = placedEggs.getKeys(false);
            if (!eggIds.isEmpty()) {
                for (int i = 0; i <= eggIds.size(); i++) {
                    String key = Integer.toString(i);
                    if (!eggIds.contains(key)) {
                        nextNumber = i;
                        break;
                    }
                }
            }
            new ConfigLocationUtil(plugin, location, "PlacedEggs." + nextNumber).saveBlockLocation(collection);
            player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.EGG_PLACED).replaceAll("%ID%", String.valueOf(nextNumber)));
            plugin.getIndividualPresetDataManager().loadPresetIntoEggCommands(plugin.getPluginConfig().getDefaultIndividualLoadingPreset(), collection, String.valueOf(nextNumber));
        }else {
            new ConfigLocationUtil(plugin, location, "PlacedEggs.0").saveBlockLocation(collection);
            player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.EGG_PLACED).replaceAll("%ID%", "0"));
            plugin.getIndividualPresetDataManager().loadPresetIntoEggCommands(plugin.getPluginConfig().getDefaultIndividualLoadingPreset(), collection, "0");
        }
        updateMaxEggs(collection);
    }

    public void removeEgg(Player player, Block block, String collection){
        FileConfiguration config = plugin.getEggDataManager().getPlacedEggs(collection);
        if(config.contains("PlacedEggs.")){
            Set<String> keys = new HashSet<>();
            keys.clear();
            for (String key : config.getConfigurationSection("PlacedEggs.").getKeys(false)) {
                keys.add(key);
                ConfigLocationUtil location = new ConfigLocationUtil(plugin, "PlacedEggs." + key);
                if (location.loadLocation(collection) != null) {
                    if (block.getX() == location.loadLocation(collection).getBlockX() && block.getY() == location.loadLocation(collection).getBlockY() && block.getZ() == location.loadLocation(collection).getBlockZ()) {
                        config.set("PlacedEggs." + key, null);
                        Main.getInstance().getEggDataManager().savePlacedEggs(collection);
                        player.sendMessage(messageManager.getMessage(MessageKey.EGG_BROKEN).replaceAll("%ID%", key));
                        keys.remove(key);
                    }
                }
            }
            for (UUID uuids : plugin.getEggDataManager().savedPlayers()) {
                FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuids);
                if(playerConfig.contains("Found." + collection)) {
                    for (String eggID : playerConfig.getConfigurationSection("Found." + collection).getKeys(false)) {
                        if(eggID.equalsIgnoreCase("Count") || eggID.equalsIgnoreCase("Name")) continue;
                        ConfigLocationUtil location = new ConfigLocationUtil(plugin, "Found." + collection + "." + eggID);
                        if (location.loadLocation(uuids) != null) {
                            if (block.getX() == location.loadLocation(uuids).getBlockX() && block.getY() == location.loadLocation(uuids).getBlockY() && block.getZ() == location.loadLocation(uuids).getBlockZ()) {
                                playerConfig.set("Found." + collection + "." + eggID, null);
                                playerConfig.set("Found." + collection + ".Count", getPlayerCount(uuids, collection) - 1);
                                plugin.getPlayerEggDataManager().savePlayerData(uuids, playerConfig);
                            }
                        }
                    }
                }
            }
            if(keys.isEmpty()){
                plugin.getEggDataManager().getPlacedEggs(collection).set("PlacedEggs", null);
                plugin.getEggDataManager().savePlacedEggs(collection);
            }
            updateMaxEggs(collection);
        }
    }

    public int getPlayerCount(UUID uuid, String collection){
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuid);
        return playerConfig.getInt("Found." + collection + ".Count");
    }

    public int getRandomNotFoundEgg(Player player, String collection){
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(player.getUniqueId());
        if(plugin.getEggDataManager().getPlacedEggs(collection).contains("PlacedEggs.") && playerConfig.contains("Found.")) {
            for (int i = 0; i < getMaxEggs(collection); i++) {
                if (!hasFound(player, String.valueOf(i), collection)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean containsEgg(Block block){
        for(String collection : plugin.getEggDataManager().savedEggCollections()) {
            if (!plugin.getEggDataManager().getPlacedEggs(collection).contains("PlacedEggs.")) continue;
            for (String key : plugin.getEggDataManager().getPlacedEggs(collection).getConfigurationSection("PlacedEggs.").getKeys(false)) {
                ConfigLocationUtil location = new ConfigLocationUtil(plugin, "PlacedEggs." + key + ".");
                if (location.loadLocation(collection) != null) {
                    if (block.getX() == location.loadLocation(collection).getBlockX() && block.getY() == location.loadLocation(collection).getBlockY() && block.getZ() == location.loadLocation(collection).getBlockZ()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String getEggID(Block block, String collection){
        for (String key : plugin.getEggDataManager().getPlacedEggs(collection).getConfigurationSection("PlacedEggs.").getKeys(false)) {
            if(!plugin.getEggDataManager().getPlacedEggs(collection).contains("PlacedEggs.")) continue;
            ConfigLocationUtil location = new ConfigLocationUtil(plugin, "PlacedEggs." + key + ".");
            if (location.loadLocation(collection) != null) {
                if (block.getX() == location.loadLocation(collection).getBlockX() && block.getY() == location.loadLocation(collection).getBlockY() && block.getZ() == location.loadLocation(collection).getBlockZ()) {
                    return key;
                }
            }
        }
        return null;
    }

    public String getEggCollection(Block block){
        for(String collection : plugin.getEggDataManager().savedEggCollections()) {
            if (!plugin.getEggDataManager().getPlacedEggs(collection).contains("PlacedEggs.")) continue;
            for (String key : plugin.getEggDataManager().getPlacedEggs(collection).getConfigurationSection("PlacedEggs.").getKeys(false)) {
                ConfigLocationUtil location = new ConfigLocationUtil(plugin, "PlacedEggs." + key + ".");
                if (location.loadLocation(collection) != null) {
                    if (block.getX() == location.loadLocation(collection).getBlockX() && block.getY() == location.loadLocation(collection).getBlockY() && block.getZ() == location.loadLocation(collection).getBlockZ()) {
                        return collection;
                    }
                }
            }
        }
        return null;
    }

    public Location getEggLocation(String eggID, String collection){
        ConfigLocationUtil location = new ConfigLocationUtil(plugin, "PlacedEggs." + eggID + ".");
        return location.loadLocation(collection);
    }

    public String getEggCollectionFromPlayerData(UUID uuid){
        FileConfiguration config = plugin.getPlayerEggDataManager().getPlayerData(uuid);
        if(config.contains("SelectedSection")){
            return config.getString("SelectedSection");
        }
        return null;
    }

    public void saveFoundEggs(Player player, Block block, String id, String collection){
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(player.getUniqueId());
        placedEggs.set("PlacedEggs." + id + ".TimesFound", placedEggs.contains("PlacedEggs." + id + ".TimesFound") ? placedEggs.getInt("PlacedEggs." + id + ".TimesFound") + 1 : 1);
        Main.getInstance().getEggDataManager().savePlacedEggs(collection);
        playerConfig.set("Found." + collection + ".Count", playerConfig.contains("Found." + collection + ".Count") ? playerConfig.getInt("Found." + collection + ".Count") + 1 : 1);
        playerConfig.set("Found." + collection + ".Name", player.getName());
        new ConfigLocationUtil(plugin, block.getLocation(), "Found." + collection + "." + id).saveBlockLocation(player.getUniqueId());
        plugin.getPlayerEggDataManager().savePlayerData(player.getUniqueId(), playerConfig);
        if(!Main.getInstance().getPluginConfig().getPlayerFoundOneEggRewards() || !Main.getInstance().getPluginConfig().getPlayerFoundAllEggsReward())
            player.sendMessage(messageManager.getMessage(MessageKey.EGG_FOUND).replaceAll("%TREASURES_FOUND%", String.valueOf(getEggsFound(player, collection))).replaceAll("%TREASURES_MAX%", String.valueOf(getMaxEggs(collection))));
    }

    public int getTimesFound(String id, String collection) {
        return plugin.getEggDataManager().getPlacedEggs(collection).contains("PlacedEggs." + id + ".TimesFound") ? plugin.getEggDataManager().getPlacedEggs(collection).getInt("PlacedEggs." + id + ".TimesFound") : 0;
    }

    public String getEggDatePlaced(String id, String collection) {
        return plugin.getEggDataManager().getPlacedEggs(collection).getString("PlacedEggs." + id + ".Date");
    }

    public String getEggTimePlaced(String id, String collection) {
        return plugin.getEggDataManager().getPlacedEggs(collection).getString("PlacedEggs." + id + ".Time");
    }

    public String getEggDateCollected(String uuid, String id, String collection) {
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(UUID.fromString(uuid));
        return playerConfig.getString("Found." + collection + "." + id + ".Date");
    }

    public String getEggTimeCollected(String uuid, String id, String collection) {
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(UUID.fromString(uuid));
        return playerConfig.getString("Found." + collection + "." + id + ".Time");
    }

    public boolean hasFound(Player player, String id, String collection){
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(player.getUniqueId());
        return playerConfig.contains("Found." + collection + "." + id);
    }

    public int getMaxEggs(String collection){
        return plugin.getEggDataManager().getPlacedEggs(collection).getInt("MaxEggs");
    }

    public int getEggsFound(Player player, String collection){
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(player.getUniqueId());
        return playerConfig.getInt("Found." + collection + ".Count");
    }

    public void updateMaxEggs(String collection){
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        if(!placedEggs.contains("PlacedEggs.")){
            placedEggs.set("MaxEggs", 0);
            Main.getInstance().getEggDataManager().savePlacedEggs(collection);
            return;
        }
        ArrayList<String> maxEggs = new ArrayList<>(placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false));
        placedEggs.set("MaxEggs", maxEggs.size());
        Main.getInstance().getEggDataManager().savePlacedEggs(collection);
    }

    public boolean checkFoundAll(Player player, String collection){
        return getEggsFound(player, collection) == getMaxEggs(collection);
    }

    public void markEggAsFound(String collection, String eggID, boolean marked){
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        placedEggs.set("PlacedEggs." + eggID + ".markedAsFound", marked);
        Main.getInstance().getEggDataManager().savePlacedEggs(collection);
        Bukkit.broadcastMessage("reset "+eggID+" in "+collection+ " "+marked);
    }

    public boolean isMarkedAsFound(String collection, String eggID){
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        return placedEggs.getBoolean("PlacedEggs." + eggID + ".markedAsFound");
    }

    public void spawnEggParticle() {
        if (spawnEggParticleTask != null)
            spawnEggParticleTask.cancel();

        if (!Main.getInstance().getPluginConfig().getParticleEnabled()) {
            return;
        }
        spawnEggParticleTask = new BukkitRunnable() {
            double time = 0;

            List<String> collections = plugin.getEggDataManager().savedEggCollections();

            @Override
            public void run() {
                for(String collection : collections) {
                    FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
                    if (time > 3.0)
                        time = 0;
                    else
                        time += 0.025;
                    if (!placedEggs.contains("PlacedEggs.") || !placedEggs.contains("Enabled")) {
                        continue;
                    }
                    // check if the collection is enabled
                    boolean enabled = placedEggs.getBoolean("Enabled");
                    if(!enabled) return;

                    for (String eggId : placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false)) {
                        ConfigLocationUtil locationUtil = new ConfigLocationUtil(plugin, "PlacedEggs." + eggId);
                        if (locationUtil.loadLocation(collection) == null) {
                            continue;
                        }
                        if(isMarkedAsFound(collection,eggId)) continue;
                        String world = placedEggs.getString("PlacedEggs." + eggId + ".World");
                        int x = placedEggs.getInt("PlacedEggs." + eggId + ".X");
                        int y = placedEggs.getInt("PlacedEggs." + eggId + ".Y");
                        int z = placedEggs.getInt("PlacedEggs." + eggId + ".Z");
                        Location startLocation = new Location(Bukkit.getWorld(world), x, y, z);
                        for (Entity entity : startLocation.getWorld().getNearbyEntities(startLocation, 10, 10, 10)) {
                            if (!(entity instanceof Player)) {
                                continue;
                            }
                            Player player = (Player) entity;
                            if (time > 2.0) {
                                double startX = startLocation.getX() - 1;
                                double startY = startLocation.getY();
                                double startZ = startLocation.getZ() + 1;
                                player.spawnParticle(getParticle(player, eggId, collection), (startX - 1) + time, (startY), (startZ), 0);
                                player.spawnParticle(getParticle(player, eggId, collection), (startX + 2), (startY), (startZ - 3) + time, 0);
                                player.spawnParticle(getParticle(player, eggId, collection), (startX + 2), (startY + 3) - time, (startZ), 0);
                                continue;
                            }
                            if (time > 1.0) {
                                double startX = startLocation.getX();
                                double startY = startLocation.getY();
                                double startZ = startLocation.getZ() - 1;
                                player.spawnParticle(getParticle(player, eggId, collection), (startX - 1) + time, startY, (startZ + 1), 0);
                                player.spawnParticle(getParticle(player, eggId, collection), startX, startY, startZ + time, 0);
                                player.spawnParticle(getParticle(player, eggId, collection), (startX), (startY + 2) - time, (startZ + 2), 0);
                                player.spawnParticle(getParticle(player, eggId, collection), (startX + 1), (startY + 2) - time, (startZ + 1), 0);
                                player.spawnParticle(getParticle(player, eggId, collection), (startX - 1) + time, (startY + 1), (startZ + 2), 0);
                                player.spawnParticle(getParticle(player, eggId, collection), (startX + 1), (startY + 1), (startZ) + time, 0);
                                continue;
                            }

                            double startX = startLocation.getX();
                            double startY = startLocation.getY() + 1.0;
                            double startZ = startLocation.getZ();
                            player.spawnParticle(getParticle(player, eggId, collection), startX + time, startY, startZ, 0);
                            player.spawnParticle(getParticle(player, eggId, collection), startX, startY - time, startZ, 0);
                            player.spawnParticle(getParticle(player, eggId, collection), startX, startY, startZ + time, 0);
                        }
                        int radius = Main.getInstance().getPluginConfig().getShowEggsNearbyMessageRadius();
                        for (Entity e : startLocation.getWorld().getNearbyEntities(startLocation, radius, radius, radius)) {
                            if (e instanceof Player) {
                                Player p = (Player) e;
                                if (!hasFound(p, eggId, collection)) {
                                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(messageManager.getMessage(MessageKey.EGG_NEARBY)));
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 0, 15);
    }

    public Particle getParticle(Player p, String key, String collection){
        if(hasFound(p, key, collection)){
            return eggNotFoundParticle;
        }else {
            return eggFoundParticle;
        }
    }

    public void resetStatsPlayer(String name, String collection){
        ArrayList<String> eggID = new ArrayList<>();
        for(UUID uuids : plugin.getEggDataManager().savedPlayers()){
            FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuids);
            if(playerConfig.getString("Found." + collection) == null) continue;
            if(playerConfig.getString("Found." + collection + ".Name").equals(name)) {
                eggID.addAll(playerConfig.getConfigurationSection("Found." + collection).getKeys(false));
                playerConfig.set("Found." + collection, null);
                plugin.getPlayerEggDataManager().savePlayerData(uuids, playerConfig);
            }
        }
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        ConfigurationSection eggsSection = placedEggs.getConfigurationSection("PlacedEggs");

        if (eggsSection != null) {
            for (String ids : eggsSection.getKeys(false)) {
                if (eggID.contains(ids)) {
                    placedEggs.set("PlacedEggs." + ids + ".TimesFound", getTimesFound(ids, collection) - 1);
                    markEggAsFound(collection,ids,false);
                    plugin.getEggDataManager().savePlacedEggs(collection);
                }
            }
        }
    }

    public void resetStatsPlayerEgg(UUID uuid, String collection, String id){
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuid);
        int count = getPlayerCount(uuid, collection);
        playerConfig.set("Found." + collection + "." + id, null);
        playerConfig.set("Found." + collection + ".Count", count-1);
        plugin.getPlayerEggDataManager().savePlayerData(uuid, playerConfig);

        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        placedEggs.set("PlacedEggs." + id + ".TimesFound", getTimesFound(id, collection)-1);
        markEggAsFound(collection,id,false);
        plugin.getEggDataManager().savePlacedEggs(collection);
    }

    public void resetStatsAll(){
        for(UUID uuids : plugin.getEggDataManager().savedPlayers()){
            for (String collection : plugin.getEggDataManager().savedEggCollections()) {
                ArrayList<String> eggID = new ArrayList<>();

                FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuids);
                if(playerConfig.getString("Found." + collection) == null) return;
                eggID.addAll(playerConfig.getConfigurationSection("Found." + collection).getKeys(false));
                playerConfig.set("Found." + collection, null);
                plugin.getPlayerEggDataManager().savePlayerData(uuids, playerConfig);

                FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
                ConfigurationSection eggsSection = placedEggs.getConfigurationSection("PlacedEggs");

                if (eggsSection != null) {
                    for (String ids : eggsSection.getKeys(false)) {
                        if (eggID.contains(ids)) {
                            placedEggs.set("PlacedEggs." + ids + ".TimesFound", getTimesFound(ids, collection) - 1);
                            markEggAsFound(collection,ids,false);
                            plugin.getEggDataManager().savePlacedEggs(collection);
                        }
                    }
                }
            }
        }
    }

    public boolean containsPlayer(String name) {
        for (UUID uuid : plugin.getEggDataManager().savedPlayers()) {
            FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuid);
            for (String collection : Main.getInstance().getEggDataManager().savedEggCollections()) {
                if (playerConfig == null || playerConfig.getString("Found.") == null || playerConfig.getString("Found." + collection) == null) {
                    continue;
                }
                if (playerConfig.getString("Found." + collection + ".Name").equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }
    public String getPlayerNameFromUUID(UUID uuid) {
        for (UUID uuids : plugin.getEggDataManager().savedPlayers()) {
            if(uuid.equals(uuids)){
                FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuid);
                for (String collection : Main.getInstance().getEggDataManager().savedEggCollections()) {
                    if (playerConfig == null || playerConfig.getString("Found.") == null || playerConfig.getString("Found." + collection) == null) {
                        continue;
                    }
                    return playerConfig.getString("Found." + collection + ".Name");
                }
            }
        }
        return "No Name found";
    }
    public boolean containsPlayerUUID(UUID uuid) {
        return plugin.getEggDataManager().savedPlayers().contains(uuid);
    }
    public void showAllEggs(){
        for(String collection : plugin.getEggDataManager().savedEggCollections()) {
            FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
            if (!placedEggs.contains("PlacedEggs.")) {
                continue;
            }
            for (String key : placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false)) {
                ConfigLocationUtil locationUtil = new ConfigLocationUtil(plugin, "PlacedEggs." + key);
                if (locationUtil.loadLocation(collection) == null) {
                    continue;
                }
                String world = placedEggs.getString("PlacedEggs." + key + ".World");
                int x = placedEggs.getInt("PlacedEggs." + key + ".X");
                int y = placedEggs.getInt("PlacedEggs." + key + ".Y");
                int z = placedEggs.getInt("PlacedEggs." + key + ".Z");
                Location loc = new Location(Bukkit.getWorld(world), x, y, z).add(0.5, 0.5, 0.5);
                ArmorStand armorStand = loc.getWorld().spawn(loc, ArmorStand.class);
                armorStand.setGravity(false);
                armorStand.setInvulnerable(true);
                armorStand.setGlowing(true);
                armorStand.setCustomName("§dEgg #" + key + " for collection §d§l" + collection);
                armorStand.setCustomNameVisible(true);
                armorStand.setSmall(true);
                armorStand.setVisible(false);

                NBT.modify(armorStand, nbt -> {
                    nbt.getStringList("Tags").add("AdvancedHunt_show");
                });

                plugin.getShowedArmorstands().add(armorStand);
            }
        }

        new BukkitRunnable() {
            int count = Main.getInstance().getPluginConfig().getArmorstandGlow();

            @Override
            public void run() {
                count--;
                if (count <= 0) {
                    for (ArmorStand a : Main.getInstance().getShowedArmorstands()) {
                        a.remove();
                    }
                    Main.getInstance().getShowedArmorstands().removeAll(Main.getInstance().getShowedArmorstands());
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0, 20);
    }

    public String getLeaderboardPositionName(int position, UUID holder, String collection){
        if(collection == null) collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(holder);
        HashMap<String, Integer> leaderboard = new HashMap<>();
        if(Main.getInstance().getEggDataManager().savedPlayers().size() != 0){
            for(UUID uuid : Main.getInstance().getEggDataManager().savedPlayers()) {
                FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuid);
                if(playerConfig.getString("Found." + collection) == null) continue;
                leaderboard.put(playerConfig.getString("Found." + collection + ".Name"), playerConfig.getInt("Found." + collection + ".Count"));
            }
        }

        List<Map.Entry<String, Integer>> leaderList = new ArrayList<>(leaderboard.entrySet());
        leaderList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        if (!leaderList.isEmpty() && position >= 0 && leaderList.size() > position) {
            return String.valueOf(leaderList.get(position).getKey());
        } else {
            return String.valueOf(plugin.getPluginConfig().getPlaceholderAPIName());
        }

    }

    public String getLeaderboardPositionCount(int position, UUID holder, String collection){
        if(collection == null) collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(holder);
        HashMap<String, Integer> leaderboard = new HashMap<>();
        if(!Main.getInstance().getEggDataManager().savedPlayers().isEmpty()){
            for(UUID uuid : Main.getInstance().getEggDataManager().savedPlayers()) {
                FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuid);
                if(playerConfig.getString("Found." + collection) == null) continue;
                leaderboard.put(playerConfig.getString("Found." + collection + ".Name"), playerConfig.getInt("Found." + collection + ".Count"));
            }
        }

        List<Map.Entry<String, Integer>> leaderList = new ArrayList<>(leaderboard.entrySet());
        leaderList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        if(!leaderList.isEmpty() && position >= 0 && leaderList.size() > position){
            return String.valueOf(leaderList.get(position).getValue());
        }else
            return String.valueOf(plugin.getPluginConfig().getPlaceholderAPICount());
    }

    private boolean isVersionLessThan(String versionToCompare) {
        String pluginVersion = plugin.getDescription().getVersion();
        return VersionComparator.isLessThan(pluginVersion, versionToCompare);
    }

    public XMaterial getBlockMaterialOfEgg(String eggID, String collection){
        return XMaterial.matchXMaterial(getEggLocation(eggID, collection).getBlock().getType());
    }

    public String getHeadTextureValue(String eggID, String collection) {
        ItemStack treasure = ItemHelper.getItemStackFromBlock(getEggLocation(eggID, collection).getBlock());
        if (treasure == null)
            return Main.getInstance().getEggManager().getRandomEggTexture(new Random().nextInt(7));
        return Main.getTexture(ItemHelper.getSkullTexture(treasure));
    }

    public void convertEggData() {
        File eggsFile = new File(plugin.getDataFolder(), "eggs.yml");

        if (!eggsFile.exists() || !isVersionLessThan("2.1.0")) {
            return;
        }
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        messageManager.sendMessage(console, MessageKey.EGG_DATA_CONVERT_START);
        FileConfiguration eggsConfig = YamlConfiguration.loadConfiguration(eggsFile);

        Bukkit.broadcastMessage(messageManager.getMessage(MessageKey.EGG_DATA_CONVERT_BROADCAST));
        for(Player player : Bukkit.getOnlinePlayers()){
            if(player.isOp())
                messageManager.sendMessage(player, MessageKey.EGG_DATA_CONVERT_OP_MESSAGE);
        }
        if (!eggsConfig.contains("Eggs")) {
            return;
        }
        File oldFile = new File(plugin.getDataFolder() + "/eggs/", "eggs.yml");
        FileConfiguration placedEggsConfig = YamlConfiguration.loadConfiguration(oldFile);

        placedEggsConfig.set("PlacedEggs", eggsConfig.getConfigurationSection("Eggs"));
        placedEggsConfig.set("MaxEggs", eggsConfig.getInt("MaxEggs"));
        try {
            placedEggsConfig.save(oldFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        messageManager.sendMessage(console, MessageKey.EGG_DATA_CONVERT_LOCATIONS);

        ArrayList<UUID> convertPlayers = new ArrayList<>();

        for (String uuids : eggsConfig.getConfigurationSection("Found.").getKeys(false)) {
            UUID uuid = UUID.fromString(uuids);
            convertPlayers.add(uuid);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!convertPlayers.isEmpty()) {
                    List<UUID> playersToConvert = convertPlayers.subList(0, Math.min(5, convertPlayers.size()));

                    for (UUID uuid : playersToConvert) {
                        plugin.getPlayerEggDataManager().createPlayerFile(uuid);
                        FileConfiguration playerEggsConfig = plugin.getPlayerEggDataManager().getPlayerData(uuid);
                        ConfigurationSection foundEggsSection = eggsConfig.getConfigurationSection("Found." + uuid);
                        if (foundEggsSection != null) {
                            playerEggsConfig.set("Found", foundEggsSection);
                            plugin.getPlayerEggDataManager().savePlayerData(uuid, playerEggsConfig);
                        }
                        messageManager.sendMessage(console, MessageKey.EGG_DATA_CONVERT_PLAYER, "%UUID%", uuid.toString());
                    }

                    convertPlayers.removeAll(playersToConvert);
                }else{
                    cancel();
                    Bukkit.broadcastMessage(messageManager.getMessage(MessageKey.EGG_DATA_CONVERT_DONE));

                    try {
                        if (eggsFile.delete()) {
                            messageManager.sendMessage(console, MessageKey.EGG_DATA_CONVERT_SUCCESS);
                        } else {
                            messageManager.sendMessage(console, MessageKey.EGG_DATA_CONVERT_DELETE_FAIL);
                        }
                    } catch (SecurityException e) {
                        messageManager.sendMessage(console, MessageKey.EGG_DATA_CONVERT_DELETE_ERROR, "%ERROR%", e.getMessage());
                    }
                    messageManager.sendMessage(console, MessageKey.EGG_DATA_CONVERT_COMPLETE);
                }
            }
        }.runTaskTimer(plugin, 0, 60);
    }
}
