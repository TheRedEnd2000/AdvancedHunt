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
            player.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
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

        try {
            handleTreasureCollectInternal(player, treasureCore);
        } finally {
            playersCollecting.remove(playerId);
        }
    }

    private void handleTreasureCollectInternal(Player player, TreasureCore treasureCore) {
        if (placeModeManager.isInPlaceMode(player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("treasure.placemode"));
            plugin.getSoundManager().playPlaceModeCollectDeny(player);
            return;
        }

        PlayerData data = playerManager.getPlayerData(player.getUniqueId());

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
                return;
            }

            if (data.hasFound(treasureCore.getId())) {
                player.sendMessage(plugin.getMessageManager().getMessage("treasure.already_found"));
                plugin.getSoundManager().playTreasureAlreadyFound(player);
                return;
            }

            plugin.getFireworkManager().spawnFireworkRocket(treasureCore.getLocation().clone().add(0.5, 1, 0.5));

            if (collection.isSinglePlayerFind()) {
                plugin.getDataRepository().getPlayersWhoFound(treasureCore.getId()).thenAccept(claimers -> {
                    if (!claimers.isEmpty()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(plugin.getMessageManager().getMessage("treasure.already_claimed_global"));
                            plugin.getSoundManager().playTreasureClaimedByOther(player);
                        });
                        return;
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> claimTreasure(player, treasureCore, data));
                });
            } else {
                claimTreasure(player, treasureCore, data);
            }
        });
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
