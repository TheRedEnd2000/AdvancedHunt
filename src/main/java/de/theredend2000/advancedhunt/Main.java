package de.theredend2000.advancedhunt;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.bstats.Metrics;
import de.theredend2000.advancedhunt.commands.AdvancedHuntCommand;
import de.theredend2000.advancedhunt.configurations.PluginConfig;
import de.theredend2000.advancedhunt.listeners.*;
import de.theredend2000.advancedhunt.managers.*;
import de.theredend2000.advancedhunt.managers.eggmanager.EggDataManager;
import de.theredend2000.advancedhunt.managers.eggmanager.EggHidingManager;
import de.theredend2000.advancedhunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedhunt.managers.eggmanager.PlayerEggDataManager;
import de.theredend2000.advancedhunt.managers.inventorymanager.eggrewards.RarityManager;
import de.theredend2000.advancedhunt.managers.inventorymanager.eggrewards.global.GlobalPresetDataManager;
import de.theredend2000.advancedhunt.managers.inventorymanager.eggrewards.individual.IndividualPresetDataManager;
import de.theredend2000.advancedhunt.placeholderapi.PlaceholderExtension;
import de.theredend2000.advancedhunt.util.*;
import de.theredend2000.advancedhunt.util.embed.EmbedCreator;
import de.theredend2000.advancedhunt.util.enums.LeaderboardSortTypes;
import de.theredend2000.advancedhunt.util.messages.MenuManager;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageManager;
import de.theredend2000.advancedhunt.util.saveinventory.DatetimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public final class Main extends JavaPlugin {

    // Static fields
    private static Main plugin;
    public static String PREFIX = "";
    public static boolean setupDefaultCollection;

    // Configuration
    private PluginConfig pluginConfig;

    private DynamicCommandRegistrar commandRegistrar;

    // Managers
    private MessageManager messageManager;
    private MenuManager menuMessageManager;
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
    private RarityManager rarityManager;
    private EggHidingManager eggHidingManager;

    // Utility classes
    private DatetimeUtils datetimeUtils;
    private EmbedCreator embedCreator;
    private ProtocolManager protocolManager;

    // Collections
    private HashMap<String, Long> refreshCooldown;
    private ArrayList<Player> placePlayers;
    private HashMap<Player, Integer> playerAddCommand;
    private ArrayList<ArmorStand> showedArmorstands;
    private HashMap<Player, LeaderboardSortTypes> sortTypeLeaderboard;
    private static final HashMap<Player, PlayerMenuUtility> playerMenuUtilityMap = new HashMap<>();
    private HashMap<Player, Inventory>  lastOpenedInventory = new HashMap<>();

    @Override
    public void onEnable() {
        plugin = this;
        renameConfigFolder();
        initialisePlugin();

        String version = Bukkit.getBukkitVersion().split("-", 2)[0];
        if (VersionComparator.isGreaterThan(version, "1.21")) {
            this.getLogger().warning("The plugin has not been tested on the current version.");
        }

        setupAutoUpdating();

        if (!checkDependencies())
            return;

        setupManagers();
        registerCommands();
        initListeners();
        initialiseData();
        setupDefaultCollectionIfNeeded();
        finalizeSetup();
        setupPlaceholderAPI();
    }

    @Override
    public void onDisable() {
        commandRegistrar.unregisterCommands();
        giveAllItemsBack();
        removeAllArmorStands();
    }

    private void renameConfigFolder() {
        File oldFolder = new File(getDataFolder().getParentFile(), "AdvancedEggHunt");
        File newFolder = new File(getDataFolder().getParentFile(), "AdvancedHunt");

        if (oldFolder.exists() && !newFolder.exists()) {
            boolean success = oldFolder.renameTo(newFolder);
            if (success) {
                getLogger().log(Level.INFO, "Folder 'AdvancedHunt' successfully renamed to 'AdvancedHunt'.");
            } else {
                getLogger().log(Level.SEVERE, "There was an error renaming 'AdvancedHunt'.");
            }
        }
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
        checkSoftDependency("PlaceholderAPI");
        checkSoftDependency("ProtocolLib");
    }

    private void checkSoftDependency(String pluginName) {
        Plugin dependency = Bukkit.getPluginManager().getPlugin(pluginName);
        if (dependency != null && dependency.isEnabled()) {
            getLogger().log(Level.INFO, "Detected soft dependency: " + pluginName);
            initialiseSoftDependency(pluginName);
        } else {
            getLogger().log(Level.WARNING, "Soft dependency not found or not enabled: " + pluginName);
            getLogger().log(Level.WARNING, "Some features related to " + pluginName + " may be unavailable.");
        }
    }

    private void initialiseSoftDependency(String pluginName) {
        switch (pluginName) {
            case "ProtocolLib":
                initialiseProtocolLib();
                break;
            default:
                getLogger().log(Level.INFO, "No specific initialization needed for " + pluginName);
        }
    }

    private void initialiseProtocolLib() {
        if (pluginConfig.isProtocolLibSupportEnabled()) {
            protocolManager = ProtocolLibrary.getProtocolManager();
            if (protocolManager != null) {
                getLogger().log(Level.INFO, "ProtocolLib support initialised successfully.");
            } else {
                getLogger().log(Level.WARNING, "Failed to initialize ProtocolLib support.");
            }
        } else {
            getLogger().log(Level.INFO, "ProtocolLib support is disabled in the config.");
        }
    }

    private void initialisePlugin() {
        setupConfigs();
        setupDefaultCollection = false;
        PREFIX = HexColor.color(pluginConfig.getPrefix());
        new Metrics(this, 19495);
        commandRegistrar = new DynamicCommandRegistrar(this);
        initialiseCollections();
    }

    private void initialiseCollections() {
        refreshCooldown = new HashMap<>();
        placePlayers = new ArrayList<>();
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
        String commandName = plugin.getPluginConfig().getCommandFirstEntry();
        List<String> aliases = plugin.getPluginConfig().getCommandAlias();

        commandRegistrar.command(commandName)
                .aliases(aliases)
                .tabExecuter(new AdvancedHuntCommand())
                .register();
    }


    private void setupPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && Bukkit.getPluginManager().getPlugin("PlaceholderAPI").isEnabled()) {
            messageManager.sendMessage(Bukkit.getConsoleSender(), MessageKey.PLACEHOLDERAPI_DETECTED);
            new PlaceholderExtension().register();
            messageManager.sendMessage(Bukkit.getConsoleSender(), MessageKey.PLACEHOLDERAPI_ENABLED);
        }
    }

    private void initialiseData() {
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

        for (Player player : Bukkit.getOnlinePlayers()) {
            getPlayerEggDataManager().createPlayerFile(player.getUniqueId());
        }
    }

    private void removeAllArmorStands() {
        showedArmorstands.forEach(ArmorStand::remove);
    }

    private void initData(){
        List<String> eggCollections = eggDataManager.savedEggCollections();
        List<UUID> playerCollection = eggDataManager.savedPlayers();
        playerEggDataManager.initPlayers();
        messageManager.sendMessage(Bukkit.getConsoleSender(), MessageKey.INIT_DATA_PLAYERS_LOADED, "%COUNT%", String.valueOf(playerCollection.size()));
        eggDataManager.initEggs();
        messageManager.sendMessage(Bukkit.getConsoleSender(), MessageKey.INIT_DATA_COLLECTIONS_LOADED, "%COUNT%", String.valueOf(eggCollections.size()));
        for(String collection : eggCollections)
            eggManager.updateMaxEggs(collection);
    }

    private void initManagers(){
        individualPresetDataManager = new IndividualPresetDataManager(this);
        globalPresetDataManager = new GlobalPresetDataManager(this);
        messageManager = new MessageManager();
        menuMessageManager = new MenuManager();
        eggDataManager = new EggDataManager(this);
        eggManager = new EggManager();
        soundManager = new SoundManager();
        extraManager = new ExtraManager();
        playerEggDataManager = new PlayerEggDataManager();
        requirementsManager = new RequirementsManager();
        permissionManager = new PermissionManager();
        rarityManager = new RarityManager();
        embedCreator = new EmbedCreator();
        checkSoftDependencies();
        eggHidingManager = new EggHidingManager();
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
        new EntityDamageEventListener();
    }

    private void setupAutoUpdating() {
        PluginDownloader downloader = new PluginDownloader(plugin);

        if (plugin.getPluginConfig().getAutoDownloadAdvancedHunt())
            downloader.downloadPlugin("109085", "AdvancedHunt", "spigot");
        if (plugin.getPluginConfig().getAutoDownloadPlaceholderAPI())
            downloader.downloadPlugin("6245", "PlaceholderAPI", "spigot");
        if (plugin.getPluginConfig().getAutoDownloadProtocolLib())
            downloader.downloadPlugin("1997", "ProtocolLib", "spigot");
        if (plugin.getPluginConfig().getAutoDownloadNBTAPI())
            downloader.downloadPlugin("nfGCP9fk", "NBTAPI", "modrinth");
    }

    private void giveAllItemsBack(){
        for(Player player : Bukkit.getServer().getOnlinePlayers()){
            if(placePlayers.contains(player)){
                eggManager.finishEggPlacing(player);
            }
        }
    }

    private void sendCurrentLanguage() {
        String lang = pluginConfig.getLanguage();
        messageManager.sendMessage(Bukkit.getConsoleSender(), MessageKey.LANGUAGE_DETECTED, "%LANG%", lang);
    }

    private void setupConfigs(){
        pluginConfig = PluginConfig.getInstance(plugin);
    }

    public XMaterial getMaterial(String materialString) {
        try {
            return Optional.ofNullable(XMaterial.valueOf(materialString))
                    .orElse(XMaterial.BARRIER);
        } catch (Exception ex) {
            messageManager.sendMessage(Bukkit.getConsoleSender(), MessageKey.MATERIAL_ERROR_CONSOLE, "%ERROR%", ex.getMessage());
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
        return "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUv" + texture;
    }

    public static PlayerMenuUtility getPlayerMenuUtility(Player p) {
        return playerMenuUtilityMap.computeIfAbsent(p, PlayerMenuUtility::new);
    }

    public ArrayList<Player> getPlacePlayers() {
        return placePlayers;
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

    public MenuManager getMenuManager() {
        return menuMessageManager;
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

    public boolean isProtocolLibEnabled() {
        return protocolManager != null;
    }

    public EggHidingManager getEggHidingManager() {
        return eggHidingManager;
    }

    public Inventory getLastOpenedInventory(Player player) {
        return lastOpenedInventory.get(player);
    }

    public void setLastOpenedInventory(Inventory lastOpenedInventory, Player player) {
        this.lastOpenedInventory.remove(player);
        this.lastOpenedInventory.put(player, lastOpenedInventory);
    }
}
