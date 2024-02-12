package de.theredend2000.advancedegghunt;

import com.cryptomorin.xseries.XMaterial;
import de.likewhat.customheads.api.CustomHeadsAPI;
import de.theredend2000.advancedegghunt.bstats.Metrics;
import de.theredend2000.advancedegghunt.commands.AdvancedEggHuntCommand;
import de.theredend2000.advancedegghunt.listeners.*;
import de.theredend2000.advancedegghunt.listeners.inventoryListeners.RequirementsListeners;
import de.theredend2000.advancedegghunt.managers.eggmanager.PlayerEggDataManager;
import de.theredend2000.advancedegghunt.managers.extramanager.RequirementsManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.InventoryRequirementsManager;
import de.theredend2000.advancedegghunt.placeholderapi.PlaceholderExtension;
import de.theredend2000.advancedegghunt.util.HexColor;
import de.theredend2000.advancedegghunt.util.Updater;
import de.theredend2000.advancedegghunt.util.enums.LeaderboardSortTypes;
import de.theredend2000.advancedegghunt.util.saveinventory.DatetimeUtils;
import de.theredend2000.advancedegghunt.managers.CooldownManager;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggDataManager;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedegghunt.managers.extramanager.ExtraManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.InventoryManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.hintInventory.HintInventoryCreator;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.managers.soundmanager.SoundManager;
import org.bstats.charts.CustomChart;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;

public final class Main extends JavaPlugin {

    private static Main plugin;
    private DatetimeUtils datetimeUtils;
    private Map<String, Long> refreshCooldown;
    private ArrayList<Player> placeEggsPlayers;
    private HashMap<Player, Integer> playerAddCommand;
    private ArrayList<ArmorStand> showedArmorstands;
    public YamlConfiguration messages;
    public File messagesData;
    private HashMap<Player, LeaderboardSortTypes> sortTypeLeaderboard;
    private InventoryRequirementsManager inventoryRequirementsManager;
    private CooldownManager cooldownManager;

    private EggDataManager eggDataManager;
    private EggManager eggManager;
    private SoundManager soundManager;
    private ExtraManager extraManager;
    private InventoryManager inventoryManager;
    private PlayerEggDataManager playerEggDataManager;
    private RequirementsManager requirementsManager;
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
        initManagers();
        getCommand("advancedegghunt").setExecutor(new AdvancedEggHuntCommand());
        initListeners();
        datetimeUtils = new DatetimeUtils();
        cooldownManager = new CooldownManager(this);
        eggManager.spawnEggParticle();
        checkCommandFeedback();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Bukkit.getConsoleSender().sendMessage(messages.getString("Prefix").replaceAll("&","§")+"§aAdvanced Egg Hunt detected PlaceholderAPI, enabling placeholders.");
            new PlaceholderExtension().register();
            Bukkit.getConsoleSender().sendMessage(messages.getString("Prefix").replaceAll("&","§")+"§2§lAll placeholders successfully enabled.");
        }
        getEggManager().convertEggData();
        initData();
    }

    @Override
    public void onDisable() {
        giveAllItemsBack();
        for(ArmorStand a : showedArmorstands){
            a.remove();
        }
        getConfig().set("Edit",null);
        saveConfig();
    }

    private void initData(){
        playerEggDataManager.initPlayers();
        Bukkit.getConsoleSender().sendMessage("§2§l" +
                "Loaded data of "+eggDataManager.savedPlayers().size()+" player(s).");
        eggDataManager.initEggs();
        Bukkit.getConsoleSender().sendMessage("§2§lLoaded data of "+eggDataManager.savedEggSections().size()+" collection(s).");
        for(String section : getEggDataManager().savedEggSections())
            eggManager.updateMaxEggs(section);
    }

    private void initManagers(){
        eggDataManager = new EggDataManager(this);
        eggManager = new EggManager();
        inventoryManager = new InventoryManager();
        soundManager = new SoundManager();
        extraManager = new ExtraManager();
        playerEggDataManager = new PlayerEggDataManager();
        inventoryRequirementsManager = new InventoryRequirementsManager();
        requirementsManager = new RequirementsManager();
    }

    public void checkCommandFeedback(){
        if(XMaterial.supports(9)) {
            if (getConfig().getBoolean("Settings.DisableCommandFeedback")) {
                for (World worlds : Bukkit.getServer().getWorlds())
                    worlds.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
            } else {
                for (World worlds : Bukkit.getServer().getWorlds())
                    worlds.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, true);
            }
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
        new RequirementsListeners(this);
    }

    private void giveAllItemsBack(){
        for(Player player : Bukkit.getServer().getOnlinePlayers()){
            if(placeEggsPlayers.contains(player)){
                eggManager.finishEggPlacing(player);
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
        checkUpdatePath();
    }

    private void checkUpdatePath(){
        if(messages.getDouble("version") < 2.3){
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

    public XMaterial getMaterial(String materialString) {
        try {
            XMaterial material = XMaterial.valueOf(materialString);
            if (material == null) {
                return XMaterial.BARRIER;
            }
            return material;
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage("§4Material Error: "+ex);
            return XMaterial.STONE;
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
            return HexColor.color(Objects.requireNonNull(messages.getString("Prefix"))+messages.getString(message));
        }else
            return HexColor.color(Objects.requireNonNull(messages.getString(message)));
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

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public EggDataManager getEggDataManager() {
        return eggDataManager;
    }

    public EggManager getEggManager() {
        return eggManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public ExtraManager getExtraManager() {
        return extraManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public PlayerEggDataManager getPlayerEggDataManager() {
        return new PlayerEggDataManager();
    }

    public static HashMap<Player, PlayerMenuUtility> getPlayerMenuUtilityMap() {
        return playerMenuUtilityMap;
    }

    public InventoryRequirementsManager getInventoryRequirementsManager() {
        return inventoryRequirementsManager;
    }

    public RequirementsManager getRequirementsManager() {
        return requirementsManager;
    }
}
