package de.theredend2000.advancedhunt.commands;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.data.SqlRepository;
import de.theredend2000.advancedhunt.data.YamlRepository;
import de.theredend2000.advancedhunt.managers.MigrationService;
import de.theredend2000.advancedhunt.managers.minigame.MinigameType;
import de.theredend2000.advancedhunt.menu.collection.*;
import de.theredend2000.advancedhunt.menu.place.PlacePresetGroupMenu;
import de.theredend2000.advancedhunt.menu.reward.RewardsMenu;
import de.theredend2000.advancedhunt.model.*;
import de.theredend2000.advancedhunt.model.Collection;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.block.TileState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitRunnable;
import org.incendo.cloud.Command;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AdvancedHuntCommand {

    private static final String DEBUG_PLACED_MARKER = "AH_DEBUG_PLACED";

    private record PaletteEntry(Material type, PlayerProfile ownerProfile, String materialName, String blockState, String nbtData) {
    }

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

        commandManager.command(
            playerBuilder()
                .literal("placepresets")
                .permission("advancedhunt.admin.place_presets")
                .handler(context -> new PlacePresetGroupMenu((Player) context.sender(), plugin).open())
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
                playerBuilder()
                        .literal("collection")
                        .literal("delete")
                        .required("name", StringParser.stringParser(), collectionsSuggestions)
                        .permission("advancedhunt.admin.collection.delete")
                        .handler(context -> deleteCollection((Player) context.sender(), context.get("name")))
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

        // ==================== Debug Commands ====================
        commandManager.command(
            playerBuilder()
                .literal("debug")
                .literal("hint")
                .permission("advancedhunt.admin")
                .handler(context -> debugHint((Player) context.sender()))
        );

        commandManager.command(
            playerBuilder()
                .literal("debug")
                .literal("placecollection")
                .required(collectionArg)
                .required("amount", StringParser.stringParser())
                .permission("advancedhunt.admin")
                .handler(context -> debugPlaceCollectionRandom((Player) context.sender(), context.get("collection"), context.get("amount")))
        );

        commandManager.command(
            playerBuilder()
                .literal("debug")
                .literal("placecollection")
                .required(collectionArg)
                .required("amount", StringParser.stringParser())
                .literal("random")
                .permission("advancedhunt.admin")
                .handler(context -> debugPlaceCollectionRandom((Player) context.sender(), context.get("collection"), context.get("amount")))
        );

        commandManager.command(
            playerBuilder()
                .literal("debug")
                .literal("placecollection")
                .required(collectionArg)
                .required("amount", StringParser.stringParser())
                .literal("plane")
                .permission("advancedhunt.admin")
                .handler(context -> debugPlaceCollectionPlane((Player) context.sender(), context.get("collection"), context.get("amount")))
        );

        commandManager.command(
            playerBuilder()
                .literal("debug")
                .literal("removecollectionplaced")
                .required(collectionArg)
                .permission("advancedhunt.admin")
                .handler(context -> debugRemoveCollectionPlaced((Player) context.sender(), context.get("collection")))
        );

        // ==================== Migration Commands ====================
        SuggestionProvider<CommandSender> migrationTypes = (context, input) ->
            CompletableFuture.completedFuture(List.of("yaml", "sqlite", "mysql").stream()
                .map(Suggestion::suggestion).collect(Collectors.toList()));

        commandManager.command(
            baseBuilder()
                .literal("migrate")
                .required("type", StringParser.stringParser(), migrationTypes)
                .flag(commandManager.flagBuilder("force"))
                .flag(commandManager.flagBuilder("purge"))
                .permission("advancedhunt.admin.migrate")
                .handler(context -> {
                    boolean force = context.flags().isPresent("force");
                    boolean purge = context.flags().isPresent("purge");
                    migrate(context.sender(), context.<String>get("type").toUpperCase(), force, purge);
                })
        );
    }

    // ==================== Helper Methods ====================

    private Command.Builder<CommandSender> baseBuilder() {
        String[] parts = plugin.getConfig().getString("command.name", "advancedhunt|ah").split("\\|");
        return commandManager.commandBuilder(parts[0], Arrays.copyOfRange(parts, 1, parts.length));
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
        plugin.getSoundManager().reload();

        // Storage backend (supports storage.type swap)
        plugin.reloadStorageBackend();

        plugin.getParticleManager().reload();
        plugin.getHintManager().cancelAllVisualHints();
        plugin.getHintManager().reloadConfig();
        plugin.getProximityManager().reloadConfig();

        // Rebuild in-memory indexes/caches
        plugin.getTreasureManager().loadTreasures();
        CompletableFuture<Void> collectionsReload = plugin.getCollectionManager().reloadCollections();
        CompletableFuture<Void> presetsReload = plugin.getRewardPresetManager().reloadPresets();
        plugin.getLeaderboardManager().forceUpdate();

        // Ensure cached player progress is refreshed after backend edits
        Bukkit.getOnlinePlayers().forEach(p -> plugin.getPlayerManager().invalidate(p.getUniqueId()));

        CompletableFuture.allOf(collectionsReload, presetsReload).whenComplete((v, ex) -> {
            if (ex != null) {
                plugin.getLogger().warning("Reload failed: " + ex.getMessage());
            }

            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(
                    plugin.getMessageManager().getMessage(ex == null ? "command.reload.success" : "command.reload.failed")
            ));
        });
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

    public void deleteCollection(Player player, String name){
        withCollection(player, name, collection ->
                plugin.getCollectionManager().deleteCollection(collection.getId()).thenRun(() -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(plugin.getMessageManager().getMessage("feedback.settings.delete.success", "%collection%", collection.getName()));
                    });
                })
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
        Optional<TreasureCore> treasureOpt =
                plugin.getHintManager().findRandomUnfoundTreasure(player);
        
        if (treasureOpt.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getMessage("hint.no_unfound_nearby"));
            // Apply failure cooldown if configured
            plugin.getHintManager().applyFailureCooldown(player);
            return;
        }

        TreasureCore treasure = treasureOpt.get();

        // Get minigame type from config
        String minigameTypeStr = plugin.getConfig().getString("minigames.hint.minigame-type", "REACTION");
        MinigameType minigameType;
        
        if (minigameTypeStr.equalsIgnoreCase("RANDOM")) {
            // Randomly select between REACTION and MEMORY
            MinigameType[] types = MinigameType.values();
            minigameType = types[new Random().nextInt(types.length)];
        } else {
            try {
                minigameType = MinigameType.valueOf(minigameTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(plugin.getMessageManager().getMessage("minigame.error.invalid_type"));
                plugin.getLogger().warning("Invalid hint minigame type in config: " + minigameTypeStr);
                return;
            }
        }

        // Start minigame with hint delivery on success
        plugin.getMinigameManager().startMinigame(player, minigameType, (success) -> {
            if (success) {
                player.sendMessage(plugin.getMessageManager().getMessage("hint.success"));
                plugin.getHintManager().deliverHint(player, treasure);
            } else {
                player.sendMessage(plugin.getMessageManager().getMessage("hint.minigame_failed"));
                // Apply failure cooldown if configured
                plugin.getHintManager().applyFailureCooldown(player);
            }
        });
    }

    private void debugHint(Player player) {
        // Debug command to test visual hints without minigame/cooldown
        sendDebugChat(player, "&e[DEBUG] Searching for nearby treasure...");
        
        // Find a random unfound treasure in range
        Optional<TreasureCore> treasureOpt = plugin.getHintManager().findRandomUnfoundTreasure(player);
        
        if (treasureOpt.isEmpty()) {
            sendDebugChat(player, "&c[DEBUG] No unfound treasures nearby in active collections!");
            return;
        }

        TreasureCore treasure = treasureOpt.get();
        sendDebugChat(player, "&a[DEBUG] Found treasure! Delivering hint...");
        
        // Directly deliver hint (bypasses cooldown and minigame)
        plugin.getHintManager().deliverHint(player, treasure);
        
        sendDebugChat(player, "&e[DEBUG] Hint delivered. Visual effects active if enabled in config.");
    }

    private void debugPlaceCollectionRandom(Player player, String collectionName, String amountRaw) {
        int amount;
        try {
            amount = Integer.parseInt(amountRaw);
        } catch (NumberFormatException e) {
            sendDebugChat(player, "&c[DEBUG] Amount must be a number.");
            return;
        }

        if (amount <= 0) {
            sendDebugChat(player, "&c[DEBUG] Amount must be >= 1.");
            return;
        }

        List<PaletteEntry> palette = getHotbarPaletteEntries(player);
        if (palette.isEmpty()) {
            sendDebugChat(player, "&c[DEBUG] Put at least one placeable block in your hotbar (slots 1-9). ");
            return;
        }

        Optional<Collection> collectionOpt = plugin.getCollectionManager().getCollectionByName(collectionName);
        if (collectionOpt.isEmpty()) {
            sendDebugChat(player, "&c[DEBUG] Unknown collection: &f" + collectionName);
            return;
        }
        Collection collection = collectionOpt.get();

        sendDebugChat(player, "&e[DEBUG] Placing &f" + amount + "&e treasures &7(random)&e for collection &b" + collection.getName() + "&e...");

        final int radius = 12;
        final int verticalRadius = 8;
        final long maxAttemptsLong = Math.max((long) amount * 25L, 50L);
        final int maxAttempts = (int) Math.min(Integer.MAX_VALUE, maxAttemptsLong);
        final Random random = new Random();
        final NamespacedKey debugPlacedKey = new NamespacedKey(plugin, "debug_placed");
        final Location base = player.getLocation().clone();

        final List<Reward> defaultRewards = new ArrayList<>();
        Optional.ofNullable(collection.getDefaultTreasureRewardPresetId())
                .flatMap(defaultPresetId -> plugin.getRewardPresetManager().getPreset(RewardPresetType.TREASURE, defaultPresetId))
                .ifPresent(preset -> defaultRewards.addAll(preset.getRewards()));
        final List<Reward> sharedRewards = defaultRewards.isEmpty() ? Collections.emptyList() : List.copyOf(defaultRewards);

        final int batchSize = 250; // tuned to avoid long single-tick stalls
        final int maxAttemptsPerTick = Math.max(batchSize * 30, 500);
        final Set<String> reserved = new HashSet<>(Math.min(amount * 2, 32_768));

        final AtomicInteger saved = new AtomicInteger(0);
        final AtomicInteger saveFailed = new AtomicInteger(0);

        final int saveBatchSize = 200;
        final int maxInFlightBatches = 2;
        final AtomicInteger inFlightBatches = new AtomicInteger(0);
        final List<Treasure> pendingSaveBatch = new ArrayList<>(saveBatchSize);

        new BukkitRunnable() {
            int placed = 0;
            int attempts = 0;
            boolean placingDone = false;

            @Override
            public void run() {
                if (!player.isOnline() || player.getWorld() == null) {
                    cancel();
                    return;
                }
                if (!placingDone && (placed >= amount || attempts >= maxAttempts)) {
                    placingDone = true;
                }

                if (placingDone && pendingSaveBatch.isEmpty() && inFlightBatches.get() == 0 && saved.get() + saveFailed.get() >= placed) {
                    sendActionBar(player, "&a[DEBUG] Done: &f" + placed + "&a/" + amount
                            + "&7 (Saved: " + saved.get() + ", Failed: " + saveFailed.get() + ")");
                    sendDebugChat(player, "&a[DEBUG] Placed &f" + placed + "&a/" + amount + "&a treasures.&7 Attempts: &f" + attempts + "&7. Saved: &f" + saved.get() + "&7, Failed: &f" + saveFailed.get() + "&7.");
                    if (placed < amount) {
                    sendDebugChat(player, "&e[DEBUG] Not enough valid spots found near you. Try moving to a flatter/open area.");
                    }
                    cancel();
                    return;
                }

                // Flush any remaining batch once placement is done.
                if (placingDone && !pendingSaveBatch.isEmpty() && inFlightBatches.get() < maxInFlightBatches) {
                    List<Treasure> batch = new ArrayList<>(pendingSaveBatch);
                    pendingSaveBatch.clear();
                    inFlightBatches.incrementAndGet();
                    plugin.getTreasureManager().addTreasuresBatchAsync(batch)
                            .thenRun(() -> {
                                saved.addAndGet(batch.size());
                                inFlightBatches.decrementAndGet();
                            })
                            .exceptionally(ex -> {
                                saveFailed.addAndGet(batch.size());
                                inFlightBatches.decrementAndGet();
                                return null;
                            });
                }

                int placedThisTick = 0;
                int attemptsThisTick = 0;
                while (!placingDone && placedThisTick < batchSize && attemptsThisTick < maxAttemptsPerTick && placed < amount && attempts < maxAttempts) {
                    // Backpressure: don't generate unlimited queued saves.
                    if (pendingSaveBatch.size() >= saveBatchSize && inFlightBatches.get() >= maxInFlightBatches) {
                        break;
                    }
                    attempts++;
                    attemptsThisTick++;

                    int dx = random.nextInt(radius * 2 + 1) - radius;
                    int dz = random.nextInt(radius * 2 + 1) - radius;
                    int dy = random.nextInt(verticalRadius * 2 + 1) - verticalRadius;
                    int x = base.getBlockX() + dx;
                    int y = base.getBlockY() + dy;
                    int z = base.getBlockZ() + dz;

                    int minY = base.getWorld().getMinHeight();
                    int maxY = base.getWorld().getMaxHeight() - 1;
                    if (y < minY || y > maxY) continue;

                    Block block = base.getWorld().getBlockAt(x, y, z);
                    if (!block.getType().isAir()) continue;

                    Location loc = block.getLocation();
                    String key = loc.getWorld().getUID() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
                    if (!reserved.add(key)) continue;
                    if (plugin.getTreasureManager().getTreasureCoreAt(loc) != null) continue;

                    PaletteEntry chosen = palette.get(random.nextInt(palette.size()));
                    block.setType(chosen.type(), false);

                    if (chosen.type() == Material.PLAYER_HEAD && chosen.ownerProfile() != null && block.getState() instanceof Skull skullState) {
                        try {
                            skullState.setOwnerProfile(chosen.ownerProfile());
                        } catch (Throwable ignored) {
                        }
                        skullState.update(true, false);
                    }

                    if (block.getState() instanceof TileState tileState) {
                        tileState.getPersistentDataContainer().set(debugPlacedKey, PersistentDataType.BYTE, (byte) 1);
                        tileState.update(true, false);
                    }

                    Treasure treasure = new Treasure(
                            plugin.getTreasureManager().generateUniqueTreasureId(),
                            collection.getId(),
                            loc,
                            sharedRewards,
                            chosen.nbtData(),
                            chosen.materialName(),
                            chosen.blockState()
                    );
                    pendingSaveBatch.add(treasure);

                    if (pendingSaveBatch.size() >= saveBatchSize && inFlightBatches.get() < maxInFlightBatches) {
                        List<Treasure> batch = new ArrayList<>(pendingSaveBatch);
                        pendingSaveBatch.clear();
                        inFlightBatches.incrementAndGet();
                        plugin.getTreasureManager().addTreasuresBatchAsync(batch)
                                .thenRun(() -> {
                                    saved.addAndGet(batch.size());
                                    inFlightBatches.decrementAndGet();
                                })
                                .exceptionally(ex -> {
                                    saveFailed.addAndGet(batch.size());
                                    inFlightBatches.decrementAndGet();
                                    return null;
                                });
                    }

                    placed++;
                    placedThisTick++;
                }

                if (amount > 0) {
                    if (!placingDone) {
                        sendActionBar(player, "&e[DEBUG] Placing: &f" + placed + "&e/" + amount
                                + "&7 (Saved: " + saved.get() + ", Failed: " + saveFailed.get() + ", Attempts: " + attempts + ")");
                    } else {
                        sendActionBar(player, "&e[DEBUG] Saving: &f" + saved.get() + "&e/" + placed
                                + "&7 (Failed: " + saveFailed.get() + ")");
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void debugPlaceCollectionPlane(Player player, String collectionName, String amountRaw) {
        int amount;
        try {
            amount = Integer.parseInt(amountRaw);
        } catch (NumberFormatException e) {
            sendDebugChat(player, "&c[DEBUG] Amount must be a number.");
            return;
        }

        if (amount <= 0) {
            sendDebugChat(player, "&c[DEBUG] Amount must be >= 1.");
            return;
        }

        List<PaletteEntry> palette = getHotbarPaletteEntries(player);
        if (palette.isEmpty()) {
            sendDebugChat(player, "&c[DEBUG] Put at least one placeable block in your hotbar (slots 1-9). ");
            return;
        }

        Optional<Collection> collectionOpt = plugin.getCollectionManager().getCollectionByName(collectionName);
        if (collectionOpt.isEmpty()) {
            sendDebugChat(player, "&c[DEBUG] Unknown collection: &f" + collectionName);
            return;
        }
        Collection collection = collectionOpt.get();

        sendDebugChat(player, "&e[DEBUG] Placing &f" + amount + "&e treasures &7(plane)&e for collection &b" + collection.getName() + "&e...");

        final List<Reward> defaultRewards = new ArrayList<>();
        Optional.ofNullable(collection.getDefaultTreasureRewardPresetId())
                .flatMap(defaultPresetId -> plugin.getRewardPresetManager().getPreset(RewardPresetType.TREASURE, defaultPresetId))
                .ifPresent(preset -> defaultRewards.addAll(preset.getRewards()));
        final List<Reward> sharedRewards = defaultRewards.isEmpty() ? Collections.emptyList() : List.copyOf(defaultRewards);

        final NamespacedKey debugPlacedKey = new NamespacedKey(plugin, "debug_placed");
        final Random random = new Random();

        final Location base = player.getLocation().clone();
        final int y = Math.min(base.getWorld().getMaxHeight() - 1, base.getBlockY() + 1);
        final int side = (int) Math.ceil(Math.sqrt(amount));
        final int start = -side / 2;

        final int batchSize = 400;

        final AtomicInteger saved = new AtomicInteger(0);
        final AtomicInteger saveFailed = new AtomicInteger(0);

        final int saveBatchSize = 200;
        final int maxInFlightBatches = 2;
        final AtomicInteger inFlightBatches = new AtomicInteger(0);
        final List<Treasure> pendingSaveBatch = new ArrayList<>(saveBatchSize);

        new BukkitRunnable() {
            int placed = 0;
            int checked = 0;
            int dx = 0;
            int dz = 0;
            boolean placingDone = false;

            @Override
            public void run() {
                if (!player.isOnline() || player.getWorld() == null) {
                    cancel();
                    return;
                }
                if (!placingDone && (placed >= amount || dz >= side)) {
                    placingDone = true;
                }

                if (placingDone && pendingSaveBatch.isEmpty() && inFlightBatches.get() == 0 && saved.get() + saveFailed.get() >= placed) {
                    sendActionBar(player, "&a[DEBUG] Done: &f" + placed + "&a/" + amount
                            + "&7 (Saved: " + saved.get() + ", Failed: " + saveFailed.get() + ")");
                    sendDebugChat(player, "&a[DEBUG] Placed &f" + placed + "&a/" + amount + "&a treasures on a plane.&7 Checked: &f" + checked + "&7. Saved: &f" + saved.get() + "&7, Failed: &f" + saveFailed.get() + "&7.");
                    if (placed < amount) {
                    sendDebugChat(player, "&e[DEBUG] Plane had blocked cells. Try an emptier area.");
                    }
                    cancel();
                    return;
                }

                if (placingDone && !pendingSaveBatch.isEmpty() && inFlightBatches.get() < maxInFlightBatches) {
                    List<Treasure> batch = new ArrayList<>(pendingSaveBatch);
                    pendingSaveBatch.clear();
                    inFlightBatches.incrementAndGet();
                    plugin.getTreasureManager().addTreasuresBatchAsync(batch)
                            .thenRun(() -> {
                                saved.addAndGet(batch.size());
                                inFlightBatches.decrementAndGet();
                            })
                            .exceptionally(ex -> {
                                saveFailed.addAndGet(batch.size());
                                inFlightBatches.decrementAndGet();
                                return null;
                            });
                }

                int placedThisTick = 0;
                while (!placingDone && placedThisTick < batchSize && placed < amount && dz < side) {
                    if (pendingSaveBatch.size() >= saveBatchSize && inFlightBatches.get() >= maxInFlightBatches) {
                        break;
                    }
                    int x = base.getBlockX() + start + dx;
                    int z = base.getBlockZ() + start + dz;
                    Block block = base.getWorld().getBlockAt(x, y, z);
                    checked++;

                    if (block.getType().isAir() && plugin.getTreasureManager().getTreasureCoreAt(block.getLocation()) == null) {
                        PaletteEntry chosen = palette.get(random.nextInt(palette.size()));
                        block.setType(chosen.type(), false);

                        if (chosen.type() == Material.PLAYER_HEAD && chosen.ownerProfile() != null && block.getState() instanceof Skull skullState) {
                            try {
                                skullState.setOwnerProfile(chosen.ownerProfile());
                            } catch (Throwable ignored) {
                            }
                            skullState.update(true, false);
                        }

                        if (block.getState() instanceof TileState tileState) {
                            tileState.getPersistentDataContainer().set(debugPlacedKey, PersistentDataType.BYTE, (byte) 1);
                            tileState.update(true, false);
                        }

                        Treasure treasure = new Treasure(
                                plugin.getTreasureManager().generateUniqueTreasureId(),
                                collection.getId(),
                                block.getLocation(),
                                sharedRewards,
                                chosen.nbtData(),
                                chosen.materialName(),
                                chosen.blockState()
                        );
                        pendingSaveBatch.add(treasure);

                        if (pendingSaveBatch.size() >= saveBatchSize && inFlightBatches.get() < maxInFlightBatches) {
                            List<Treasure> batch = new ArrayList<>(pendingSaveBatch);
                            pendingSaveBatch.clear();
                            inFlightBatches.incrementAndGet();
                            plugin.getTreasureManager().addTreasuresBatchAsync(batch)
                                    .thenRun(() -> {
                                        saved.addAndGet(batch.size());
                                        inFlightBatches.decrementAndGet();
                                    })
                                    .exceptionally(ex -> {
                                        saveFailed.addAndGet(batch.size());
                                        inFlightBatches.decrementAndGet();
                                        return null;
                                    });
                        }
                        placed++;
                        placedThisTick++;
                    }

                    dx++;
                    if (dx >= side) {
                        dx = 0;
                        dz++;
                    }
                }

                if (amount > 0) {
                    if (!placingDone) {
                        sendActionBar(player, "&e[DEBUG] Placing: &f" + placed + "&e/" + amount
                                + "&7 (Saved: " + saved.get() + ", Failed: " + saveFailed.get() + ", Checked: " + checked + ")");
                    } else {
                        sendActionBar(player, "&e[DEBUG] Saving: &f" + saved.get() + "&e/" + placed
                                + "&7 (Failed: " + saveFailed.get() + ")");
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void sendActionBar(Player player, String message) {
        try {
            String colored = ChatColor.translateAlternateColorCodes('&', message);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(colored));
        } catch (Throwable ignored) {
            // Best-effort only; don't fail debug commands if action bar isn't supported.
        }
    }

    private void sendDebugChat(Player player, String message) {
        String prefix = plugin.getMessageManager().getMessage("prefix", false);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }

    private List<PaletteEntry> getHotbarPaletteEntries(Player player) {
        List<PaletteEntry> entries = new ArrayList<>();
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot <= 8; slot++) {
            if (contents.length <= slot) break;
            ItemStack item = contents[slot];
            if (item == null) continue;
            Material type = item.getType();
            if (type.isAir()) continue;
            if (!type.isBlock()) continue;

            if (type == Material.PLAYER_WALL_HEAD) {
                type = Material.PLAYER_HEAD;
            }

            PlayerProfile ownerProfile = null;
            if (type == Material.PLAYER_HEAD) {
                ItemMeta meta = item.getItemMeta();
                if (meta instanceof SkullMeta skullMeta) {
                    try {
                        ownerProfile = skullMeta.getOwnerProfile();
                    } catch (Throwable ignored) {
                    }
                }
            }

            String blockState;
            try {
                blockState = type.createBlockData().getAsString();
            } catch (Throwable ignored) {
                // Fallback: if something goes wrong, keep old behavior by computing later.
                blockState = "";
            }

            String nbtData = DEBUG_PLACED_MARKER;
            if (type == Material.PLAYER_HEAD) {
                try {
                    String headNbt = NBT.get(item, ReadableNBT::toString);
                    if (headNbt != null && !headNbt.isEmpty()) {
                        nbtData = DEBUG_PLACED_MARKER + "\n" + headNbt;
                    }
                } catch (Throwable ignored) {
                }
            }

            entries.add(new PaletteEntry(type, ownerProfile, type.name(), blockState, nbtData));
        }
        return entries;
    }

    private void debugRemoveCollectionPlaced(Player player, String collectionName) {
        Optional<Collection> collectionOpt = plugin.getCollectionManager().getCollectionByName(collectionName);
        if (collectionOpt.isEmpty()) {
            sendDebugChat(player, "&c[DEBUG] Unknown collection: &f" + collectionName);
            return;
        }
        Collection collection = collectionOpt.get();

        List<TreasureCore> cores = new ArrayList<>(plugin.getTreasureManager().getTreasureCoresInCollection(collection.getId()));
        int total = cores.size();

        sendDebugChat(player, "&e[DEBUG] Removing &f" + total + "&e treasures for collection &b" + collection.getName() + "&e...");

        // Remove from in-memory caches immediately to avoid lookups seeing deleted treasures.
        plugin.getTreasureManager().removeCollection(collection.getId());

        AtomicInteger blocksProcessed = new AtomicInteger(0);
        AtomicInteger blocksRemoved = new AtomicInteger(0);
        AtomicInteger blocksSkippedUnloaded = new AtomicInteger(0);
        AtomicInteger dbDeleted = new AtomicInteger(-1);
        AtomicInteger dbFailed = new AtomicInteger(0);

        // Kick off repository-side bulk delete (fast for SQL; directory delete for YAML).
        plugin.getDataRepository().deleteTreasuresInCollection(collection.getId())
                .thenAccept(dbDeleted::set)
                .exceptionally(ex -> {
                    dbFailed.incrementAndGet();
                    dbDeleted.set(0);
                    return null;
                });

        final int batchSize = 600;
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                int processedThisTick = 0;
                while (index < total && processedThisTick < batchSize) {
                    TreasureCore core = cores.get(index++);
                    processedThisTick++;
                    blocksProcessed.incrementAndGet();

                    Location loc = core.getLocation();
                    World world = loc.getWorld();
                    if (world == null) {
                        continue;
                    }

                    int cx = loc.getBlockX() >> 4;
                    int cz = loc.getBlockZ() >> 4;
                    if (!world.isChunkLoaded(cx, cz)) {
                        blocksSkippedUnloaded.incrementAndGet();
                        continue;
                    }

                    Block block = world.getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                    if (!block.getType().isAir()) {
                        block.setType(Material.AIR, false);
                        blocksRemoved.incrementAndGet();
                    }
                }

                int deletedCount = dbDeleted.get();
                int deletedClamped = deletedCount < 0 ? 0 : Math.min(deletedCount, total);
                String deletePart = deletedCount >= 0
                    ? "&7 Deleted: &f" + deletedClamped + "&7/&f" + total
                    : "&7 Deleted: &f0&7/&f" + total + " &e(deleting...)";

                sendActionBar(player, "&e[DEBUG] Removing: &f" + blocksProcessed.get() + "&e/" + total
                    + "&7 (Blocks: " + blocksRemoved.get() + ", Skipped: " + blocksSkippedUnloaded.get() + ") " + deletePart);

                if (index >= total && dbDeleted.get() >= 0) {
                    sendActionBar(player, "&a[DEBUG] Done: &f" + total + "&a treasures" + "&7 (Deleted: " + dbDeleted.get() + ")");
                    sendDebugChat(player, "&a[DEBUG] Removed collection treasures.&7 Total: &f" + total
                            + "&7, Blocks removed: &f" + blocksRemoved.get()
                            + "&7, Skipped (unloaded): &f" + blocksSkippedUnloaded.get()
                            + "&7, DB deleted: &f" + dbDeleted.get() + "&7.");
                    if (dbFailed.get() > 0) {
                        sendDebugChat(player, "&c[DEBUG] Note: DB delete reported an error; check console logs.");
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void migrate(CommandSender sender, String type, boolean force, boolean purge) {
        // Create target repository based on type
        DataRepository targetRepo = createTargetRepository(type, sender);
        if (targetRepo == null) {
            return; // Error message already sent
        }

        sender.sendMessage(plugin.getMessageManager().getMessage("command.migration.started"));
        targetRepo.init();

        // Check for existing data and proceed with migration
        checkExistingDataAndMigrate(sender, type, targetRepo, force, purge);
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

    private void checkExistingDataAndMigrate(CommandSender sender, String type, DataRepository targetRepo, boolean force, boolean purge) {
        CompletableFuture.allOf(
                targetRepo.loadCollections(),
                targetRepo.getAllTreasureUUIDs(),
            targetRepo.getAllPlayerUUIDs(),
            targetRepo.loadRewardPresets(RewardPresetType.TREASURE),
            targetRepo.loadRewardPresets(RewardPresetType.COLLECTION)
        ).thenCompose(v -> {
            int existingCollections = safeGetSize(targetRepo.loadCollections());
            int existingTreasures = safeGetSize(targetRepo.getAllTreasureUUIDs());
            int existingPlayerData = safeGetSize(targetRepo.getAllPlayerUUIDs());
            int existingTreasurePresets = safeGetSize(targetRepo.loadRewardPresets(RewardPresetType.TREASURE));
            int existingCollectionPresets = safeGetSize(targetRepo.loadRewardPresets(RewardPresetType.COLLECTION));

            boolean hasExistingData = existingCollections > 0
                || existingTreasures > 0
                || existingPlayerData > 0
                || existingTreasurePresets > 0
                || existingCollectionPresets > 0;

            if (hasExistingData && !force && !purge) {
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

            CompletableFuture<Void> prepareTarget;
            if (purge && hasExistingData) {
                prepareTarget = purgeTargetRepository(targetRepo);
            } else {
                prepareTarget = CompletableFuture.completedFuture(null);
            }

            return prepareTarget.thenCompose(v2 -> {
                final AtomicInteger lastPercentSent = new AtomicInteger(-1);
                final AtomicReference<String> lastStageSent = new AtomicReference<>("");
                final AtomicLong lastUpdateMs = new AtomicLong(System.currentTimeMillis());
                final AtomicReference<MigrationService.MigrationProgress> lastProgress = new AtomicReference<>(new MigrationService.MigrationProgress(0, "loading", 0, 0));

                final BukkitRunnable heartbeat;
                if (sender instanceof Player player) {
                    heartbeat = new BukkitRunnable() {
                        @Override
                        public void run() {
                            long now = System.currentTimeMillis();
                            if (now - lastUpdateMs.get() < 4000L) {
                                return;
                            }
                            var p = lastProgress.get();
                            sendActionBar(player, plugin.getMessageManager().getMessage(
                                    "command.migration.progress_actionbar",
                                    false,
                                    "%percent%", String.valueOf(p.percent()),
                                    "%current%", String.valueOf(p.current()),
                                    "%total%", String.valueOf(p.total()),
                                    "%stage%", String.valueOf(p.stage())));
                        }
                    };
                    heartbeat.runTaskTimer(plugin, 40L, 40L);
                } else {
                    heartbeat = null;
                }

                return plugin.getMigrationService().migrate(plugin.getDataRepository(), targetRepo, progress -> {
                    lastProgress.set(progress);

                    int percent = progress.percent();
                    String stage = String.valueOf(progress.stage());
                    long now = System.currentTimeMillis();

                    boolean shouldSend = false;
                    if (lastPercentSent.get() != percent) {
                        shouldSend = true;
                    } else if (!stage.equals(lastStageSent.get())) {
                        // Important for long 0% phases (e.g., scanning large SQL tables)
                        shouldSend = true;
                    } else if (now - lastUpdateMs.get() > 2000L) {
                        // Heartbeat for console (players get ActionBar heartbeat too)
                        shouldSend = true;
                    }

                    if (!shouldSend) {
                        return;
                    }

                    lastPercentSent.set(percent);
                    lastStageSent.set(stage);
                    lastUpdateMs.set(now);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (sender instanceof Player player) {
                            sendActionBar(player, plugin.getMessageManager().getMessage(
                                    "command.migration.progress_actionbar",
                                    false,
                                    "%percent%", String.valueOf(progress.percent()),
                                    "%current%", String.valueOf(progress.current()),
                                    "%total%", String.valueOf(progress.total()),
                                    "%stage%", String.valueOf(progress.stage())));
                            return;
                        }

                        sender.sendMessage(plugin.getMessageManager().getMessage("command.migration.progress",
                                "%percent%", String.valueOf(progress.percent()),
                                "%current%", String.valueOf(progress.current()),
                                "%total%", String.valueOf(progress.total()),
                                "%stage%", String.valueOf(progress.stage())));
                    });
                }).whenComplete((v3, t) -> {
                    if (heartbeat != null) {
                        Bukkit.getScheduler().runTask(plugin, heartbeat::cancel);
                    }
                });
            });
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
            // Close the migration target repository before switching the live backend.
            // This is important for SQLite and some JDBC drivers to release file/connection locks.
            try {
                targetRepo.shutdown();
            } catch (Exception ignored) {
            }

            // Persist the new backend choice to config (delegated to Main).
            plugin.applyMigratedStorageConfig(type);

            sender.sendMessage(plugin.getMessageManager().getMessage("command.migration.success"));

            if (sender instanceof Player player) {
                sendActionBar(player, "");
            }

            // Reuse the plugin's existing reload routine to swap repository + refresh caches.
            reload(sender);

            if (type != null && type.equalsIgnoreCase("MYSQL")) {
                // Reset migration-only config section to defaults now that live storage is configured.
                resetMySQLConfig(sender);
            }
        })).exceptionally(e -> {
            // Only show an error if it's not our intentional abort
            if (!(e.getCause() instanceof IllegalStateException &&
                    e.getCause().getMessage().equals("Migration aborted - existing data"))) {
                sender.sendMessage(plugin.getMessageManager().getMessage("command.migration.failed"));
                e.printStackTrace();
            }
            if (sender instanceof Player player) {
                Bukkit.getScheduler().runTask(plugin, () -> sendActionBar(player, ""));
            }
            targetRepo.shutdown();
            return null;
        });
    }

    private CompletableFuture<Void> purgeTargetRepository(DataRepository targetRepo) {
        CompletableFuture<Void> deletePresets = targetRepo.loadRewardPresets(RewardPresetType.TREASURE)
            .thenCompose(presets -> {
                if (presets == null || presets.isEmpty()) {
                    return CompletableFuture.completedFuture(null);
                }
                CompletableFuture<?>[] futures = presets.stream()
                    .map(preset -> targetRepo.deleteRewardPreset(RewardPresetType.TREASURE, preset.getId()))
                    .toArray(CompletableFuture[]::new);
                return CompletableFuture.allOf(futures);
            }).thenCompose(v -> targetRepo.loadRewardPresets(RewardPresetType.COLLECTION)
                .thenCompose(presets -> {
                    if (presets == null || presets.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }
                    CompletableFuture<?>[] futures = presets.stream()
                        .map(preset -> targetRepo.deleteRewardPreset(RewardPresetType.COLLECTION, preset.getId()))
                        .toArray(CompletableFuture[]::new);
                    return CompletableFuture.allOf(futures);
                }));

        CompletableFuture<Void> deleteCollections = targetRepo.loadCollections()
            .thenCompose(collections -> {
                if (collections == null || collections.isEmpty()) {
                    return CompletableFuture.completedFuture(null);
                }
                CompletableFuture<?>[] futures = collections.stream()
                    .map(collection -> targetRepo.deleteCollection(collection.getId()))
                    .toArray(CompletableFuture[]::new);
                return CompletableFuture.allOf(futures);
            });

        CompletableFuture<Void> deleteRemainingTreasures = targetRepo.getAllTreasureUUIDs()
            .thenCompose(treasureIds -> {
                if (treasureIds == null || treasureIds.isEmpty()) {
                    return CompletableFuture.completedFuture(null);
                }
                CompletableFuture<?>[] futures = treasureIds.stream()
                    .map(targetRepo::deleteTreasure)
                    .toArray(CompletableFuture[]::new);
                return CompletableFuture.allOf(futures);
            });

        CompletableFuture<Void> resetProgress = targetRepo.resetAllProgress().thenApply(v -> null);

        return deletePresets
            .thenCompose(v -> deleteCollections)
            .thenCompose(v -> deleteRemainingTreasures)
            .thenCompose(v -> resetProgress);
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
