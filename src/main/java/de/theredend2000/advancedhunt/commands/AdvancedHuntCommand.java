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
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AdvancedHuntCommand {

    private final Main plugin;

    public AdvancedHuntCommand(Main plugin) {
        this.plugin = plugin;
    }

    public void register(final LegacyPaperCommandManager<CommandSender> commandManager) {
        final SuggestionProvider<CommandSender> collectionsSuggestions = (context, input) -> CompletableFuture.completedFuture(
            plugin.getCollectionManager().getAllCollectionNames().stream()
                .map(Suggestion::suggestion)
                .collect(Collectors.toList())
        );

        final SuggestionProvider<CommandSender> playerNameSuggestions = (context, input) -> {
            return CompletableFuture.supplyAsync(() -> {
                // Online players
                List<Suggestion> suggestions = new ArrayList<>();

                suggestions.addAll(Arrays.stream(Bukkit.getOfflinePlayers())
                        .map(OfflinePlayer::getName)
                        .filter(name -> name != null && !name.isEmpty())
                        .map(Suggestion::suggestion)
                        .toList());

                return suggestions.stream().distinct().collect(Collectors.toList());
            });
        };

        final CommandComponent.Builder<CommandSender, String> collectionArgument = CommandComponent.<CommandSender, String>builder()
            .name("collection")
            .parser(StringParser.stringParser())
            .suggestionProvider(collectionsSuggestions);

        final SuggestionProvider<CommandSender> minigameSuggestions = (context, input) -> CompletableFuture.completedFuture(
            Arrays.stream(MinigameType.values())
                .map(Enum::name)
                .map(Suggestion::suggestion)
                .collect(Collectors.toList())
        );

        commandManager.command(
            commandManager.commandBuilder("advancedhunt", "ah")
                .literal("help")
                .permission("advancedhunt.help")
                .handler(context -> help(context.sender()))
        );

        commandManager.command(
            commandManager.commandBuilder("advancedhunt", "ah")
                .literal("rewards")
                .permission("advancedhunt.admin.rewards")
                .handler(context -> rewards(context.sender()))
        );

        commandManager.command(
            commandManager.commandBuilder("advancedhunt", "ah")
                .literal("collection")
                .permission("advancedhunt.admin.editor")
                .handler(context -> editor(context.sender()))
        );

        commandManager.command(
            commandManager.commandBuilder("advancedhunt", "ah")
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

        commandManager.command(
            commandManager.commandBuilder("advancedhunt", "ah")
                .literal("collection")
                .literal("create")
                .required("name", StringParser.stringParser())
                .permission("advancedhunt.admin.collection.create")
                .handler(context -> createCollection(context.sender(), context.get("name")))
        );

        commandManager.command(
                commandManager.commandBuilder("advancedhunt", "ah")
                        .literal("collection")
                        .literal("edit")
                        .required("name", StringParser.stringParser(), collectionsSuggestions)
                        .permission("advancedhunt.admin.collection.edit")
                        .handler(context -> editCollection(context.sender(), context.get("name")))
        );

        commandManager.command(
            commandManager.commandBuilder("advancedhunt", "ah")
                .literal("reload")
                .permission("advancedhunt.admin.reload")
                .handler(context -> reload(context.sender()))
        );

        commandManager.command(
            commandManager.commandBuilder("advancedhunt", "ah")
                .literal("list")
                .permission("advancedhunt.list")
                .handler(context -> list(context.sender(), null))
        );

        commandManager.command(
            commandManager.commandBuilder("advancedhunt", "ah")
                .literal("list")
                .required(collectionArgument)
                .permission("advancedhunt.list")
                .handler(context -> list(context.sender(), context.get("collection")))
        );

        commandManager.command(
            commandManager.commandBuilder("advancedhunt", "ah")
                .literal("leaderboard")
                .required(collectionArgument)
                .permission("advancedhunt.leaderboard")
                .handler(context -> leaderboard(context.sender(), context.get("collection")))
        );

        commandManager.command(
            commandManager.commandBuilder("advancedhunt", "ah")
                .literal("progress")
                .required(collectionArgument)
                .permission("advancedhunt.progress")
                .handler(context -> progress(context.sender(), context.get("collection")))
        );

        commandManager.command(
            commandManager.commandBuilder("advancedhunt", "ah")
                .literal("place")
                .required(collectionArgument)
                .permission("advancedhunt.admin.place")
                .handler(context -> placeMode(context.sender(), context.get("collection")))
        );

        commandManager.command(
            commandManager.commandBuilder("advancedhunt", "ah")
                .literal("reset")
                .literal("all")
                .permission("advancedhunt.admin.reset")
                .handler(context -> resetAll(context.sender()))
        );

        commandManager.command(
            commandManager.commandBuilder("advancedhunt", "ah")
                .literal("reset")
                .literal("collection")
                .required(collectionArgument)
                .permission("advancedhunt.admin.reset")
                .handler(context -> resetCollection(context.sender(), context.get("collection")))
        );

        commandManager.command(
            commandManager.commandBuilder("advancedhunt", "ah")
                .literal("reset")
                .literal("player")
                .required("player", StringParser.stringParser(), playerNameSuggestions)
                .permission("advancedhunt.admin.reset")
                .handler(context -> resetPlayer(context.sender(), context.get("player")))
        );

        commandManager.command(
            commandManager.commandBuilder("advancedhunt", "ah")
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

        commandManager.command(
            commandManager.commandBuilder("advancedhunt", "ah")
                .literal("minigame")
                .required("type", StringParser.stringParser(), minigameSuggestions)
                .permission("advancedhunt.minigame")
                .handler(context -> minigame(context.sender(), context.get("type")))
        );

        commandManager.command(
                commandManager.commandBuilder("advancedhunt", "ah")
                        .literal("migrate")
                        .literal("yaml")
                        .permission("advancedhunt.admin.migrate")
                        .handler(context -> migrate(context.sender(), "YAML"))
        );

        commandManager.command(
                commandManager.commandBuilder("advancedhunt", "ah")
                        .literal("migrate")
                        .literal("sqlite")
                        .permission("advancedhunt.admin.migrate")
                        .handler(context -> migrate(context.sender(), "SQLITE"))
        );

        commandManager.command(
                commandManager.commandBuilder("advancedhunt", "ah")
                        .literal("migrate")
                        .literal("mysql")
                        .permission("advancedhunt.admin.migrate")
                        .handler(context -> migrate(context.sender(), "MYSQL"))
        );
    }

    public void help(CommandSender sender) {
        sender.sendMessage(plugin.getMessageManager().getMessageList("command.help").toArray(new String[0]));
    }

    public void rewards(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.not_player"));
            return;
        }
        
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
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.not_player"));
            return;
        }
        new CollectionEditorMenu((Player) sender, plugin).open();
    }

    public void reload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getMessageManager().reloadMessages();
        plugin.getParticleManager().reload();
        plugin.getSoundManager().reload();
        plugin.getDataRepository().reload();
        plugin.getTreasureManager().loadTreasures();
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.not_player"));
            return;
        }

        Optional<Collection> collectionOpt = plugin.getCollectionManager().getCollectionByName(collectionName);
        if (collectionOpt.isPresent()) {
            new LeaderboardMenu(player, plugin, collectionOpt.get(), null).open();
        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.collection_not_found"));
        }
    }

    public void progress(CommandSender sender, String collectionName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.not_player"));
            return;
        }

        Optional<Collection> collectionOpt = plugin.getCollectionManager().getCollectionByName(collectionName);
        if (collectionOpt.isPresent()) {
            Collection collection = collectionOpt.get();
            
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
        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.collection_not_found"));
        }
    }

    public void editCollection(CommandSender sender, String name) {
        if(!(sender instanceof Player)) return;
        Optional<Collection> collectionOpt = plugin.getCollectionManager().getCollectionByName(name);
        if (collectionOpt.isPresent()) {
            Collection collection = collectionOpt.get();

            new CollectionSettingsMenu((Player) sender, plugin, collection).open();
        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.collection_not_found"));
        }
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
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.not_player"));
            return;
        }
        Player player = (Player) sender;
        
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
            
            // Invalidate cache for all online players
            for (Player p : Bukkit.getOnlinePlayers()) {
                plugin.getPlayerManager().invalidate(p.getUniqueId());
            }
            sender.sendMessage(plugin.getMessageManager().getMessage("command.reset.all_success", 
                "%count%", String.valueOf(count)));
        });
    }

    public void resetCollection(CommandSender sender, String collectionName) {
        plugin.getCollectionManager().getCollectionByName(collectionName).ifPresentOrElse(collection -> {
            plugin.getDataRepository().resetCollectionProgress(collection.getId()).thenAccept(count -> {
                // Clear the particle manager cache for this collection
                plugin.getParticleManager().clearGlobalCache(collection.getId());
                
                // Invalidate cache for all online players
                for (Player p : Bukkit.getOnlinePlayers()) {
                    plugin.getPlayerManager().invalidate(p.getUniqueId());
                }
                sender.sendMessage(plugin.getMessageManager().getMessage("command.reset.collection_success",
                    "%collection%", collection.getName(),
                    "%count%", String.valueOf(count)));
            });
        }, () -> {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.collection_not_found"));
        });
    }

    public void resetPlayer(CommandSender sender, String playerName) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.reset.player_not_found"));
            return;
        }

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
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.reset.player_not_found"));
            return;
        }

        plugin.getCollectionManager().getCollectionByName(collectionName).ifPresentOrElse(collection -> {
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
            });
        }, () -> {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.collection_not_found"));
        });
    }

    public void minigame(CommandSender sender, String typeName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.not_player"));
            return;
        }

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

    private void migrate(CommandSender sender, String type) {
        sender.sendMessage(plugin.getMessageManager().getMessage("command.migration.started"));

        DataRepository targetRepo;
        if (type.equalsIgnoreCase("YAML")) {
            targetRepo = new YamlRepository(plugin);
        } else if (type.equalsIgnoreCase("SQLITE")) {
            targetRepo = new SqlRepository(plugin, null, 0, null, null, null, true);
        } else {
            String host = plugin.getConfig().getString("migration.target.host");
            int port = plugin.getConfig().getInt("migration.target.port");
            String database = plugin.getConfig().getString("migration.target.database");
            String username = plugin.getConfig().getString("migration.target.username");
            String password = plugin.getConfig().getString("migration.target.password");
            targetRepo = new SqlRepository(plugin, host, port, database, username, password, false);
        }

        targetRepo.init();

        plugin.getMigrationService()
                .migrate(plugin.getDataRepository(), targetRepo)
                .thenRun(() -> {
                    sender.sendMessage(plugin.getMessageManager().getMessage("command.migration.success"));
                    targetRepo.shutdown();
                })
                .exceptionally(e -> {
                    sender.sendMessage(plugin.getMessageManager().getMessage("command.migration.failed"));
                    e.printStackTrace();
                    targetRepo.shutdown();
                    return null;
                });
    }
}
