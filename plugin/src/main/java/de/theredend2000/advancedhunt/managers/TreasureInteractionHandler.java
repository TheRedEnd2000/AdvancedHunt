package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.reward.RewardsMenu;
import de.theredend2000.advancedhunt.model.CollectionRewardHolder;
import de.theredend2000.advancedhunt.model.PlayerData;
import de.theredend2000.advancedhunt.model.TreasureCore;
import de.theredend2000.advancedhunt.model.TreasureRewardHolder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TreasureInteractionHandler {

    private static volatile TreasureInteractionHandler instance;

    private final Main plugin;
    private final TreasureManager treasureManager;
    private final PlayerManager playerManager;
    private final CollectionManager collectionManager;
    private final RewardManager rewardManager;
    private final PlaceModeManager placeModeManager;

    /** Tracks players currently in the collection flow to prevent double collections */
    private final Set<UUID> playersCollecting = ConcurrentHashMap.newKeySet();

    /**
     * Gets the singleton instance, creating it if necessary.
     *
     * @param plugin the Main plugin instance
     * @return the shared TreasureInteractionHandler instance
     */
    public static TreasureInteractionHandler getInstance(Main plugin) {
        TreasureInteractionHandler localRef = instance;
        if (localRef == null) {
            synchronized (TreasureInteractionHandler.class) {
                localRef = instance;
                if (localRef == null) {
                    instance = localRef = new TreasureInteractionHandler(plugin);
                }
            }
        }
        return localRef;
    }

    /**
     * Resets the singleton instance. Call this on plugin disable/reload.
     */
    public static synchronized void reset() {
        instance = null;
    }

    TreasureInteractionHandler(Main plugin) {
        this.plugin = plugin;
        this.treasureManager = plugin.getTreasureManager();
        this.playerManager = plugin.getPlayerManager();
        this.collectionManager = plugin.getCollectionManager();
        this.rewardManager = plugin.getRewardManager();
        this.placeModeManager = plugin.getPlaceModeManager();
    }

    /**
     * Handles the sneaking admin reward editor case.
     *
     * @return true if the interaction was consumed (player was sneaking)
     */
    public boolean handleSneakRewardsEditor(Player player, TreasureCore treasureCore) {
        if (player == null || treasureCore == null) return false;
        if (!player.isSneaking()) return false;

        if (!player.hasPermission("advancedhunt.admin.rewards")) {
            return true;
        }

        treasureManager.getFullTreasureAsync(treasureCore.getId()).thenAccept(treasure -> {
            if (treasure == null) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                RewardsMenu menu = new RewardsMenu(player, plugin, new TreasureRewardHolder(plugin, treasure));
                collectionManager.getCollectionById(treasure.getCollectionId()).ifPresent(collection ->
                        menu.setAlternateContext(new CollectionRewardHolder(plugin, collection))
                );
                menu.open();
            });
        });

        return true;
    }

    public void handleTreasureCollect(Player player, TreasureCore treasureCore) {
        if (player == null || treasureCore == null) return;

        UUID playerId = player.getUniqueId();

        // Prevent double collection - if player is already collecting, ignore
        if (!playersCollecting.add(playerId)) {
            return;
        }

        // Guard is released inside handleTreasureCollectInternal (or its async callbacks)
        // to ensure it covers the full duration of the claim flow.
        handleTreasureCollectInternal(player, treasureCore, playerId);
    }

    private void handleTreasureCollectInternal(Player player, TreasureCore treasureCore, UUID playerId) {
        if (placeModeManager.isInPlaceMode(player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("treasure.placemode"));
            plugin.getSoundManager().playPlaceModeCollectDeny(player);
            playersCollecting.remove(playerId);
            return;
        }

        // Guard against interactions before player data is loaded.
        if (!playerManager.isPlayerDataLoaded(player.getUniqueId())) {
            playersCollecting.remove(playerId);
            return;
        }

        PlayerData data = playerManager.getPlayerData(player.getUniqueId());

        // asyncGuardHeld: true when an async callback will release the guard later.
        // Used to decide whether to release synchronously after ifPresent.
        final boolean[] asyncGuardHeld = {false};

        collectionManager.getCollectionById(treasureCore.getCollectionId()).ifPresent(collection -> {
            if (!collectionManager.isCollectionAvailable(collection)) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getSoundManager().playCollectionUnavailable(player);
                    player.sendMessage(plugin.getMessageManager().getMessage("collection.unavailable",
                            "%collection%", collection.getName()));

                    collectionManager.getNextActivation(collection).ifPresent(nextTime -> {
                        String timeStr = plugin.getMessageManager().formatDateTime(nextTime);
                        player.sendMessage(plugin.getMessageManager().getMessage("collection.available_at",
                                "%time%", timeStr));
                    });
                });
                playersCollecting.remove(playerId);
                return;
            }

            if (data.hasFound(treasureCore.getId())) {
                player.sendMessage(plugin.getMessageManager().getMessage("treasure.already_found"));
                plugin.getSoundManager().playTreasureAlreadyFound(player);
                playersCollecting.remove(playerId);
                return;
            }

            plugin.getFireworkManager().spawnFireworkRocket(treasureCore.getLocation().clone().add(0.5, 1, 0.5));

            if (collection.isSinglePlayerFind()) {
                // Guard is held until the async callback releases it.
                asyncGuardHeld[0] = true;
                plugin.getDataRepository().getPlayersWhoFound(treasureCore.getId()).thenAccept(claimers -> {
                    if (!claimers.isEmpty()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(plugin.getMessageManager().getMessage("treasure.already_claimed_global"));
                            plugin.getSoundManager().playTreasureClaimedByOther(player);
                        });
                        playersCollecting.remove(playerId);
                        return;
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        claimTreasure(player, treasureCore, data);
                        playersCollecting.remove(playerId);
                    });
                }).exceptionally(ex -> {
                    playersCollecting.remove(playerId);
                    return null;
                });
            } else {
                claimTreasure(player, treasureCore, data);
                playersCollecting.remove(playerId);
            }
        });

        // Release guard if no collection was found (ifPresent body never ran) and no
        // async path took ownership of releasing it.
        if (!asyncGuardHeld[0]) {
            playersCollecting.remove(playerId);
        }
    }

    private void claimTreasure(Player player, TreasureCore treasureCore, PlayerData data) {
        data.addFoundTreasure(treasureCore.getId());
        playerManager.savePlayerData(player.getUniqueId());
        player.sendMessage(plugin.getMessageManager().getMessage("treasure.found"));

        collectionManager.getCollectionById(treasureCore.getCollectionId()).ifPresent(collection -> {
            if (collection.isSinglePlayerFind()) {
                plugin.getParticleManager().markTreasureAsGloballyClaimed(treasureCore.getId());
            }
        });

        treasureManager.getFullTreasureAsync(treasureCore.getId()).thenAccept(treasure -> {
            if (treasure != null && treasure.getRewards() != null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        collectionManager.getCollectionById(treasureCore.getCollectionId()).ifPresent(c ->
                                rewardManager.giveRewards(player, treasure.getRewards(), c)
                        )
                );
            }
        });

        collectionManager.getCollectionById(treasureCore.getCollectionId()).ifPresent(c ->
                collectionManager.checkCompletion(player, c)
        );
    }
}
