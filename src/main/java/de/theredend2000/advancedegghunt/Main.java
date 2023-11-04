package de.theredend2000.advancedegghunt;

import de.theredend2000.advancedegghunt.bstats.Metrics;
import de.theredend2000.advancedegghunt.commands.AdvancedEggHuntCommand;
import de.theredend2000.advancedegghunt.listeners.*;
import de.theredend2000.advancedegghunt.placeholderapi.PlaceholderExtension;
import de.theredend2000.advancedegghunt.util.Updater;
import de.theredend2000.advancedegghunt.util.enums.LeaderboardSortTypes;
import de.theredend2000.advancedegghunt.util.saveinventory.DatetimeUtils;
import de.theredend2000.advancedegghunt.versions.VersionManager;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.hintInventory.HintInventoryCreator;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class Main extends JavaPlugin {

    private static Main plugin;
    private DatetimeUtils datetimeUtils;
    private Map<String, Long> refreshCooldown;
    private ArrayList<Player> placeEggsPlayers;
    private HashMap<Player, Integer> playerAddCommand;
    private ArrayList<ArmorStand> showedArmorstands;
    public YamlConfiguration messages;
    public File messagesData;
    public YamlConfiguration eggs;
    private HashMap<Player, LeaderboardSortTypes> sortTypeLeaderboard;
    private File data = new File(getDataFolder(), "eggs.yml");

    @Override
    public void onEnable() {
        plugin = this;
        Metrics metrics = new Metrics(this,19495);
        refreshCooldown = new HashMap<String, Long>();
        placeEggsPlayers = new ArrayList<>();
        showedArmorstands = new ArrayList<>();
        playerAddCommand = new HashMap<>();
        sortTypeLeaderboard = new HashMap<>();
        setupConfigs();
        VersionManager.registerAllManagers();
        getCommand("advancedegghunt").setExecutor(new AdvancedEggHuntCommand());
        initListeners();
        datetimeUtils = new DatetimeUtils();
        VersionManager.getEggManager().spawnEggParticle();
        checkCommandFeedback();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Bukkit.getConsoleSender().sendMessage(messages.getString("Prefix").replaceAll("&","§")+"§aAdvanced Egg Hunt detected PlaceholderAPI, enabling placeholders.");
            new PlaceholderExtension().register();
            Bukkit.getConsoleSender().sendMessage(messages.getString("Prefix").replaceAll("&","§")+"§2§lAll placeholders successfully enabled.");
        }
    }

    @Override
    public void onDisable() {
        giveAllItemsBack();
        for(ArmorStand a : showedArmorstands){
            a.remove();
        }
        Main.getInstance().eggs.set("Edit",null);
        Main.getInstance().saveEggs();
    }

    public void checkCommandFeedback(){
        if(getConfig().getBoolean("Settings.DisableCommandFeedback")){
            for(World worlds : Bukkit.getServer().getWorlds())
                worlds.setGameRule(GameRule.SEND_COMMAND_FEEDBACK,false);
        }else{
            for(World worlds : Bukkit.getServer().getWorlds())
                worlds.setGameRule(GameRule.SEND_COMMAND_FEEDBACK,true);
        }
    }

    private void initListeners(){
        new InventoryClickEventListener();
        new BlockPlaceEventListener();
        new BlockBreakEventListener();
        new PlayerInteractEventListener();
        new PlayerInteractItemEvent();
        new Updater(this);
        new PlayerChatEventListener();
        new ExplodeEventListener();
        new PlayerConnectionListener();
        new EntityChangeListener();
        new HintInventoryCreator(null,null,false);
        Bukkit.getConsoleSender().sendMessage(getMessage("Prefix").replaceAll("&","§")+"§aAll Listeners registered.");
    }

    private void giveAllItemsBack(){
        for(Player player : Bukkit.getServer().getOnlinePlayers()){
            if(placeEggsPlayers.contains(player)){
                VersionManager.getEggManager().finishEggPlacing(player);
            }
        }
    }

    private void setupConfigs(){
        saveDefaultConfig();
        messagesData = new File(getDataFolder(),"messages.yml");
        try {
            if(!messagesData.exists()){
                InputStream in = getResource("messages.yml");
                assert in != null;
                Files.copy(in,messagesData.toPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.messages = YamlConfiguration.loadConfiguration(this.messagesData);
        saveMessages();
        this.eggs = YamlConfiguration.loadConfiguration(this.data);
        this.saveEggs();
        checkUpdatePath();
    }

    private void checkUpdatePath(){
        if(messages.getDouble("version") < 2.0){
            messagesData.delete();
            setupConfigs();
            for(Player player : Bukkit.getOnlinePlayers()){
                if(player.isOp()){
                    player.sendMessage(Main.getInstance().getMessage("Prefix").replaceAll("&","§")+"§cBecause of a newer version, your files got reinstalled. Please check your messages.yml again.");
                }
            }
            Bukkit.getConsoleSender().sendMessage(Main.getInstance().getMessage("Prefix").replaceAll("&","§")+"§cBecause of a newer version, your files got reinstalled. Please check your messages.yml again.");
        }
        if(getConfig().getDouble("config-version") < 2.4){
            File configFile = new File(getDataFolder(), "config.yml");
            configFile.delete();
            saveDefaultConfig();
            reloadConfig();
            for(Player player : Bukkit.getOnlinePlayers()){
                if(player.isOp()){
                    player.sendMessage(Main.getInstance().getMessage("Prefix").replaceAll("&","§")+"§cBecause of a newer version, your files got reinstalled. Please check your config.yml again.");
                }
            }
            Bukkit.getConsoleSender().sendMessage(Main.getInstance().getMessage("Prefix").replaceAll("&","§")+"§cBecause of a newer version, your files got reinstalled. Please check your config.yml again.");
        }
    }

    public void saveMessages() {
        try {
            this.messages.save(this.messagesData);
        } catch (IOException var2) {
            var2.printStackTrace();
        }
    }

    public void saveEggs() {
        try {
            this.eggs.save(this.data);
        } catch (IOException var2) {
            var2.printStackTrace();
        }
    }

    public Material getMaterial(String materialString) {
        try {
            Material material = Material.getMaterial(materialString);
            if (material == null) {
                return Material.BARRIER;
            }
            return material;
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage("§4Material Error: "+ex);
            return Material.STONE;
        }
    }


    public static Main getInstance() {
        return plugin;
    }

    public static String getTexture(String texture){
        String prefix = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUv";
        texture = prefix+texture;
        return texture;
    }
    public String getMessage(String message){
        if(getConfig().getBoolean("Settings.PluginPrefixEnabled")){
            return messages.getString("Prefix").replace("&","§")+messages.getString(message).replace("&","§");
        }else
            return messages.getString(message).replace("&","§");
    }
    private static final HashMap<Player, PlayerMenuUtility> playerMenuUtilityMap = new HashMap<>();
    public static PlayerMenuUtility getPlayerMenuUtility(Player p) {
        PlayerMenuUtility playerMenuUtility;
        if (!(playerMenuUtilityMap.containsKey(p))) {

            playerMenuUtility = new PlayerMenuUtility(p);
            playerMenuUtilityMap.put(p, playerMenuUtility);

            return playerMenuUtility;
        } else {
            return playerMenuUtilityMap.get(p);
        }
    }

    public ArrayList<Player> getPlaceEggsPlayers() {
        return placeEggsPlayers;
    }
    public Map<String, Long> getRefreshCooldown() {
        return refreshCooldown;
    }

    public DatetimeUtils getDatetimeUtils() {
        return datetimeUtils;
    }

    public ArrayList<ArmorStand> getShowedArmorstands() {
        return showedArmorstands;
    }

    public HashMap<Player, Integer> getPlayerAddCommand() {
        return playerAddCommand;
    }

    public HashMap<Player, LeaderboardSortTypes> getSortTypeLeaderboard() {
        return sortTypeLeaderboard;
    }
}
