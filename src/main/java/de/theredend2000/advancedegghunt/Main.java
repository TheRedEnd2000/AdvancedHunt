package de.theredend2000.advancedegghunt;

import com.comphenix.protocol.ProtocolManager;
import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.bstats.Metrics;
import de.theredend2000.advancedegghunt.commands.AdvancedEggHuntCommand;
import de.theredend2000.advancedegghunt.configurations.PluginConfig;
import de.theredend2000.advancedegghunt.listeners.*;
import de.theredend2000.advancedegghunt.managers.*;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggDataManager;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggHidingManager;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedegghunt.managers.eggmanager.PlayerEggDataManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.RarityManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.global.GlobalPresetDataManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.individual.IndividualPresetDataManager;
import de.theredend2000.advancedegghunt.placeholderapi.PlaceholderExtension;
import de.theredend2000.advancedegghunt.util.*;
import de.theredend2000.advancedegghunt.util.embed.EmbedCreator;
import de.theredend2000.advancedegghunt.util.enums.LeaderboardSortTypes;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import de.theredend2000.advancedegghunt.util.saveinventory.DatetimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public final class Main extends JavaPlugin {

    // Static fields
    private static Main plugin;
    public static String PREFIX = "";
    public static boolean setupDefaultCollection;

    // Configuration
    private PluginConfig pluginConfig;
    public YamlConfiguration messages;
    public File messagesData;

    // Managers
    private CooldownManager cooldownManager;
    private EggDataManager eggDataManager;
    private EggManager eggManager;
    private SoundManager soundManager;
    private ExtraManager extraManager;
    private PlayerEggDataManager playerEggDataManager;
    private RequirementsManager requirementsManager;
    private PermissionManager permissionManager;
    private IndividualPresetDataManager individualPresetDataManager;
    private GlobalPresetDataManager globalPresetDataManager;
    private MessageManager messageManager;
    private RarityManager rarityManager;
    private EggHidingManager eggHidingManager;

    // Utility classes
    private DatetimeUtils datetimeUtils;
    private EmbedCreator embedCreator;
    private ProtocolManager protocolManager;

    // Collections
    private HashMap<String, Long> refreshCooldown;
    private ArrayList<Player> placeEggsPlayers;
    private HashMap<Player, Integer> playerAddCommand;
    private ArrayList<ArmorStand> showedArmorstands;
    private HashMap<Player, LeaderboardSortTypes> sortTypeLeaderboard;
    private static final HashMap<Player, PlayerMenuUtility> playerMenuUtilityMap = new HashMap<>();

    @Override
    public void onEnable() {
        plugin = this;
        initializePlugin();
        setupAutoUpdating();

        if (!checkDependencies())
            return;

        setupManagers();
        registerCommands();
        initListeners();
        setupPlaceholderAPI();
        initializeData();
        setupDefaultCollectionIfNeeded();
        finalizeSetup();
    }

    /**
     * Checks if all required plugin dependencies are present and enabled.
     *
     * @return true if all dependencies are present and enabled, false otherwise
     */
    private boolean checkDependencies() {
        List<String> missingDependencies = new ArrayList<>();

        // List of required plugin names
        String[] requiredPlugins = {"NBTAPI"};

        for (String pluginName : requiredPlugins) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
            if (plugin == null || !plugin.isEnabled()) {
                missingDependencies.add(pluginName);
            }
        }

        if (!missingDependencies.isEmpty()) {
            getLogger().log(Level.SEVERE, "Missing required dependencies: " + String.join(", ", missingDependencies));
            plugin.setEnabled(false);
            return false;
        }

        return true;
    }

    /**
     * Checks for soft dependencies and logs their status.
     */
    private void checkSoftDependencies() {
        List<String> missingDependencies = new ArrayList<>();
        List<String> presentDependencies = new ArrayList<>();

        // List of soft dependency plugin names
        String[] softDependencies = {"PlaceholderAPI", "ProtocolLib"};

        for (String pluginName : softDependencies) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
            if (plugin == null || !plugin.isEnabled()) {
                missingDependencies.add(pluginName);
            } else {
                presentDependencies.add(pluginName);
            }
        }

        if (!missingDependencies.isEmpty()) {
            getLogger().log(Level.WARNING, "Some soft dependencies are missing: " + String.join(", ", missingDependencies));
            getLogger().log(Level.WARNING, "The plugin will continue to run, but some features may be unavailable.");
        }

        if (!presentDependencies.isEmpty()) {
            getLogger().log(Level.INFO, "Detected soft dependencies: " + String.join(", ", presentDependencies));
        }
    }

    private void initializePlugin() {
        setupConfigs();
        setupDefaultCollection = false;
        PREFIX = HexColor.color(ChatColor.translateAlternateColorCodes('&', pluginConfig.getPrefix()));
        new Metrics(this, 19495);
        initializeCollections();
    }

    private void initializeCollections() {
        refreshCooldown = new HashMap<>();
        placeEggsPlayers = new ArrayList<>();
        showedArmorstands = new ArrayList<>();
        playerAddCommand = new HashMap<>();
        sortTypeLeaderboard = new HashMap<>();
    }

    private void setupManagers() {
        initManagers();
        datetimeUtils = new DatetimeUtils();
        cooldownManager = new CooldownManager(this);
    }

    private void registerCommands() {
        getCommand("advancedegghunt").setExecutor(new AdvancedEggHuntCommand());
    }


    private void setupPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Bukkit.getConsoleSender().sendMessage(PREFIX + "§aAdvanced Egg Hunt detected PlaceholderAPI, enabling placeholders.");
            new PlaceholderExtension().register();
            Bukkit.getConsoleSender().sendMessage(PREFIX + "§2§lAll placeholders successfully enabled.");
        }
    }

    private void initializeData() {
        getEggManager().convertEggData();
        initData();
        sendCurrentLanguage();
    }

    private void setupDefaultCollectionIfNeeded() {
        if (setupDefaultCollection) {
            getRequirementsManager().changeActivity("default", true);
            getRequirementsManager().resetReset("default");
            getGlobalPresetDataManager().loadPresetIntoCollectionCommands(getPluginConfig().getDefaultGlobalLoadingPreset(), "default");
        }
    }

    private void finalizeSetup() {
        playerEggDataManager.checkReset();
        eggManager.spawnEggParticle();
        new Converter().convertAllSystems();

        for (Player player : Bukkit.getOnlinePlayers()) {
            getPlayerEggDataManager().createPlayerFile(player.getUniqueId());
        }
    }

    @Override
    public void onDisable() {
        giveAllItemsBack();
        removeAllArmorStands();
    }

    private void removeAllArmorStands() {
        showedArmorstands.forEach(ArmorStand::remove);
    }

    private void initData(){
        List<String > eggCollections = eggDataManager.savedEggCollections();
        List<UUID> playerCollection = eggDataManager.savedPlayers();
        playerEggDataManager.initPlayers();
        Bukkit.getConsoleSender().sendMessage("§2§l" +
                "Loaded data of " + playerCollection.size() + " player(s).");
        eggDataManager.initEggs();
        Bukkit.getConsoleSender().sendMessage("§2§lLoaded data of " + eggCollections.size() + " collection(s).");
        for(String collection : eggCollections)
            eggManager.updateMaxEggs(collection);
    }

    private void initManagers(){
        individualPresetDataManager = new IndividualPresetDataManager(this);
        globalPresetDataManager = new GlobalPresetDataManager(this);
        messageManager = new MessageManager();
        eggDataManager = new EggDataManager(this);
        eggManager = new EggManager();
        soundManager = new SoundManager();
        extraManager = new ExtraManager();
        playerEggDataManager = new PlayerEggDataManager();
        requirementsManager = new RequirementsManager();
        permissionManager = new PermissionManager();
        rarityManager = new RarityManager();
        embedCreator = new EmbedCreator();
//        protocolManager = ProtocolLibrary.getProtocolManager();
        eggHidingManager = new EggHidingManager();
        new Checker();
    }

    private void initListeners() {
        new InventoryClickEventListener();
        new InventoryCloseEventListener();
        new BlockPlaceEventListener();
        new BlockBreakEventListener();
        new PlayerInteractEventListener();
        new PlayerInteractItemEvent();
        new Updater(this);
        new PlayerChatEventListener();
        new PlayerConnectionListener();
        new EntityChangeListener();
    }

    private void setupAutoUpdating() {
        PluginDownloader downloader = new PluginDownloader(plugin);

        if (plugin.getPluginConfig().getAutoDownloadAdvancedEggHunt())
            downloader.downloadPlugin("109085", "AdvancedEggHunt", "spigot");
        if (plugin.getPluginConfig().getAutoDownloadPlaceholderAPI())
            downloader.downloadPlugin("6245", "PlaceholderAPI", "spigot");
        if (plugin.getPluginConfig().getAutoDownloadProtocolLib())
            downloader.downloadPlugin("1997", "ProtocolLib", "spigot");
        if (plugin.getPluginConfig().getAutoDownloadNBTAPI())
            downloader.downloadPlugin("nfGCP9fk", "NBTAPI", "modrinth");
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
            return Optional.ofNullable(XMaterial.valueOf(materialString))
                    .orElse(XMaterial.BARRIER);
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage("§4Material Error: " + ex);
            return XMaterial.STONE;
        }
    }

    public static Main getInstance() {
        return plugin;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public static String getTexture(String texture) {
        return "eyJ0ZXh0dXJlcy" +
                "I6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUv" + texture;
    }

    public static PlayerMenuUtility getPlayerMenuUtility(Player p) {
        return playerMenuUtilityMap.computeIfAbsent(p, PlayerMenuUtility::new);
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

    public RequirementsManager getRequirementsManager() {
        return requirementsManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public IndividualPresetDataManager getIndividualPresetDataManager() {
        return individualPresetDataManager;
    }

    public GlobalPresetDataManager getGlobalPresetDataManager() {
        return globalPresetDataManager;
    }

    public RarityManager getRarityManager() {
        return rarityManager;
    }

    public EmbedCreator getEmbedCreator() {
        return embedCreator;
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    public EggHidingManager getEggHidingManager() {
        return eggHidingManager;
    }
}
