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
import java.util.List;
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

        // Setup suggestion providers inline
        SuggestionProvider<CommandSender> collectionsSuggestions = (context, input) ->
                CompletableFuture.completedFuture(plugin.getCollectionManager().getAllCollectionNames().stream()
                        .map(Suggestion::suggestion).collect(Collectors.toList()));

        SuggestionProvider<CommandSender> playerNameSuggestions = (context, input) ->
                CompletableFuture.supplyAsync(() -> Arrays.stream(Bukkit.getOfflinePlayers())
                        .map(OfflinePlayer::getName)
                        .filter(name -> name != null && !name.isEmpty())
                        .map(Suggestion::suggestion)
                        .distinct()
                        .collect(Collectors.toList()));

        SuggestionProvider<CommandSender> minigameSuggestions = (context, input) ->
                CompletableFuture.completedFuture(Arrays.stream(MinigameType.values())
                        .map(Enum::name).map(Suggestion::suggestion).collect(Collectors.toList()));

        CommandComponent.Builder<CommandSender, String> collectionArg = CommandComponent.<CommandSender, String>builder()
                .name("collection").parser(StringParser.stringParser()).suggestionProvider(collectionsSuggestions);

        // ==================== General Commands ====================
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
            playerBuilder()
                .literal("rewards")
                .permission("advancedhunt.admin.rewards")
                .handler(context -> rewards((Player) context.sender()))
        );

        // ==================== Collection Management ====================
        commandManager.command(
            playerBuilder()
                .literal("collection")
                .permission("advancedhunt.admin.editor")
                .handler(context -> editor((Player) context.sender()))
        );

        commandManager.command(
            baseBuilder()
                .literal("collection")
                .literal("create")
                .required("name", StringParser.stringParser())
                .permission("advancedhunt.admin.collection.create")
                .handler(context -> createCollection(context.sender(), context.get("name")))
        );

        commandManager.command(
            playerBuilder()
                .literal("collection")
                .literal("edit")
                .required("name", StringParser.stringParser(), collectionsSuggestions)
                .permission("advancedhunt.admin.collection.edit")
                .handler(context -> editCollection((Player) context.sender(), context.get("name")))
        );

        commandManager.command(
            baseBuilder()
                .literal("collection")
                .literal("rename")
                .required("oldName", StringParser.stringParser(), collectionsSuggestions)
                .required("newName", StringParser.stringParser())
                .permission("advancedhunt.admin.collection.rename")
                .handler(context -> renameCollection(context.sender(), context.get("oldName"), context.get("newName")))
        );

        // ==================== Player Commands ====================
        commandManager.command(
            playerBuilder()
                .literal("list")
                .permission("advancedhunt.list")
                .handler(context -> list((Player) context.sender(), null))
        );

        commandManager.command(
            playerBuilder()
                .literal("list")
                .required(collectionArg)
                .permission("advancedhunt.list")
                .handler(context -> list((Player) context.sender(), context.get("collection")))
        );

        commandManager.command(
            playerBuilder()
                .literal("leaderboard")
                .required(collectionArg)
                .permission("advancedhunt.leaderboard")
                .handler(context -> leaderboard((Player) context.sender(), context.get("collection")))
        );

        commandManager.command(
            playerBuilder()
                .literal("progress")
                .required(collectionArg)
                .permission("advancedhunt.progress")
                .handler(context -> progress((Player) context.sender(), context.get("collection")))
        );

        commandManager.command(
            playerBuilder()
                .literal("place")
                .required(collectionArg)
                .permission("advancedhunt.admin.place")
                .handler(context -> placeMode((Player) context.sender(), context.get("collection")))
        );

        // ==================== Reset Commands ====================
        commandManager.command(
            baseBuilder()
                .literal("reset")
                .literal("all")
                .permission("advancedhunt.admin.reset")
                .handler(context -> resetAll(context.sender()))
        );

        commandManager.command(
            baseBuilder()
                .literal("reset")
                .literal("collection")
                .required(collectionArg)
                .permission("advancedhunt.admin.reset")
                .handler(context -> resetCollection(context.sender(), context.get("collection")))
        );

        commandManager.command(
            baseBuilder()
                .literal("reset")
                .literal("player")
                .required("player", StringParser.stringParser(), playerNameSuggestions)
                .permission("advancedhunt.admin.reset")
                .handler(context -> resetPlayer(context.sender(), context.get("player")))
        );

        commandManager.command(
            baseBuilder()
                .literal("reset")
                .literal("player")
                .required("player", StringParser.stringParser(), playerNameSuggestions)
                .required(collectionArg)
                .permission("advancedhunt.admin.reset")
                .handler(context -> resetPlayerCollection(context.sender(), context.get("player"), context.get("collection")))
        );

        // ==================== Minigame Command ====================
        commandManager.command(
            playerBuilder()
                .literal("minigame")
                .required("type", StringParser.stringParser(), minigameSuggestions)
                .permission("advancedhunt.minigame")
                .handler(context -> minigame((Player) context.sender(), context.get("type")))
        );

        // ==================== Hint Command ====================
        commandManager.command(
            playerBuilder()
                .literal("hint")
                .permission("advancedhunt.hint")
                .handler(context -> hint((Player) context.sender()))
        );

        // ==================== Migration Commands ====================
        SuggestionProvider<CommandSender> migrationTypes = (context, input) ->
            CompletableFuture.completedFuture(List.of("yaml", "sqlite", "mysql").stream()
                .map(Suggestion::suggestion).collect(Collectors.toList()));

        commandManager.command(
            baseBuilder()
                .literal("migrate")
                .required("type", StringParser.stringParser(), migrationTypes)
                .permission("advancedhunt.admin.migrate")
                .handler(context -> migrate(context.sender(), context.<String>get("type").toUpperCase(), false))
        );

        commandManager.command(
            baseBuilder()
                .literal("migrate")
                .required("type", StringParser.stringParser(), migrationTypes)
                .literal("--force")
                .permission("advancedhunt.admin.migrate")
                .handler(context -> migrate(context.sender(), context.<String>get("type").toUpperCase(), true))
        );
    }

    // ==================== Helper Methods ====================

    private Command.Builder<CommandSender> baseBuilder() {
        return commandManager.commandBuilder("advancedhunt", "ah");
    }

    private Command.Builder<CommandSender> playerBuilder() {
        return baseBuilder().senderType(Player.class);
    }

    private void withCollection(CommandSender sender, String collectionName, Consumer<Collection> action) {
        plugin.getCollectionManager().getCollectionByName(collectionName)
                .ifPresentOrElse(action,
                        () -> sender.sendMessage(plugin.getMessageManager().getMessage("command.collection_not_found")));
    }

    private void invalidateAllPlayerCaches() {
        Bukkit.getOnlinePlayers().forEach(p -> plugin.getPlayerManager().invalidate(p.getUniqueId()));
    }

    private Optional<OfflinePlayer> validateOfflinePlayer(CommandSender sender, String playerName) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.reset.player_not_found"));
            return Optional.empty();
        }
        return Optional.of(offlinePlayer);
    }

    // ==================== Command Handlers ====================

    public void help(CommandSender sender) {
        sender.sendMessage(plugin.getMessageManager().getMessageList("command.help").toArray(new String[0]));
    }

    public void rewards(Player player) {
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

    public void editor(Player player) {
        new CollectionEditorMenu(player, plugin).open();
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

    public void list(Player player, String collectionName) {

        if (collectionName == null) {
            UUID selectedId = plugin.getPlaceModeManager().getCollectionId(player);
            if (selectedId != null) {
                new CollectionListMenu(player, selectedId, plugin).open();
            } else {
                player.sendMessage(plugin.getMessageManager().getMessage("command.list.header"));
                for (Collection collection : plugin.getCollectionManager().getAllCollections()) {
                    boolean isAvailable = plugin.getCollectionManager().isCollectionAvailable(collection);
                    String availabilityStatus = isAvailable ?
                            plugin.getMessageManager().getMessage("collection.available", false) :
                            plugin.getMessageManager().getMessage("collection.not_available", false);
                    player.sendMessage(plugin.getMessageManager().getMessage("command.list.format",
                            "%collection%", collection.getName() + " " + availabilityStatus));
                }
                player.sendMessage(plugin.getMessageManager().getMessage("command.list.hint"));
            }
        } else {
            Optional<Collection> collectionOpt = plugin.getCollectionManager().getCollectionByName(collectionName);
            if (collectionOpt.isPresent()) {
                new CollectionListMenu(player, collectionOpt.get().getId(), plugin).open();
            } else {
                player.sendMessage(plugin.getMessageManager().getMessage("command.collection_not_found"));
            }
        }
    }

    public void leaderboard(Player player, String collectionName) {
        withCollection(player, collectionName, collection ->
                new LeaderboardMenu(player, plugin, collection, null).open()
        );
    }

    public void progress(Player player, String collectionName) {
        withCollection(player, collectionName, collection -> {
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
        });
    }

    public void editCollection(Player player, String name) {
        withCollection(player, name, collection ->
                new CollectionSettingsMenu(player, plugin, collection).open()
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

    public void placeMode(Player player, String collectionName) {
        if (plugin.getPlaceModeManager().isInPlaceMode(player)) {
            plugin.getPlaceModeManager().removePlaceMode(player);
            player.sendMessage(plugin.getMessageManager().getMessage("command.place_mode.disabled"));
            return;
        }

        plugin.getCollectionManager().getCollectionByName(collectionName).ifPresentOrElse(collection -> {
            plugin.getPlaceModeManager().setPlaceMode(player, collection);
            player.sendMessage(plugin.getMessageManager().getMessage("command.place_mode.enabled", "%collection%", collection.getName()));
        }, () -> {
            player.sendMessage(plugin.getMessageManager().getMessage("command.collection_not_found"));
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

    public void minigame(Player player, String typeName) {
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

    private void hint(Player player) {
        // Check if hint minigame is enabled
        if (!plugin.getConfig().getBoolean("minigames.hint.enabled", true)) {
            player.sendMessage(plugin.getMessageManager().getMessage("error.feature_disabled"));
            return;
        }

        // Check cooldown
        if (plugin.getHintManager().isOnCooldown(player.getUniqueId())) {
            long remaining = plugin.getHintManager().getRemainingCooldown(player.getUniqueId());
            player.sendMessage(plugin.getMessageManager().getMessage("hint.cooldown",
                    "%time%", String.valueOf(remaining)));
            return;
        }

        // Find a random unfound treasure in range
        Optional<de.theredend2000.advancedhunt.model.TreasureCore> treasureOpt = 
                plugin.getHintManager().findRandomUnfoundTreasure(player);
        
        if (treasureOpt.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getMessage("hint.no_unfound_nearby"));
            // Apply failure cooldown if configured
            plugin.getHintManager().applyFailureCooldown(player.getUniqueId());
            return;
        }

        de.theredend2000.advancedhunt.model.TreasureCore treasure = treasureOpt.get();

        // Get minigame type from config
        String minigameTypeStr = plugin.getConfig().getString("minigames.hint.minigame-type", "REACTION");
        MinigameType minigameType;
        try {
            minigameType = MinigameType.valueOf(minigameTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(plugin.getMessageManager().getMessage("minigame.error.invalid_type"));
            plugin.getLogger().warning("Invalid hint minigame type in config: " + minigameTypeStr);
            return;
        }

        // Start minigame with hint delivery on success
        plugin.getMinigameManager().startMinigame(player, minigameType, (success) -> {
            if (success) {
                player.sendMessage(plugin.getMessageManager().getMessage("hint.success"));
                plugin.getHintManager().deliverHint(player, treasure);
            } else {
                player.sendMessage(plugin.getMessageManager().getMessage("hint.minigame_failed"));
                // Apply failure cooldown if configured
                plugin.getHintManager().applyFailureCooldown(player.getUniqueId());
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
