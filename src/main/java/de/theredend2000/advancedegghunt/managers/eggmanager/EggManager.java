package de.theredend2000.advancedegghunt.managers.eggmanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.configurations.InventoryConfig;
import de.theredend2000.advancedegghunt.util.ConfigLocationUtil;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
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

    public ItemStack giveFinishedEggToPlayer(int id){
        return new ItemBuilder(XMaterial.PLAYER_HEAD).setDisplayname("§6Easter Egg").setLore("§7Place this egg around the map", "§7that everyone can search and find it.").setSkullOwner(getRandomEggTexture(id)).build();
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
                        Main.getInstance().getEggDataManager().savePlacedEggs(collection, config);
                        player.sendMessage(messageManager.getMessage(MessageKey.EGG_BROKEN).replaceAll("%ID%", key));
                        keys.remove(key);
                    }
                }
            }
            for (UUID uuids : plugin.getEggDataManager().savedPlayers()) {
                FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuids);
                if(playerConfig.contains("FoundEggs." + collection)) {
                    for (String eggID : playerConfig.getConfigurationSection("FoundEggs." + collection).getKeys(false)) {
                        if(eggID.equalsIgnoreCase("Count") || eggID.equalsIgnoreCase("Name")) continue;
                        ConfigLocationUtil location = new ConfigLocationUtil(plugin, "FoundEggs." + collection + "." + eggID);
                        if (location.loadLocation(uuids) != null) {
                            if (block.getX() == location.loadLocation(uuids).getBlockX() && block.getY() == location.loadLocation(uuids).getBlockY() && block.getZ() == location.loadLocation(uuids).getBlockZ()) {
                                playerConfig.set("FoundEggs." + collection + "." + eggID, null);
                                playerConfig.set("FoundEggs." + collection + ".Count", getPlayerCount(uuids, collection) - 1);
                                plugin.getPlayerEggDataManager().savePlayerData(uuids, playerConfig);
                            }
                        }
                    }
                }
            }
            if(keys.isEmpty()){
                plugin.getEggDataManager().getPlacedEggs(collection).set("PlacedEggs", null);
                plugin.getEggDataManager().savePlacedEggs(collection, config);
            }
            updateMaxEggs(collection);
        }
    }

    public int getPlayerCount(UUID uuid, String collection){
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuid);
        return playerConfig.getInt("FoundEggs." + collection + ".Count");
    }

    public int getRandomNotFoundEgg(Player player, String collection){
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(player.getUniqueId());
        if(plugin.getEggDataManager().getPlacedEggs(collection).contains("PlacedEggs.") && playerConfig.contains("FoundEggs.")) {
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
        Main.getInstance().getEggDataManager().savePlacedEggs(collection, placedEggs);
        playerConfig.set("FoundEggs." + collection + ".Count", playerConfig.contains("FoundEggs." + collection + ".Count") ? playerConfig.getInt("FoundEggs." + collection + ".Count") + 1 : 1);
        playerConfig.set("FoundEggs." + collection + ".Name", player.getName());
        new ConfigLocationUtil(plugin, block.getLocation(), "FoundEggs." + collection + "." + id).saveBlockLocation(player.getUniqueId());
        plugin.getPlayerEggDataManager().savePlayerData(player.getUniqueId(), playerConfig);
        if(!Main.getInstance().getPluginConfig().getPlayerFoundOneEggRewards() || !Main.getInstance().getPluginConfig().getPlayerFoundAllEggsReward())
            player.sendMessage(messageManager.getMessage(MessageKey.EGG_FOUND).replaceAll("%EGGS_FOUND%", String.valueOf(getEggsFound(player, collection))).replaceAll("%EGGS_MAX%", String.valueOf(getMaxEggs(collection))));
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
        return playerConfig.getString("FoundEggs." + collection + "." + id + ".Date");
    }

    public String getEggTimeCollected(String uuid, String id, String collection) {
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(UUID.fromString(uuid));
        return playerConfig.getString("FoundEggs." + collection + "." + id + ".Time");
    }

    public boolean hasFound(Player player, String id, String collection){
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(player.getUniqueId());
        return playerConfig.contains("FoundEggs." + collection + "." + id);
    }

    public int getMaxEggs(String collection){
        return plugin.getEggDataManager().getPlacedEggs(collection).getInt("MaxEggs");
    }

    public int getEggsFound(Player player, String collection){
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(player.getUniqueId());
        return playerConfig.getInt("FoundEggs." + collection + ".Count");
    }

    public void updateMaxEggs(String collection){
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        if(!placedEggs.contains("PlacedEggs.")){
            placedEggs.set("MaxEggs", 0);
            Main.getInstance().getEggDataManager().savePlacedEggs(collection, placedEggs);
            return;
        }
        ArrayList<String> maxEggs = new ArrayList<>(placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false));
        placedEggs.set("MaxEggs", maxEggs.size());
        Main.getInstance().getEggDataManager().savePlacedEggs(collection, placedEggs);
    }

    public boolean checkFoundAll(Player player, String collection){
        return getEggsFound(player, collection) == getMaxEggs(collection);
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
                    if (!placedEggs.contains("PlacedEggs.")) {
                        continue;
                    }
                    for (String eggId : placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false)) {
                        ConfigLocationUtil locationUtil = new ConfigLocationUtil(plugin, "PlacedEggs." + eggId);
                        if (locationUtil.loadLocation(collection) == null) {
                            continue;
                        }
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
            if(playerConfig.getString("FoundEggs." + collection) == null) continue;
            if(playerConfig.getString("FoundEggs." + collection + ".Name").equals(name)) {
                eggID.addAll(playerConfig.getConfigurationSection("FoundEggs." + collection).getKeys(false));
                playerConfig.set("FoundEggs." + collection, null);
                plugin.getPlayerEggDataManager().savePlayerData(uuids, playerConfig);
            }
        }
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        ConfigurationSection eggsSection = placedEggs.getConfigurationSection("PlacedEggs");

        if (eggsSection != null) {
            for (String ids : eggsSection.getKeys(false)) {
                if (eggID.contains(ids)) {
                    placedEggs.set("PlacedEggs." + ids + ".TimesFound", getTimesFound(ids, collection) - 1);
                    plugin.getEggDataManager().savePlacedEggs(collection, placedEggs);
                }
            }
        }
    }

    public void resetStatsPlayerEgg(UUID uuid, String collection, String id){
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuid);
        int count = getPlayerCount(uuid, collection);
        playerConfig.set("FoundEggs." + collection + "." + id, null);
        playerConfig.set("FoundEggs." + collection + ".Count", count-1);
        plugin.getPlayerEggDataManager().savePlayerData(uuid, playerConfig);

        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        placedEggs.set("PlacedEggs." + id + ".TimesFound", getTimesFound(id, collection)-1);
        plugin.getEggDataManager().savePlacedEggs(collection, placedEggs);
    }

    public boolean containsPlayer(String name){
        for(UUID uuids : plugin.getEggDataManager().savedPlayers()){
            FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuids);
            for(String collections : Main.getInstance().getEggDataManager().savedEggCollections()) {
                if (playerConfig == null || playerConfig.getString("FoundEggs.") == null || playerConfig.getString("FoundEggs." + collections) == null) {
                    continue;
                }
                if (playerConfig.getString("FoundEggs." + collections + ".Name").equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }
    public void resetStatsAll(){
        for(UUID uuids : plugin.getEggDataManager().savedPlayers()){
            FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuids);
            String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(uuids);
            playerConfig.set("FoundEggs." + collection, null);
            plugin.getPlayerEggDataManager().savePlayerData(uuids, playerConfig);
        }
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
                plugin.getShowedArmorstands().add(armorStand);
            }
        }

        new BukkitRunnable() {
            int count = Main.getInstance().getPluginConfig().getArmorstandGlow();

            @Override
            public void run() {
                count--;
                if (count == 0) {
                    for (ArmorStand a : Main.getInstance().getShowedArmorstands()) {
                        a.remove();
                    }
                    Main.getInstance().getShowedArmorstands().removeAll(Main.getInstance().getShowedArmorstands());
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0, 20);
    }

    public String getLeaderboardPositionName(int position, UUID holder){
        HashMap<String, Integer> leaderboard = new HashMap<>();
        if(Main.getInstance().getEggDataManager().savedPlayers().size() != 0){
            for(UUID uuid : Main.getInstance().getEggDataManager().savedPlayers()) {
                String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(holder);
                FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuid);
                if(playerConfig.getString("FoundEggs." + collection) == null) continue;
                leaderboard.put(playerConfig.getString("FoundEggs." + collection + ".Name"), playerConfig.getInt("FoundEggs." + collection + ".Count"));
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

    public String getLeaderboardPositionCount(int position, UUID holder){
        HashMap<String, Integer> leaderboard = new HashMap<>();
        if(!Main.getInstance().getEggDataManager().savedPlayers().isEmpty()){
            for(UUID uuid : Main.getInstance().getEggDataManager().savedPlayers()) {
                String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(holder);
                FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuid);
                if(playerConfig.getString("FoundEggs." + collection) == null) continue;
                leaderboard.put(playerConfig.getString("FoundEggs." + collection + ".Name"), playerConfig.getInt("FoundEggs." + collection + ".Count"));
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
        String[] currentVersionParts = pluginVersion.split("\\.");
        String[] targetVersionParts = versionToCompare.split("\\.");

        if (currentVersionParts.length != 3 || targetVersionParts.length != 3) {
            return false;
        }

        int currentMajor = Integer.parseInt(currentVersionParts[0]);
        int currentMinor = Integer.parseInt(currentVersionParts[1]);
        int currentPatch = Integer.parseInt(currentVersionParts[2]);

        int targetMajor = Integer.parseInt(targetVersionParts[0]);
        int targetMinor = Integer.parseInt(targetVersionParts[1]);
        int targetPatch = Integer.parseInt(targetVersionParts[2]);

        if (currentMajor < targetMajor) {
            return true;
        } else if (currentMajor == targetMajor && currentMinor < targetMinor) {
            return true;
        } else return currentMajor == targetMajor && currentMinor == targetMinor && currentPatch < targetPatch;
    }

    public void convertEggData() {
        File eggsFile = new File(plugin.getDataFolder(), "eggs.yml");

        if (!eggsFile.exists() || !isVersionLessThan("2.1.0")) {
            return;
        }
        Bukkit.getConsoleSender().sendMessage("§cAdvancedEggHunt found the old configuration system.\n §cIt is now changing to the new one.\n§4THIS CAN TAKE A WHILE!");
        FileConfiguration eggsConfig = YamlConfiguration.loadConfiguration(eggsFile);

        Bukkit.broadcastMessage("§4AdvancedEggHunt IS UPDATING THEIR CONFIGURATION SYSTEM PLEASE DONT MOVE UTIL FINISHED");
        for(Player player : Bukkit.getOnlinePlayers()){
            if(player.isOp())
                player.sendMessage("§cTHE BEST IS WHEN NOBODY IS ON THE SERVER DURING THE CONVERT. §4CONSOLE SHOWS THE PROGRESS");
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
        Bukkit.getConsoleSender().sendMessage("§aAdvancedEggHunt converted all egg locations!");

        ArrayList<UUID> convertPlayers = new ArrayList<>();

        for (String uuids : eggsConfig.getConfigurationSection("FoundEggs.").getKeys(false)) {
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
                        ConfigurationSection foundEggsSection = eggsConfig.getConfigurationSection("FoundEggs." + uuid);
                        if (foundEggsSection != null) {
                            playerEggsConfig.set("FoundEggs", foundEggsSection);
                            plugin.getPlayerEggDataManager().savePlayerData(uuid, playerEggsConfig);
                        }
                        Bukkit.getConsoleSender().sendMessage("§eConverted player data of " + uuid + ".");
                    }

                    convertPlayers.removeAll(playersToConvert);
                }else{
                    cancel();
                    Bukkit.broadcastMessage("§aUPDATING DONE!");

                    try {
                        if (eggsFile.delete()) {
                            Bukkit.getConsoleSender().sendMessage("§aeggs.yml was converted successfully.");
                        } else {
                            Bukkit.getConsoleSender().sendMessage("§ceggs.yml can't be deleted.");
                        }
                    } catch (SecurityException e) {
                        Bukkit.getConsoleSender().sendMessage("§4There was an error while deleting the eggs.yml file: " + e.getMessage());
                    }
                    Bukkit.getConsoleSender().sendMessage("§aAdvancedEggHunt converted all data to the new system.\n§2The plugin is now running flawless again\n§4Please report bugs on the discord: §9https://discord.gg/qapvQMUt34");
                }
            }
        }.runTaskTimer(plugin, 0, 60);
    }
}
