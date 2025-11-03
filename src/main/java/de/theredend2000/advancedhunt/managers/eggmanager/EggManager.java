package de.theredend2000.advancedhunt.managers.eggmanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.configurations.InventoryConfig;
import de.theredend2000.advancedhunt.data.EggDataStorage;
import de.theredend2000.advancedhunt.mysql.sqldata.EggManagerSQL;
import de.theredend2000.advancedhunt.mysql.yamldata.EggManagerYAML;
import de.theredend2000.advancedhunt.util.ConfigLocationUtil;
import de.theredend2000.advancedhunt.util.ItemHelper;
import de.theredend2000.advancedhunt.util.VersionComparator;
import de.theredend2000.advancedhunt.util.XMaterialHelper;
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
import java.util.concurrent.CompletableFuture;

public class EggManager {

    private Main plugin;

    private EggDataStorage dataStorage;

    private MessageManager messageManager;
    private Particle eggNotFoundParticle;
    private Particle eggFoundParticle;
    private BukkitTask spawnEggParticleTask;

    public EggManager(){
        this.plugin = Main.getInstance();
        messageManager = Main.getInstance().getMessageManager();

        eggNotFoundParticle = Main.getInstance().getPluginConfig().getEggNotFoundParticle();
        eggFoundParticle = Main.getInstance().getPluginConfig().getEggFoundParticle();
        if(plugin.getMySQLConfig().isEnabled()){
            dataStorage = new EggManagerSQL();
        }else
            dataStorage = new EggManagerYAML();
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
        dataStorage.saveEgg(player,location,collection);
    }

    public void removeEgg(Player player, Block block, String collection){
        dataStorage.removeEgg(player,block,collection);
    }

    public int getPlayerCount(UUID uuid, String collection){
        return dataStorage.getPlayerCount(uuid,collection);
    }

    public int getRandomNotFoundEgg(UUID uuid, String collection){
        return  dataStorage.getRandomNotFoundEgg(uuid,collection);
    }

    public boolean containsEgg(Block block){
        return dataStorage.containsEgg(block);
    }

    public String getEggID(Block block, String collection){
        return dataStorage.getEggID(block,collection);
    }

    public String getEggCollection(Block block){
        return dataStorage.getEggCollection(block);
    }

    public Location getEggLocation(String eggID, String collection){
        return dataStorage.getEggLocation(eggID,collection);
    }

    public String getEggCollectionFromPlayerData(UUID uuid){
        return dataStorage.getEggCollectionFromPlayerData(uuid);
    }

    public void saveFoundEggs(UUID uuid, Block block, String id, String collection){
        dataStorage.saveFoundEggs(uuid,block,id,collection);
    }

    public int getTimesFound(String id, String collection) {
        return dataStorage.getTimesFound(id,collection);
    }

    public String getEggDatePlaced(String id, String collection) {
        return dataStorage.getEggDatePlaced(id,collection);
    }

    public String getEggTimePlaced(String id, String collection) {
        return dataStorage.getEggTimePlaced(id,collection);
    }

    public String getEggDateCollected(UUID uuid, String id, String collection) {
        return dataStorage.getEggDateCollected(uuid,id,collection);
    }

    public String getEggTimeCollected(UUID uuid, String id, String collection) {
        return dataStorage.getEggTimeCollected(uuid,id,collection);
    }

    public boolean hasFound(UUID uuid, String id, String collection){
        return dataStorage.hasFound(uuid,id,collection);
    }

    public int getMaxEggs(String collection){
        return dataStorage.getMaxEggs(collection);
    }

    public int getEggsFound(UUID uuid, String collection){
        return dataStorage.getEggsFound(uuid,collection);
    }

    public void updateMaxEggs(String collection){
        dataStorage.updateMaxEggs(collection);
    }

    public boolean checkFoundAll(UUID uuid, String collection){
        return getEggsFound(uuid, collection) == getMaxEggs(collection);
    }

    public void markEggAsFound(String collection, String eggID, boolean marked){
        dataStorage.markEggAsFound(collection,eggID,marked);
    }

    public boolean isMarkedAsFound(String collection, String eggID){
        return dataStorage.isMarkedAsFound(collection,eggID);
    }

    public void spawnEggParticle() {
        dataStorage.spawnEggParticle();
    }

    /**
     * Resets all found‐egg data for the given player and collection.
     *
     * @param uuid the player's UUID as string
     * @param collection the egg collection to reset
     */
    public void resetStatsPlayer(UUID uuid, String collection) {
        dataStorage.resetStatsPlayer(uuid,collection);
    }

    public void resetStatsPlayerEgg(UUID uuid, String collection, String id){
        dataStorage.resetStatsPlayerEgg(uuid,collection,id);
    }

    public CompletableFuture<Void> resetStatsAll() {
        return CompletableFuture.runAsync(() -> {
            plugin.getPlayerEggDataManager().resetAllPlayerData();
            plugin.getEggDataManager().resetAllEggStatistics();
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), runnable));
    }

    public boolean containsPlayer(String name) {
        return dataStorage.containsPlayer(name);
    }

    public void showAllEggs(){
        dataStorage.showAllEggs();
    }

    public String getLeaderboardPositionName(int position, UUID holder, String collection){
        return dataStorage.getLeaderboardPositionName(position, holder, collection);
    }

    public String getLeaderboardPositionCount(int position, UUID holder, String collection){
        return dataStorage.getLeaderboardPositionCount(position, holder, collection);
    }

    private boolean isVersionLessThan(String versionToCompare) {
        String pluginVersion = plugin.getDescription().getVersion();
        return VersionComparator.isLessThan(pluginVersion, versionToCompare);
    }

    public ItemStack getBlockMaterialOfEgg(String eggID, String collection){
        Location location = getEggLocation(eggID, collection);
        if (location == null) return XMaterial.PLAYER_HEAD.parseItem();

        XMaterial material = XMaterial.matchXMaterial(location.getBlock().getType());
        if (material == XMaterial.AIR)
            return XMaterial.PLAYER_HEAD.parseItem();
        return XMaterialHelper.getItemStack(material);
    }

    public String getHeadTextureValue(String eggID, String collection) {
        Location location = getEggLocation(eggID, collection);
        if (location == null) return Main.getTexture("YmFkYzA0OGE3Y2U3OGY3ZGFkNzJhMDdkYTI3ZDg1YzA5MTY4ODFlNTUyMmVlZWQxZTNkYWYyMTdhMzhjMWEifX19");

        ItemStack treasure = ItemHelper.getItemStackFromBlock(location.getBlock());
        if (treasure == null)
            return Main.getTexture("YmFkYzA0OGE3Y2U3OGY3ZGFkNzJhMDdkYTI3ZDg1YzA5MTY4ODFlNTUyMmVlZWQxZTNkYWYyMTdhMzhjMWEifX19");
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
