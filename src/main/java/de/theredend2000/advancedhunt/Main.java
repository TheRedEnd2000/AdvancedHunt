package de.theredend2000.advancedhunt;

import de.theredend2000.advancedhunt.commands.AdvancedHuntCommand;
import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.data.SqlRepository;
import de.theredend2000.advancedhunt.data.YamlRepository;
import de.theredend2000.advancedhunt.listeners.*;
import de.theredend2000.advancedhunt.managers.*;
import de.theredend2000.advancedhunt.managers.minigame.MinigameManager;
import de.theredend2000.advancedhunt.placeholder.AdvancedHuntExpansion;
import de.theredend2000.advancedhunt.util.ConfigMigrationHandler;
import de.theredend2000.advancedhunt.util.ConfigUpdater;
import de.theredend2000.advancedhunt.util.updater.PluginUpdater;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class Main extends JavaPlugin {

    private LegacyPaperCommandManager<CommandSender> commandManager;
    private DataRepository dataRepository;
    private TreasureManager treasureManager;
    private PlayerManager playerManager;
    private LeaderboardManager leaderboardManager;
    private CollectionManager collectionManager;
    private RewardManager rewardManager;
    private PlaceModeManager placeModeManager;
    private ParticleManager particleManager;
    private MigrationService migrationService;
    private MessageManager messageManager;
    private PluginUpdater pluginUpdater;
    private ChatInputListener chatInputListener;
    private MinigameManager minigameManager;
    private SoundManager soundManager;
    private ProximityManager proximityManager;
    private ScanManager scanManager;
    private HintManager hintManager;
    private FireworkManager fireworkManager;

    private Random random;

    @Override
    public void onEnable() {
        random = new Random();
        saveDefaultConfig();
        ConfigUpdater.update(this, "config.yml", new File(getDataFolder(), "config.yml"), ConfigMigrationHandler::migrateConfig);
        reloadConfig();

        migrationService = new MigrationService(getLogger());
        // Initialize Message Manager
        messageManager = new MessageManager(this);
        soundManager = new SoundManager(this);

        // Initialize Data Repository
        String storageType = getConfig().getString("storage.type", "YAML");
        if (storageType.equalsIgnoreCase("MYSQL")) {
            dataRepository = new SqlRepository(
                    this,
                    getConfig().getString("storage.mysql.host"),
                    getConfig().getInt("storage.mysql.port"),
                    getConfig().getString("storage.mysql.database"),
                    getConfig().getString("storage.mysql.username"),
                    getConfig().getString("storage.mysql.password"),
                    false
            );
        } else if (storageType.equalsIgnoreCase("SQLITE")) {
            dataRepository = new SqlRepository(this, null, 0, null, null, null, true);
        } else {
            dataRepository = new YamlRepository(this);
        }
        dataRepository.init();
        
        // Start periodic index flush for YAML backend (every 60 seconds)
        if (dataRepository instanceof YamlRepository) {
            ((YamlRepository) dataRepository).startPeriodicFlush(60);
        }

        // Initialize Managers
        rewardManager = new RewardManager(this);
        treasureManager = new TreasureManager(dataRepository);
        treasureManager.loadTreasures();

        playerManager = new PlayerManager(this, dataRepository);
        leaderboardManager = new LeaderboardManager(this, dataRepository);
        collectionManager = new CollectionManager(this, dataRepository, treasureManager, playerManager, rewardManager);
        placeModeManager = new PlaceModeManager(this);
        minigameManager = new MinigameManager(this);
        fireworkManager = new FireworkManager(this);
        particleManager = new ParticleManager(this, treasureManager, playerManager, collectionManager);
        hintManager = new HintManager(this, treasureManager, collectionManager, playerManager, messageManager, particleManager);
        particleManager.start();

        proximityManager = new ProximityManager(this, treasureManager, playerManager);

        scanManager = new ScanManager(this, collectionManager, proximityManager, particleManager,placeModeManager);
        scanManager.start();

        // Register Listeners
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlaceModeListener(this), this);
        getServer().getPluginManager().registerEvents(new TreasureProtectListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerProtectionListener(this), this);
        chatInputListener = new ChatInputListener(this);
        getServer().getPluginManager().registerEvents(chatInputListener, this);

        // Register Placeholders
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AdvancedHuntExpansion(this).register();
        }

        // Initialize Commands
        setupCommands();

        // Initialize bStats
        if (!getConfig().getBoolean("dev-mode")) {
            int pluginId = 19495;
            new Metrics(this, pluginId);
        }

        // Initialize Updater
        if (getConfig().getBoolean("updater.enabled", true)) {
            pluginUpdater = new PluginUpdater(this);
            
            // Track Main Plugin
            Map<String, String> mainIds = new HashMap<>();
            if (getConfig().isConfigurationSection("updater.sources")) {
                ConfigurationSection sources = getConfig().getConfigurationSection("updater.sources");
                for (String key : sources.getKeys(false)) {
                    mainIds.put(key, sources.getString(key));
                }
            }
            pluginUpdater.trackPlugin(getDescription().getName(), getDescription().getVersion(), mainIds);

            // Track Dependencies
            if (getConfig().isConfigurationSection("updater.dependencies")) {
                ConfigurationSection deps = getConfig().getConfigurationSection("updater.dependencies");
                for (String depName : deps.getKeys(false)) {
                    if (deps.getBoolean(depName + ".enabled")) {
                        Map<String, String> depIds = new HashMap<>();
                        if (deps.isConfigurationSection(depName + ".sources")) {
                            ConfigurationSection depSources = deps.getConfigurationSection(depName + ".sources");
                            for (String key : depSources.getKeys(false)) {
                                depIds.put(key, depSources.getString(key));
                            }
                        }
                        
                        Plugin dep = Bukkit.getPluginManager().getPlugin(depName);
                        String currentVersion = (dep != null) ? dep.getDescription().getVersion() : "0.0.0";
                        
                        pluginUpdater.trackPlugin(depName, currentVersion, depIds);
                    }
                }
            }

            // Check for updates asynchronously
            getServer().getScheduler().runTaskAsynchronously(this, () -> pluginUpdater.checkForUpdates());
        }
    }

    private void setupCommands() {
        ExecutionCoordinator<CommandSender> executionCoordinator = ExecutionCoordinator.simpleCoordinator();

        commandManager = LegacyPaperCommandManager.createNative(
                this,
                executionCoordinator
        );

        if (!getConfig().getBoolean("dev-mode") && commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            commandManager.registerBrigadier();
        } else if (commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            commandManager.registerAsynchronousCompletions();
        }

        new AdvancedHuntCommand(this).register(commandManager);
    }

    @Override
    public void onDisable() {
        if (hintManager != null) {
            hintManager.cancelAllVisualHints();
        }
        if (scanManager != null) {
            scanManager.stop();
        }
        if (particleManager != null) {
            particleManager.stop();
        }
        if (dataRepository != null) {
            dataRepository.shutdown();
        }
    }

    public TreasureManager getTreasureManager() {
        return treasureManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public CollectionManager getCollectionManager() {
        return collectionManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public PlaceModeManager getPlaceModeManager() {
        return placeModeManager;
    }

    public ParticleManager getParticleManager() {
        return particleManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public MigrationService getMigrationService() {
        return migrationService;
    }

    public DataRepository getDataRepository() {
        return dataRepository;
    }

    public ChatInputListener getChatInputListener() {
        return chatInputListener;
    }

    public MinigameManager getMinigameManager() {
        return minigameManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public ProximityManager getProximityManager() {
        return proximityManager;
    }

    public ScanManager getScanManager() {
        return scanManager;
    }

    public HintManager getHintManager() {
        return hintManager;
    }

    public Random getRandom() {
        return random;
    }

    public FireworkManager getFireworkManager() {
        return fireworkManager;
    }
}
