package de.theredend2000.advancedhunt.commands;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.data.SqlRepository;
import de.theredend2000.advancedhunt.data.YamlRepository;
import de.theredend2000.advancedhunt.managers.minigame.MinigameType;
import de.theredend2000.advancedhunt.menu.*;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.model.Treasure;
import de.theredend2000.advancedhunt.model.TreasureRewardHolder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AdvancedHuntCommand {

    private final Main plugin;
    private LegacyPaperCommandManager<CommandSender> commandManager;

    public AdvancedHuntCommand(Main plugin) {
        this.plugin = plugin;
    }

    public void register(final LegacyPaperCommandManager<CommandSender> commandManager) {
        this.commandManager = commandManager;
        // Setup suggestion providers
        final SuggestionProvider<CommandSender> collectionsSuggestions = createCollectionsSuggestions();
        final SuggestionProvider<CommandSender> playerNameSuggestions = createPlayerNameSuggestions();
        final SuggestionProvider<CommandSender> minigameSuggestions = createMinigameSuggestions();
        
        // Setup reusable argument components
        final CommandComponent.Builder<CommandSender, String> collectionArgument = 
            createCollectionArgument(collectionsSuggestions);

        // Register all commands
        registerGeneralCommands(commandManager);
        registerCollectionCommands(commandManager, collectionsSuggestions, collectionArgument);
        registerPlayerCommands(commandManager, collectionArgument);
        registerResetCommands(commandManager, collectionArgument, playerNameSuggestions);
        registerMinigameCommands(commandManager, minigameSuggestions);
        registerMigrationCommands(commandManager);
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a base command builder for /advancedhunt or /ah.
     */
    private Command.Builder<CommandSender> baseBuilder() {
        return commandManager.commandBuilder("advancedhunt", "ah");
    }

    /**
     * Validates that the sender is a player and returns it, or sends error message.
     */
    private Optional<Player> requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return Optional.of(player);
        }
        sender.sendMessage(plugin.getMessageManager().getMessage("command.not_player"));
        return Optional.empty();
    }

    /**
     * Looks up a collection by name and executes action if found, or sends error message.
     */
    private void withCollection(CommandSender sender, String collectionName, 
                               Consumer<Collection> action) {
        plugin.getCollectionManager().getCollectionByName(collectionName)
            .ifPresentOrElse(action, 
                () -> sender.sendMessage(plugin.getMessageManager().getMessage("command.collection_not_found")));
    }

    /**
     * Invalidates player cache for all online players.
     */
    private void invalidateAllPlayerCaches() {
        Bukkit.getOnlinePlayers().forEach(p -> 
            plugin.getPlayerManager().invalidate(p.getUniqueId()));
    }

    /**
     * Validates an offline player exists.
     */
    private Optional<OfflinePlayer> validateOfflinePlayer(CommandSender sender, String playerName) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.reset.player_not_found"));
            return Optional.empty();
        }
        return Optional.of(offlinePlayer);
    }

    // ==================== Suggestion Providers ====================

    private SuggestionProvider<CommandSender> createCollectionsSuggestions() {
        return (context, input) -> CompletableFuture.completedFuture(
            plugin.getCollectionManager().getAllCollectionNames().stream()
                .map(Suggestion::suggestion)
                .collect(Collectors.toList())
        );
    }

    private SuggestionProvider<CommandSender> createPlayerNameSuggestions() {
        return (context, input) -> CompletableFuture.supplyAsync(() ->
            Arrays.stream(Bukkit.getOfflinePlayers())
                .map(OfflinePlayer::getName)
                .filter(name -> name != null && !name.isEmpty())
                .map(Suggestion::suggestion)
                .distinct()
                .collect(Collectors.toList())
        );
    }

    private SuggestionProvider<CommandSender> createMinigameSuggestions() {
        return (context, input) -> CompletableFuture.completedFuture(
            Arrays.stream(MinigameType.values())
                .map(Enum::name)
                .map(Suggestion::suggestion)
                .collect(Collectors.toList())
        );
    }

    private CommandComponent.Builder<CommandSender, String> createCollectionArgument(
            SuggestionProvider<CommandSender> collectionsSuggestions) {
        return CommandComponent.<CommandSender, String>builder()
            .name("collection")
            .parser(StringParser.stringParser())
            .suggestionProvider(collectionsSuggestions);
    }

    // ==================== Command Registration ====================

    /**
     * Register general utility commands (help, reload, rewards).
     */
    private void registerGeneralCommands(LegacyPaperCommandManager<CommandSender> commandManager) {
        commandManager.command(
            baseBuilder()
                .literal("help")
                .permission("advancedhunt.help")
                .handler(context -> help(context.sender()))
        );

        commandManager.command(
            baseBuilder()
                .literal("reload")
                .permission("advancedhunt.admin.reload")
                .handler(context -> reload(context.sender()))
        );

        commandManager.command(
            baseBuilder()
                .literal("rewards")
                .permission("advancedhunt.admin.rewards")
                .handler(context -> rewards(context.sender()))
        );
    }

    /**
     * Register collection management commands (editor, create, edit, rename).
     */
    private void registerCollectionCommands(
            LegacyPaperCommandManager<CommandSender> commandManager,
            SuggestionProvider<CommandSender> collectionsSuggestions,
            CommandComponent.Builder<CommandSender, String> collectionArgument) {
        
        // /ah collection - open editor
        commandManager.command(
            baseBuilder()
                .literal("collection")
                .permission("advancedhunt.admin.editor")
                .handler(context -> editor(context.sender()))
        );

        // /ah collection create <name>
        commandManager.command(
            baseBuilder()
                .literal("collection")
                .literal("create")
                .required("name", StringParser.stringParser())
                .permission("advancedhunt.admin.collection.create")
                .handler(context -> createCollection(context.sender(), context.get("name")))
        );

        // /ah collection edit <name>
        commandManager.command(
            baseBuilder()
                .literal("collection")
                .literal("edit")
                .required("name", StringParser.stringParser(), collectionsSuggestions)
                .permission("advancedhunt.admin.collection.edit")
                .handler(context -> editCollection(context.sender(), context.get("name")))
        );

        // /ah collection rename <oldName> <newName>
        commandManager.command(
            baseBuilder()
                .literal("collection")
                .literal("rename")
                .required("oldName", StringParser.stringParser(), collectionsSuggestions)
                .required("newName", StringParser.stringParser())
                .permission("advancedhunt.admin.collection.rename")
                .handler(context -> renameCollection(
                    context.sender(),
                    context.get("oldName"),
                    context.get("newName")
                ))
        );
    }

    /**
     * Register player-facing commands (list, leaderboard, progress, place).
     */
    private void registerPlayerCommands(
            LegacyPaperCommandManager<CommandSender> commandManager,
            CommandComponent.Builder<CommandSender, String> collectionArgument) {
        
        // /ah list
        commandManager.command(
            baseBuilder()
                .literal("list")
                .permission("advancedhunt.list")
                .handler(context -> list(context.sender(), null))
        );

        // /ah list <collection>
        commandManager.command(
            baseBuilder()
                .literal("list")
                .required(collectionArgument)
                .permission("advancedhunt.list")
                .handler(context -> list(context.sender(), context.get("collection")))
        );

        // /ah leaderboard <collection>
        commandManager.command(
            baseBuilder()
                .literal("leaderboard")
                .required(collectionArgument)
                .permission("advancedhunt.leaderboard")
                .handler(context -> leaderboard(context.sender(), context.get("collection")))
        );

        // /ah progress <collection>
        commandManager.command(
            baseBuilder()
                .literal("progress")
                .required(collectionArgument)
                .permission("advancedhunt.progress")
                .handler(context -> progress(context.sender(), context.get("collection")))
        );

        // /ah place <collection>
        commandManager.command(
            baseBuilder()
                .literal("place")
                .required(collectionArgument)
                .permission("advancedhunt.admin.place")
                .handler(context -> placeMode(context.sender(), context.get("collection")))
        );
    }

    /**
     * Register reset commands (all, collection, player, player + collection).
     */
    private void registerResetCommands(
            LegacyPaperCommandManager<CommandSender> commandManager,
            CommandComponent.Builder<CommandSender, String> collectionArgument,
            SuggestionProvider<CommandSender> playerNameSuggestions) {
        
        // /ah reset all
        commandManager.command(
            baseBuilder()
                .literal("reset")
                .literal("all")
                .permission("advancedhunt.admin.reset")
                .handler(context -> resetAll(context.sender()))
        );

        // /ah reset collection <collection>
        commandManager.command(
            baseBuilder()
                .literal("reset")
                .literal("collection")
                .required(collectionArgument)
                .permission("advancedhunt.admin.reset")
                .handler(context -> resetCollection(context.sender(), context.get("collection")))
        );

        // /ah reset player <player>
        commandManager.command(
            baseBuilder()
                .literal("reset")
                .literal("player")
                .required("player", StringParser.stringParser(), playerNameSuggestions)
                .permission("advancedhunt.admin.reset")
                .handler(context -> resetPlayer(context.sender(), context.get("player")))
        );

        // /ah reset player <player> <collection>
        commandManager.command(
            baseBuilder()
                .literal("reset")
                .literal("player")
                .required("player", StringParser.stringParser(), playerNameSuggestions)
                .required(collectionArgument)
                .permission("advancedhunt.admin.reset")
                .handler(context -> resetPlayerCollection(
                    context.sender(),
                    context.get("player"),
                    context.get("collection")
                ))
        );
    }

    /**
     * Register minigame commands.
     */
    private void registerMinigameCommands(
            LegacyPaperCommandManager<CommandSender> commandManager,
            SuggestionProvider<CommandSender> minigameSuggestions) {
        
        commandManager.command(
            baseBuilder()
                .literal("minigame")
                .required("type", StringParser.stringParser(), minigameSuggestions)
                .permission("advancedhunt.minigame")
                .handler(context -> minigame(context.sender(), context.get("type")))
        );
    }

    /**
     * Register migration commands (yaml, sqlite, mysql with --force variants).
     */
    private void registerMigrationCommands(LegacyPaperCommandManager<CommandSender> commandManager) {
        // YAML migration
        registerMigrationCommand(commandManager, "yaml", false);
        registerMigrationCommand(commandManager, "yaml", true);
        
        // SQLite migration
        registerMigrationCommand(commandManager, "sqlite", false);
        registerMigrationCommand(commandManager, "sqlite", true);
        
        // MySQL migration
        registerMigrationCommand(commandManager, "mysql", false);
        registerMigrationCommand(commandManager, "mysql", true);
    }

    /**
     * Helper to register a single migration command with or without --force.
     */
    private void registerMigrationCommand(
            LegacyPaperCommandManager<CommandSender> commandManager,
            String storageType,
            boolean force) {
        
        var builder = baseBuilder()
            .literal("migrate")
            .literal(storageType.toLowerCase());
        
        if (force) {
            builder = builder.literal("--force");
        }
        
        commandManager.command(
            builder.permission("advancedhunt.admin.migrate")
                .handler(context -> migrate(context.sender(), storageType.toUpperCase(), force))
        );
    }

    // ==================== Command Handlers ====================

    public void help(CommandSender sender) {
        sender.sendMessage(plugin.getMessageManager().getMessageList("command.help").toArray(new String[0]));
    }

    public void rewards(CommandSender sender) {
        var playerOpt = requirePlayer(sender);
        if (playerOpt.isEmpty()) return;
        Player player = playerOpt.get();
        
        // Get the block the player is looking at
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("command.rewards.no_block"));
            return;
        }
        
        // Check if it's a treasure
        Treasure treasure = plugin.getTreasureManager().getTreasureAt(targetBlock.getLocation());
        if (treasure == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("command.rewards.not_treasure"));
            return;
        }
        
        // Open the rewards menu
        new RewardsMenu(player, plugin, new TreasureRewardHolder(plugin, treasure)).open();
    }

    public void editor(CommandSender sender) {
        requirePlayer(sender).ifPresent(player -> 
            new CollectionEditorMenu(player, plugin).open()
        );
    }

    public void reload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getMessageManager().reloadMessages();
        plugin.getParticleManager().reload();
        plugin.getSoundManager().reload();
        plugin.getDataRepository().reload();
        plugin.getTreasureManager().loadTreasures();
        plugin.getProximityManager().reloadConfig();
        sender.sendMessage(plugin.getMessageManager().getMessage("command.reload.success"));
    }

    public void list(CommandSender sender, String collectionName) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.not_player"));
            sender.sendMessage(plugin.getMessageManager().getMessage("command.list.header"));
            for (String collection : plugin.getCollectionManager().getAllCollectionNames()) {
                sender.sendMessage(plugin.getMessageManager().getMessage("command.list.format", "%collection%", collection));
            }
            return;
        }
        Player player = (Player) sender;

        if (collectionName == null) {
            UUID selectedId = plugin.getPlaceModeManager().getCollectionId(player);
            if (selectedId != null) {
                new CollectionListMenu(player, selectedId, plugin).open();
            } else {
                sender.sendMessage(plugin.getMessageManager().getMessage("command.list.header"));
                for (Collection collection : plugin.getCollectionManager().getAllCollections()) {
                    boolean isAvailable = plugin.getCollectionManager().isCollectionAvailable(collection);
                    String availabilityStatus = isAvailable ? 
                        plugin.getMessageManager().getMessage("collection.available", false) :
                        plugin.getMessageManager().getMessage("collection.not_available", false);
                    sender.sendMessage(plugin.getMessageManager().getMessage("command.list.format", 
                        "%collection%", collection.getName() + " " + availabilityStatus));
                }
                sender.sendMessage(plugin.getMessageManager().getMessage("command.list.hint"));
            }
        } else {
            Optional<Collection> collectionOpt = plugin.getCollectionManager().getCollectionByName(collectionName);
            if (collectionOpt.isPresent()) {
                new CollectionListMenu(player, collectionOpt.get().getId(), plugin).open();
            } else {
                sender.sendMessage(plugin.getMessageManager().getMessage("command.collection_not_found"));
            }
        }
    }

    public void leaderboard(CommandSender sender, String collectionName) {
        requirePlayer(sender).ifPresent(player ->
            withCollection(sender, collectionName, collection ->
                new LeaderboardMenu(player, plugin, collection, null).open()
            )
        );
    }

    public void progress(CommandSender sender, String collectionName) {
        requirePlayer(sender).ifPresent(player ->
            withCollection(sender, collectionName, collection -> {
                // Warn if collection is not currently available
                if (!plugin.getCollectionManager().isCollectionAvailable(collection)) {
                    player.sendMessage(plugin.getMessageManager().getMessage("collection.unavailable", 
                        "%collection%", collection.getName()));
                    plugin.getCollectionManager().getNextActivation(collection).ifPresent(nextTime -> {
                        String timeStr = plugin.getMessageManager().formatDateTime(nextTime);
                        player.sendMessage(plugin.getMessageManager().getMessage("collection.available_at",
                            "%time%", timeStr));
                    });
                }
                
                new ProgressMenu(player, collection.getId(), collection.getName(), plugin).open();
            })
        );
    }

    public void editCollection(CommandSender sender, String name) {
        requirePlayer(sender).ifPresent(player ->
            withCollection(sender, name, collection ->
                new CollectionSettingsMenu(player, plugin, collection).open()
            )
        );
    }

    public void renameCollection(CommandSender sender, String oldName, String newName) {
        plugin.getCollectionManager().renameCollection(oldName, newName).thenAccept(success -> {
            if (success) {
                sender.sendMessage(plugin.getMessageManager().getMessage("command.rename.success", "%old_name%", oldName, "%new_name%", newName));
            } else {
                sender.sendMessage(plugin.getMessageManager().getMessage("command.rename.failed"));
            }
        });
    }

    public void createCollection(CommandSender sender, String name) {
        plugin.getCollectionManager().createCollection(name).thenAccept(success -> {
            if (success) {
                sender.sendMessage(plugin.getMessageManager().getMessage("command.create.success", "%name%", name));
            } else {
                sender.sendMessage(plugin.getMessageManager().getMessage("command.create.failed"));
            }
        });
    }

    public void placeMode(CommandSender sender, String collectionName) {
        var playerOpt = requirePlayer(sender);
        if (playerOpt.isEmpty()) return;
        Player player = playerOpt.get();
        
        if (plugin.getPlaceModeManager().isInPlaceMode(player)) {
            plugin.getPlaceModeManager().removePlaceMode(player);
            sender.sendMessage(plugin.getMessageManager().getMessage("command.place_mode.disabled"));
            return;
        }

        plugin.getCollectionManager().getCollectionByName(collectionName).ifPresentOrElse(collection -> {
            plugin.getPlaceModeManager().setPlaceMode(player, collection);
            sender.sendMessage(plugin.getMessageManager().getMessage("command.place_mode.enabled", "%collection%", collection.getName()));
        }, () -> {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.collection_not_found"));
        });
    }

    public void resetAll(CommandSender sender) {
        plugin.getDataRepository().resetAllProgress().thenAccept(count -> {
            plugin.getParticleManager().clearAllGlobalCache();
            invalidateAllPlayerCaches();
            sender.sendMessage(plugin.getMessageManager().getMessage("command.reset.all_success", 
                "%count%", String.valueOf(count)));
        });
    }

    public void resetCollection(CommandSender sender, String collectionName) {
        withCollection(sender, collectionName, collection ->
            plugin.getDataRepository().resetCollectionProgress(collection.getId()).thenAccept(count -> {
                plugin.getParticleManager().clearGlobalCache(collection.getId());
                invalidateAllPlayerCaches();
                sender.sendMessage(plugin.getMessageManager().getMessage("command.reset.collection_success",
                    "%collection%", collection.getName(),
                    "%count%", String.valueOf(count)));
            })
        );
    }

    public void resetPlayer(CommandSender sender, String playerName) {
        var offlinePlayerOpt = validateOfflinePlayer(sender, playerName);
        if (offlinePlayerOpt.isEmpty()) return;
        OfflinePlayer offlinePlayer = offlinePlayerOpt.get();

        plugin.getDataRepository().resetPlayerProgress(offlinePlayer.getUniqueId()).thenAccept(count -> {
            plugin.getParticleManager().clearAllGlobalCache();
            
            plugin.getPlayerManager().invalidate(offlinePlayer.getUniqueId());
            sender.sendMessage(plugin.getMessageManager().getMessage("command.reset.player_success",
                "%player%", playerName,
                "%count%", String.valueOf(count)));
        });
    }

    public void resetPlayerCollection(CommandSender sender, 
                                     String playerName,
                                     String collectionName) {
        var offlinePlayerOpt = validateOfflinePlayer(sender, playerName);
        if (offlinePlayerOpt.isEmpty()) return;
        OfflinePlayer offlinePlayer = offlinePlayerOpt.get();

        withCollection(sender, collectionName, collection ->
            plugin.getDataRepository().resetPlayerCollectionProgress(
                offlinePlayer.getUniqueId(), 
                collection.getId()
            ).thenAccept(count -> {
                if (collection.isSinglePlayerFind()) {
                    plugin.getParticleManager().clearGlobalCache(collection.getId());
                }
                
                plugin.getPlayerManager().invalidate(offlinePlayer.getUniqueId());
                sender.sendMessage(plugin.getMessageManager().getMessage("command.reset.player_collection_success",
                    "%player%", playerName,
                    "%collection%", collection.getName(),
                    "%count%", String.valueOf(count)));
            })
        );
    }

    public void minigame(CommandSender sender, String typeName) {
        var playerOpt = requirePlayer(sender);
        if (playerOpt.isEmpty()) return;
        Player player = playerOpt.get();

        MinigameType type;
        try {
            type = MinigameType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(plugin.getMessageManager().getMessage("minigame.error.invalid_type"));
            return;
        }

        plugin.getMinigameManager().startMinigame(player, type, (success) -> {
            if (success) {
                player.sendMessage(plugin.getMessageManager().getMessage("minigame.success"));
            } else {
                player.sendMessage(plugin.getMessageManager().getMessage("minigame.failed"));
            }
        });
    }

    private void migrate(CommandSender sender, String type, boolean force) {
        // Create target repository based on type
        DataRepository targetRepo = createTargetRepository(type, sender);
        if (targetRepo == null) {
            return; // Error message already sent
        }

        sender.sendMessage(plugin.getMessageManager().getMessage("command.migration.started"));
        targetRepo.init();

        // Check for existing data and proceed with migration
        checkExistingDataAndMigrate(sender, type, targetRepo, force);
    }

    private DataRepository createTargetRepository(String type, CommandSender sender) {
        if (type.equalsIgnoreCase("YAML")) {
            return new YamlRepository(plugin);
        }
        
        if (type.equalsIgnoreCase("SQLITE")) {
            return new SqlRepository(plugin, null, 0, null, null, null, true);
        }
        
        if (type.equalsIgnoreCase("MYSQL")) {
            return createMySQLRepository(sender);
        }
        
        return null;
    }

    private DataRepository createMySQLRepository(CommandSender sender) {
        String host = plugin.getConfig().getString("migration.target.host", "localhost");
        int port = plugin.getConfig().getInt("migration.target.port", 3306);
        String database = plugin.getConfig().getString("migration.target.database", "advancedhunt_target");
        String username = plugin.getConfig().getString("migration.target.username", "root");
        String password = plugin.getConfig().getString("migration.target.password", "password");
        
        // Check if ALL values are still at defaults (indicating no configuration was done)
        boolean allDefaults = host.equals("localhost") &&
                              port == 3306 &&
                              database.equals("advancedhunt_target") &&
                              username.equals("root") &&
                              password.equals("password");
        
        if (allDefaults) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.migration.mysql_not_configured"));
            return null;
        }
        
        return new SqlRepository(plugin, host, port, database, username, password, false);
    }

    private void checkExistingDataAndMigrate(CommandSender sender, String type, DataRepository targetRepo, boolean force) {
        CompletableFuture.allOf(
                targetRepo.loadCollections(),
                targetRepo.loadTreasures(),
                targetRepo.loadAllPlayerData()
        ).thenCompose(v -> {
            int existingCollections = safeGetSize(targetRepo.loadCollections());
            int existingTreasures = safeGetSize(targetRepo.loadTreasures());
            int existingPlayerData = safeGetSize(targetRepo.loadAllPlayerData());
            
            boolean hasExistingData = existingCollections > 0 || existingTreasures > 0 || existingPlayerData > 0;
            
            if (hasExistingData && !force) {
                sender.sendMessage(plugin.getMessageManager().getMessage("command.migration.existing_data_abort",
                        "%collections%", String.valueOf(existingCollections),
                        "%treasures%", String.valueOf(existingTreasures),
                        "%players%", String.valueOf(existingPlayerData)));
                targetRepo.shutdown();
                return CompletableFuture.failedFuture(new IllegalStateException("Migration aborted - existing data"));
            }
            
            if (hasExistingData) {
                sender.sendMessage(plugin.getMessageManager().getMessage("command.migration.warning_existing_data",
                        "%collections%", String.valueOf(existingCollections),
                        "%treasures%", String.valueOf(existingTreasures),
                        "%players%", String.valueOf(existingPlayerData)));
            }
            
            return plugin.getMigrationService().migrate(plugin.getDataRepository(), targetRepo);
        }).thenRun(() -> {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.migration.success"));
            targetRepo.shutdown();
            
            if (type.equalsIgnoreCase("MYSQL")) {
                resetMySQLConfig(sender);
            }
        }).exceptionally(e -> {
            // Only show an error if it's not our intentional abort
            if (!(e.getCause() instanceof IllegalStateException && 
                  e.getCause().getMessage().equals("Migration aborted - existing data"))) {
                sender.sendMessage(plugin.getMessageManager().getMessage("command.migration.failed"));
                e.printStackTrace();
            }
            targetRepo.shutdown();
            return null;
        });
    }

    private int safeGetSize(CompletableFuture<? extends java.util.Collection<?>> future) {
        try {
            return future.join().size();
        } catch (Exception e) {
            return 0;
        }
    }

    private void resetMySQLConfig(CommandSender sender) {
        plugin.getConfig().set("migration.target.host", "localhost");
        plugin.getConfig().set("migration.target.port", 3306);
        plugin.getConfig().set("migration.target.database", "advancedhunt_target");
        plugin.getConfig().set("migration.target.username", "root");
        plugin.getConfig().set("migration.target.password", "password");
        plugin.saveConfig();
        sender.sendMessage(plugin.getMessageManager().getMessage("command.migration.mysql_config_reset"));
    }
}
