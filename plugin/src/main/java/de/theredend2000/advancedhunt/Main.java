package de.theredend2000.advancedhunt;

import de.theredend2000.advancedhunt.commands.AdvancedHuntCommand;
import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.data.SqlRepository;
import de.theredend2000.advancedhunt.data.YamlRepository;
import de.theredend2000.advancedhunt.listeners.*;
import de.theredend2000.advancedhunt.managers.*;
import de.theredend2000.advancedhunt.managers.minigame.MinigameManager;
import de.theredend2000.advancedhunt.migration.legacy.LegacyDataMigrator;
import de.theredend2000.advancedhunt.migration.legacy.LegacyMigrationConfig;
import de.theredend2000.advancedhunt.placeholder.AdvancedHuntExpansion;
import de.theredend2000.advancedhunt.util.ConfigMigrationHandler;
import de.theredend2000.advancedhunt.util.ConfigUpdater;
import de.theredend2000.advancedhunt.util.ItemsAdderAdapter;
import de.theredend2000.advancedhunt.util.updater.PluginUpdater;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class Main extends JavaPlugin {

    private static final class RepoSetup {
        private final DataRepository repository;
        private final String storageType;

        private RepoSetup(DataRepository repository, String storageType) {
            this.repository = repository;
            this.storageType = storageType;
        }

        private DataRepository repository() {
            return repository;
        }

        private String storageType() {
            return storageType;
        }
    }

    private LegacyPaperCommandManager<CommandSender> commandManager;
    private MinecraftHelp<CommandSender> minecraftHelp;
    private BukkitAudiences adventure;
    private DataRepository dataRepository;
    private TreasureManager treasureManager;
    private PlayerManager playerManager;
    private LeaderboardManager leaderboardManager;
    private CollectionManager collectionManager;
    private RewardManager rewardManager;
    private PlaceModeManager placeModeManager;
    private RewardPresetManager rewardPresetManager;
    private PlaceItemManager placeItemManager;
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
    private TreasureVisibilityManager treasureVisibilityManager;
    private CollectionDeletionCleanupManager collectionDeletionCleanupManager;

    private Random random;

    private String currentStorageType;

    private static String normalizeStorageType(String rawType) {
        if (rawType == null) {
            return "YAML";
        }
        String normalized = rawType.trim().toUpperCase(Locale.ROOT);
        if ("MYSQL".equals(normalized) || "SQLITE".equals(normalized) || "YAML".equals(normalized)) {
            return normalized;
        }
        return "YAML";
    }

    private RepoSetup createRepositoryFromType(String normalizedType) {
        if (normalizedType.equals("MYSQL")) {
            return new RepoSetup(
                    new SqlRepository(
                            this,
                            getConfig().getString("storage.mysql.host"),
                            getConfig().getInt("storage.mysql.port"),
                            getConfig().getString("storage.mysql.database"),
                            getConfig().getString("storage.mysql.username"),
                            getConfig().getString("storage.mysql.password"),
                            false
                    ),
                    "MYSQL"
            );
        }
        if (normalizedType.equals("SQLITE")) {
            return new RepoSetup(new SqlRepository(this, null, 0, null, null, null, true), "SQLITE");
        }
        return new RepoSetup(new YamlRepository(this), "YAML");
    }

    private RepoSetup createRepositoryFromConfig() {
        String desiredType = normalizeStorageType(getConfig().getString("storage.type", "YAML"));
        return createRepositoryFromType(desiredType);
    }

    private void initRepository(DataRepository repository) {
        repository.init();

        // Start periodic index flush for YAML backend (every 60 seconds)
        if (repository instanceof YamlRepository) {
            ((YamlRepository) repository).startPeriodicFlush(60);
        }
    }

    private void rebuildRepositoryDependentManagers() {
        rewardPresetManager = new RewardPresetManager(this, dataRepository);
        placeItemManager = new PlaceItemManager(this, dataRepository);
        treasureManager = new TreasureManager(dataRepository);
        playerManager = new PlayerManager(this, dataRepository);
        leaderboardManager = new LeaderboardManager(this, dataRepository);
        collectionManager = new CollectionManager(this, dataRepository, treasureManager, playerManager, rewardManager);

        particleManager = new ParticleManager(this, treasureManager, playerManager, collectionManager);
        hintManager = new HintManager(this, treasureManager, collectionManager, playerManager, messageManager, particleManager);
        proximityManager = new ProximityManager(this, treasureManager, playerManager);
        placeModeManager = new PlaceModeManager(this);
        scanManager = new ScanManager(this, collectionManager, proximityManager, particleManager, placeModeManager);
        treasureVisibilityManager = new TreasureVisibilityManager(this, treasureManager, collectionManager);
        collectionDeletionCleanupManager = new CollectionDeletionCleanupManager(this);

        particleManager.start();
        scanManager.start();
    }

    @Override
    public void onEnable() {
        random = new Random();

        File configFile = new File(getDataFolder(), "config.yml");
        YamlConfiguration previousConfig = YamlConfiguration.loadConfiguration(configFile);
        int previousConfigVersion = previousConfig.getInt("config-version", 0);

        // Check if legacy migration is needed BEFORE any config updates
        LegacyMigrationConfig legacyCfg = LegacyMigrationConfig.create(getDataFolder());
        legacyCfg = maybeAutoEnableLegacyMigration(previousConfigVersion, legacyCfg);

        if (legacyCfg.enabled()) {
            // Run legacy migration BEFORE ConfigUpdater to preserve original config for backup
            runLegacyMigrationThenStartup(configFile, previousConfig, legacyCfg);
            return;
        }

        // Normal startup path (no legacy migration)
        normalStartup(configFile);
    }

    /**
     * Runs legacy migration BEFORE ConfigUpdater, so the backup contains the original config.yml
     * with Place: section and other legacy data intact.
     */
    private void runLegacyMigrationThenStartup(File configFile, YamlConfiguration previousConfig, LegacyMigrationConfig legacyCfg) {
        getLogger().warning("Legacy migration detected. Running BEFORE config update to preserve backup...");

        // Initialize Adventure early (needed for some components)
        adventure = BukkitAudiences.create(this);
        migrationService = new MigrationService(getLogger());

        // Create repository - use storage.type from legacy config if present, else default to YAML
        String storageType = normalizeStorageType(previousConfig.getString("storage.type", "YAML"));
        RepoSetup repoSetup = createRepositoryFromType(storageType);
        dataRepository = repoSetup.repository();
        currentStorageType = repoSetup.storageType();
        initRepository(dataRepository);

        // Run legacy migration on ORIGINAL files (before ConfigUpdater modifies them)
        LegacyDataMigrator migrator = new LegacyDataMigrator(this, dataRepository, legacyCfg);
        migrator.run().whenComplete((result, ex) -> {
            if (ex != null) {
                getLogger().severe("Legacy migration failed: " + ex.getMessage());
                if (legacyCfg.failFast()) {
                    Bukkit.getScheduler().runTask(this, () -> Bukkit.getPluginManager().disablePlugin(this));
                    return;
                }
                // Continue startup anyway (best-effort).
            } else if (result != null) {
                getLogger().info("Legacy migration complete: collections=" + result.collectionsImported()
                    + ", treasures=" + result.treasuresImported()
                    + ", players=" + result.playersImported()
                    + ", rewardPresets=" + result.rewardPresetsImported()
                    + ", placePresets=" + result.placePresetsImported()
                    + ", links=" + result.playerFoundLinks()
                    + ", missing-links=" + result.missingTreasureLinks());
            }

            // NOW run config update and finish startup on main thread
            Bukkit.getScheduler().runTask(this, () -> {
                saveDefaultConfig();
                ConfigUpdater.update(this, "config.yml", configFile, ConfigMigrationHandler::migrateConfig);
                reloadConfig();

                // Force storage type to YAML after legacy migration to avoid data backend changes
                getConfig().set("storage.type", "YAML");
                saveConfig();
                getLogger().info("Storage type set to YAML after legacy migration.");

                // MessageManager needs config, initialize after update
                messageManager = new MessageManager(this);
                soundManager = new SoundManager(this);

                finishStartup();
            });
        });
    }

    /**
     * Normal startup path when no legacy migration is needed.
     */
    private void normalStartup(File configFile) {
        saveDefaultConfig();
        ConfigUpdater.update(this, "config.yml", configFile, ConfigMigrationHandler::migrateConfig);
        reloadConfig();

        // Initialize Adventure
        adventure = BukkitAudiences.create(this);

        migrationService = new MigrationService(getLogger());
        // Initialize Message Manager
        messageManager = new MessageManager(this);
        soundManager = new SoundManager(this);

        // Initialize Data Repository
        RepoSetup repoSetup = createRepositoryFromConfig();
        dataRepository = repoSetup.repository();
        currentStorageType = repoSetup.storageType();
        initRepository(dataRepository);

        finishStartup();
    }

    private LegacyMigrationConfig maybeAutoEnableLegacyMigration(int previousConfigVersion, LegacyMigrationConfig legacyCfg) {
        if (legacyCfg.enabled()) {
            return legacyCfg;
        }

        // Only auto-run when upgrading from older configs (1-3).
        if (previousConfigVersion <= 0 || previousConfigVersion >= 4) {
            return legacyCfg;
        }

        // Auto-detect legacy data in plugin folder
        if (looksLikeLegacyDataFolder(legacyCfg.sourceFolder())) {
            getLogger().warning("Auto-detected legacy data (upgrade from config-version " + previousConfigVersion + ")");
            getLogger().warning("Running legacy migration automatically.");
            return legacyCfg.withEnabled(true);
        }

        return legacyCfg;
    }

    private static boolean looksLikeLegacyDataFolder(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            return false;
        }
        File eggs = new File(folder, "eggs");
        return eggs.exists() && eggs.isDirectory();
    }

    private void finishStartup() {
        // Initialize Managers
        rewardManager = new RewardManager(this);
        placeModeManager = new PlaceModeManager(this);
        rebuildRepositoryDependentManagers();
        rewardPresetManager.reloadPresets();
        placeItemManager.reloadItems();
        treasureManager.loadTreasures();
        treasureVisibilityManager.start();
        if (collectionDeletionCleanupManager != null) {
            collectionDeletionCleanupManager.start();
        }
        minigameManager = new MinigameManager(this);
        fireworkManager = new FireworkManager(this);

        // Register Listeners
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlaceModeListener(this), this);
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") != null && ItemsAdderAdapter.isEnabled()) {
            getServer().getPluginManager().registerEvents(new ItemsAdderIntegrationListener(this), this);
        }
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
    //        new Metrics(this, 19495);

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

    public PlaceItemManager getPlaceItemManager() {
        return placeItemManager;
    }

    /**
     * Reloads the data repository.
     *
     * If storage.type changed, swaps the repository instance and rebuilds dependent managers/tasks.
     * @return true if the storage backend was swapped, false if it was only reloaded.
     */
    public synchronized boolean reloadStorageBackend() {
        String desiredType = normalizeStorageType(getConfig().getString("storage.type", "YAML"));
        String existingType = normalizeStorageType(currentStorageType);

        // If the type didn't change, keep existing instance and use its reload hook.
        if (desiredType.equals(existingType)) {
            if (dataRepository != null) {
                dataRepository.reload();
            }
            return false;
        }

        // Stop tasks that would keep using old managers/repository
        try {
            if (hintManager != null) {
                hintManager.stop();
            }
        } catch (Exception ignored) {
        }

        if (scanManager != null) {
            scanManager.stop();
        }
        if (treasureVisibilityManager != null) {
            treasureVisibilityManager.stop();
        }
        if (collectionDeletionCleanupManager != null) {
            collectionDeletionCleanupManager.stop();
        }
        if (particleManager != null) {
            particleManager.stop();
        }
        if (leaderboardManager != null) {
            leaderboardManager.stop();
        }
        if (collectionManager != null) {
            collectionManager.stop();
        }
        if (playerManager != null) {
            HandlerList.unregisterAll(playerManager);
        }

        if (dataRepository != null) {
            dataRepository.shutdown();
        }

        // Create and init the new repository
        RepoSetup repoSetup = createRepositoryFromType(desiredType);
        dataRepository = repoSetup.repository();
        currentStorageType = repoSetup.storageType();
        initRepository(dataRepository);

        // Rebuild managers that depend on the repository and/or each other
        rebuildRepositoryDependentManagers();

        if (treasureVisibilityManager != null) {
            treasureVisibilityManager.start();
        }
        if (collectionDeletionCleanupManager != null) {
            collectionDeletionCleanupManager.start();
        }

        return true;
    }

    /**
     * Applies the result of a successful migration to the plugin configuration.
     *
     * This updates {@code storage.type} and, when migrating to MySQL, copies {@code migration.target.*}
     * into {@code storage.mysql.*}. This method only writes config; callers should run the plugin's
     * reload routine afterwards.
     */
    public synchronized void applyMigratedStorageConfig(String targetType) {
        String normalizedType = normalizeStorageType(targetType);
        getConfig().set("storage.type", normalizedType);

        if (normalizedType.equals("MYSQL")) {
            getConfig().set("storage.mysql.host", getConfig().getString("migration.target.host", "localhost"));
            getConfig().set("storage.mysql.port", getConfig().getInt("migration.target.port", 3306));
            getConfig().set("storage.mysql.database", getConfig().getString("migration.target.database", "advancedhunt_target"));
            getConfig().set("storage.mysql.username", getConfig().getString("migration.target.username", "root"));
            getConfig().set("storage.mysql.password", getConfig().getString("migration.target.password", "password"));
        }

        saveConfig();
    }

    private void setupCommands() {
        ExecutionCoordinator<CommandSender> executionCoordinator = ExecutionCoordinator.simpleCoordinator();

        commandManager = LegacyPaperCommandManager.createNative(
                this,
                executionCoordinator
        );

        if (getConfig().getBoolean("command.brigadier-integration", true)
                && commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            commandManager.registerBrigadier();
        } else if (commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            commandManager.registerAsynchronousCompletions();
        }

        String[] parts = getConfig().getString("command.name", "advancedhunt").split("\\|");
        minecraftHelp = MinecraftHelp.<CommandSender>builder()
                .commandManager(commandManager)
                .audienceProvider(adventure::sender)
                .commandPrefix("/" + parts[0])
                .colors(MinecraftHelp.helpColors(
                        NamedTextColor.GOLD,
                        NamedTextColor.YELLOW,
                        NamedTextColor.AQUA,
                        NamedTextColor.GRAY,
                        NamedTextColor.DARK_GRAY
                ))
                .messageProvider((sender, key, args) -> {
                    String raw = getMessageManager().getMessage("command.help.minecraft." + key, false);

                    // Nur Legacy-Farben ersetzen
                    return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
                })
                .build();


        new AdvancedHuntCommand(this).register(commandManager, isDebugMode());
    }

    @Override
    public void onDisable() {
        if (hintManager != null) {
            hintManager.cancelAllVisualHints();
        }
        if (treasureVisibilityManager != null) {
            treasureVisibilityManager.stop();
        }
        if (collectionDeletionCleanupManager != null) {
            collectionDeletionCleanupManager.stop();
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
        if (adventure != null) {
            adventure.close();
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

    public RewardPresetManager getRewardPresetManager() {
        return rewardPresetManager;
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

    public TreasureVisibilityManager getTreasureVisibilityManager() {
        return treasureVisibilityManager;
    }

    public CollectionDeletionCleanupManager getCollectionDeletionCleanupManager() {
        return collectionDeletionCleanupManager;
    }

    public MinecraftHelp<CommandSender> getMinecraftHelp() {
        return minecraftHelp;
    }

    public BukkitAudiences getAdventure() {
        return adventure;
    }

    /**
     * Returns whether debug mode is enabled in the config.
     * When debug mode is enabled, verbose logging messages will be shown.
     * @return true if dev-mode is enabled in config.yml, false otherwise
     */
    public boolean isDebugMode() {
        return getConfig().getBoolean("dev-mode", false);
    }
}