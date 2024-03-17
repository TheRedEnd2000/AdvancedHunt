package de.theredend2000.advancedegghunt;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.bstats.Metrics;
import de.theredend2000.advancedegghunt.commands.AdvancedEggHuntCommand;
import de.theredend2000.advancedegghunt.configurations.PluginConfig;
import de.theredend2000.advancedegghunt.listeners.*;
import de.theredend2000.advancedegghunt.listeners.inventoryListeners.RequirementsListeners;
import de.theredend2000.advancedegghunt.listeners.inventoryListeners.ResetListeners;
import de.theredend2000.advancedegghunt.managers.CooldownManager;
import de.theredend2000.advancedegghunt.managers.PermissionManager.PermissionManager;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggDataManager;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedegghunt.managers.eggmanager.PlayerEggDataManager;
import de.theredend2000.advancedegghunt.managers.extramanager.ExtraManager;
import de.theredend2000.advancedegghunt.managers.extramanager.RequirementsManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.InventoryManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.InventoryRequirementsManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.ResetInventoryManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.EggRewardsInventory;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.presets.PresetDataManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.presets.PresetsInventory;
import de.theredend2000.advancedegghunt.managers.soundmanager.SoundManager;
import de.theredend2000.advancedegghunt.placeholderapi.PlaceholderExtension;
import de.theredend2000.advancedegghunt.util.HexColor;
import de.theredend2000.advancedegghunt.util.Updater;
import de.theredend2000.advancedegghunt.util.enums.LeaderboardSortTypes;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import de.theredend2000.advancedegghunt.util.saveinventory.DatetimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private HashMap<Player, LeaderboardSortTypes> sortTypeLeaderboard;
    private InventoryRequirementsManager inventoryRequirementsManager;
    private PluginConfig pluginConfig;
    private CooldownManager cooldownManager;
    private EggDataManager eggDataManager;
    private EggManager eggManager;
    private SoundManager soundManager;
    private ExtraManager extraManager;
    private InventoryManager inventoryManager;
    private PlayerEggDataManager playerEggDataManager;
    private RequirementsManager requirementsManager;
    private ResetInventoryManager resetInventoryManager;
    private PermissionManager permissionManager;
    private ResetListeners resetListeners;
    private EggRewardsInventory eggRewardsInventory;
    private PresetsInventory presetsInventory;
    private PresetDataManager presetDataManager;
    private MessageManager messageManager;
    public static String PREFIX = "";
    public static boolean setupDefaultCollection;
    @Override
    public void onEnable() {
        plugin = this;
        setupDefaultCollection = false;
        PREFIX = HexColor.color(ChatColor.translateAlternateColorCodes('&', pluginConfig.getPrefix()));
        Metrics metrics = new Metrics(this, 19495);
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
        checkCommandFeedback();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Bukkit.getConsoleSender().sendMessage(PREFIX + "§aAdvanced Egg Hunt detected PlaceholderAPI, enabling placeholders.");
            new PlaceholderExtension().register();
            Bukkit.getConsoleSender().sendMessage(PREFIX + "§2§lAll placeholders successfully enabled.");
        }
        getEggManager().convertEggData();
        initData();
        sendCurrentLanguage();
        if(setupDefaultCollection) {
            getRequirementsManager().changeActivity("default", true);
            getRequirementsManager().resetReset("default");
        }
        playerEggDataManager.checkReset();
        eggManager.spawnEggParticle();
    }

    @Override
    public void onDisable() {
        giveAllItemsBack();
        for(ArmorStand a : showedArmorstands){
            a.remove();
        }
        getConfig().set("Edit", null);
        saveConfig();
    }

    private void initData(){
        List<String > collections = eggDataManager.savedEggCollections();
        playerEggDataManager.initPlayers();
        Bukkit.getConsoleSender().sendMessage("§2§l" +
                "Loaded data of " + collections.size() + " player(s).");
        eggDataManager.initEggs();
        Bukkit.getConsoleSender().sendMessage("§2§lLoaded data of " + collections.size() + " collection(s).");
        for(String collection : collections)
            eggManager.updateMaxEggs(collection);
    }

    private void initManagers(){
        messageManager = new MessageManager();
        eggDataManager = new EggDataManager(this);
        eggManager = new EggManager();
        inventoryManager = new InventoryManager();
        soundManager = new SoundManager();
        extraManager = new ExtraManager();
        playerEggDataManager = new PlayerEggDataManager();
        inventoryRequirementsManager = new InventoryRequirementsManager();
        requirementsManager = new RequirementsManager();
        resetInventoryManager = new ResetInventoryManager();
        permissionManager = new PermissionManager();
        eggRewardsInventory = new EggRewardsInventory();
        presetDataManager = new PresetDataManager(this);
        presetsInventory = new PresetsInventory();
    }

    public void checkCommandFeedback(){
        if (!pluginConfig.getForcedCommandFeedback()) return;
        if (pluginConfig.getDisableCommandFeedback()) {
            for (World worlds : Bukkit.getServer().getWorlds())
                worlds.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
        } else {
            for (World worlds : Bukkit.getServer().getWorlds())
                worlds.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, true);
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
        new RequirementsListeners(this);
        new ResetListeners(this);
    }

    private void giveAllItemsBack(){
        for(Player player : Bukkit.getServer().getOnlinePlayers()){
            if(placeEggsPlayers.contains(player)){
                eggManager.finishEggPlacing(player);
            }
        }
    }

    private void sendCurrentLanguage(){
        String lang = pluginConfig.getLanguage();
        Bukkit.getConsoleSender().sendMessage(PREFIX + "§7Language §6" + lang + " §7detected. File messages-" + lang + ".yml loaded.");
    }

    private void setupConfigs(){
        pluginConfig = PluginConfig.getInstance(plugin);
    }

    public void saveMessages() {
        try {
            this.messages.save(this.messagesData);
        } catch (IOException e) {
            getLogger().severe(e.getMessage());
        }
    }

    public static XMaterial getMaterial(String materialString) {
        try {
            XMaterial material = XMaterial.valueOf(materialString);
            if (material == null) {
                return XMaterial.BARRIER;
            }
            return material;
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage("§4Material Error: " + ex);
            return XMaterial.STONE;
        }
    }


    public static Main getInstance() {
        return plugin;
    }

    public PluginConfig getPluginConfig(){
        return pluginConfig;
    }

    public static String getTexture(String texture){
        String prefix = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUv";
        texture = prefix + texture;
        return texture;
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
        return playerEggDataManager;
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

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public ResetInventoryManager getResetInventoryManager() {
        return resetInventoryManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public EggRewardsInventory getEggRewardsInventory() {
        return eggRewardsInventory;
    }

    public PresetDataManager getPresetDataManager() {
        return presetDataManager;
    }

    public PresetsInventory getPresetsInventory() {
        return presetsInventory;
    }
}
