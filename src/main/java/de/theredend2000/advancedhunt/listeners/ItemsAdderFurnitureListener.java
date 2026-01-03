package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.*;
import de.theredend2000.advancedhunt.menu.reward.RewardsMenu;
import de.theredend2000.advancedhunt.model.*;
import dev.lone.itemsadder.api.Events.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ItemsAdder integration listener.
 *
 * This class is only registered when ItemsAdder is installed and the API is present.
 */
public class ItemsAdderFurnitureListener implements Listener {

    private final Main plugin;
    private final TreasureManager treasureManager;
    private final PlayerManager playerManager;
    private final CollectionManager collectionManager;
    private final RewardManager rewardManager;
    private final PlaceModeManager placeModeManager;

    public ItemsAdderFurnitureListener(Main plugin) {
        this.plugin = plugin;
        this.treasureManager = plugin.getTreasureManager();
        this.playerManager = plugin.getPlayerManager();
        this.collectionManager = plugin.getCollectionManager();
        this.rewardManager = plugin.getRewardManager();
        this.placeModeManager = plugin.getPlaceModeManager();
    }

    @EventHandler
    public void onFurniturePlaceSuccess(FurniturePlaceSuccessEvent event) {
        Player player = event.getPlayer();
        if (!placeModeManager.isInPlaceMode(player)) return;

        UUID collectionId = placeModeManager.getCollectionId(player);
        if (collectionId == null) return;

        Entity entity = event.getBukkitEntity();
        Location loc = toBlockLocation(entity.getLocation());

        if (treasureManager.getTreasureCoreAt(loc) != null) return;

        List<Reward> rewards = new ArrayList<>();
        plugin.getCollectionManager().getCollectionById(collectionId)
                .map(Collection::getDefaultTreasureRewardPresetId)
                .flatMap(defaultPresetId -> plugin.getRewardPresetManager().getPreset(RewardPresetType.TREASURE, defaultPresetId))
                .ifPresent(preset -> rewards.addAll(preset.getRewards()));

        Treasure treasure = new Treasure(
                treasureManager.generateUniqueTreasureId(),
                collectionId,
                loc,
                rewards,
                null,
                "ITEMS_ADDER",
                event.getNamespacedID()
        );

        treasureManager.addTreasure(treasure);
        player.sendMessage(plugin.getMessageManager().getMessage("treasure.added"));
        plugin.getSoundManager().playTreasurePlaced(player);
    }

    @EventHandler
    public void onFurnitureBreak(FurnitureBreakEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getBukkitEntity();
        Location loc = toBlockLocation(entity.getLocation());

        TreasureCore core = treasureManager.getTreasureCoreAt(loc);
        if (core == null) return;

        // Allow breaking while in place mode (we delete the treasure)
        if (placeModeManager.isInPlaceMode(player)) {
            Treasure treasure = treasureManager.getFullTreasure(core.getId());
            if (treasure != null) {
                treasureManager.deleteTreasure(treasure);
                player.sendMessage(plugin.getMessageManager().getMessage("treasure.removed"));
                plugin.getSoundManager().playTreasureBroken(player);
                return;
            }
            // Fallback: if something went weird, deny breaking.
            event.setCancelled(true);
            player.sendMessage(plugin.getMessageManager().getMessage("command.place_mode.no_break"));
            plugin.getSoundManager().playPlaceModeBreakDeny(player);
            return;
        }

        // Prevent breaking treasures by normal players
        event.setCancelled(true);
        player.sendMessage(plugin.getMessageManager().getMessage("treasure.protected"));
        plugin.getSoundManager().playBlockProtected(player);
    }

    @EventHandler
    public void onFurnitureInteract(FurnitureInteractEvent event) {
        Entity entity = event.getBukkitEntity();
        Location loc = toBlockLocation(entity.getLocation());

        TreasureCore treasureCore = treasureManager.getTreasureCoreAt(loc);
        if (treasureCore == null) return;

        Player player = event.getPlayer();
        event.setCancelled(true);

        // Shift + interact: Admin reward editor
        if (player.isSneaking()) {
            if (!player.hasPermission("advancedhunt.admin.rewards")) {
                player.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                return;
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
            return;
        }

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

    @EventHandler
    public void onCustomBlockInteract(CustomBlockInteractEvent event) {
        if (event.getAction() == null) return;
        if (event.getBlockClicked() == null) return;

        TreasureCore treasureCore = treasureManager.getTreasureCoreAt(event.getBlockClicked().getLocation());
        if (treasureCore == null) return;

        // Let the normal logic run (avoid double messages if PlayerInteract also fires)
        // by cancelling the ItemsAdder event and handling it here.
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (player == null) return;

        if (player.isSneaking()) {
            if (!player.hasPermission("advancedhunt.admin.rewards")) {
                player.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                return;
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
            return;
        }

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

    @EventHandler
    public void onCustomBlockBreak(CustomBlockBreakEvent event) {
        if (event.getBlock() == null) return;

        TreasureCore core = treasureManager.getTreasureCoreAt(event.getBlock().getLocation());
        if (core == null) return;

        Player player = event.getPlayer();
        if (player == null) return;

        // Allow breaking while in place mode (PlaceModeListener handles deletion)
        if (placeModeManager.isInPlaceMode(player)) return;

        event.setCancelled(true);
        player.sendMessage(plugin.getMessageManager().getMessage("treasure.protected"));
        plugin.getSoundManager().playBlockProtected(player);
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

    private static Location toBlockLocation(Location location) {
        return location.getBlock().getLocation();
    }
}
