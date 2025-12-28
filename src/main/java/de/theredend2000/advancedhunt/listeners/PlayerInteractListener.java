package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.*;
import de.theredend2000.advancedhunt.menu.RewardsMenu;
import de.theredend2000.advancedhunt.model.CollectionRewardHolder;
import de.theredend2000.advancedhunt.model.PlayerData;
import de.theredend2000.advancedhunt.model.TreasureCore;
import de.theredend2000.advancedhunt.model.TreasureRewardHolder;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerInteractListener implements Listener {

    private final Main plugin;
    private final TreasureManager treasureManager;
    private final PlayerManager playerManager;
    private final CollectionManager collectionManager;
    private final RewardManager rewardManager;
    private final PlaceModeManager placeModeManager;

    public PlayerInteractListener(Main plugin) {
        this.plugin = plugin;
        this.treasureManager = plugin.getTreasureManager();
        this.playerManager = plugin.getPlayerManager();
        this.collectionManager = plugin.getCollectionManager();
        this.rewardManager = plugin.getRewardManager();
        this.placeModeManager = plugin.getPlaceModeManager();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        // Use lightweight core first - fast O(1) lookup
        TreasureCore treasureCore = treasureManager.getTreasureCoreAt(block.getLocation());
        if (treasureCore == null) return;

        Player player = event.getPlayer();

        // Shift + Right-Click: Admin reward editor (requires permission)
        if (player.isSneaking()) {
            event.setCancelled(true);
            if (!player.hasPermission("advancedhunt.admin.rewards")) {
                player.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                return;
            }
            
            // Need full treasure for rewards - load on demand
            treasureManager.getFullTreasureAsync(treasureCore.getId()).thenAccept(treasure -> {
                if (treasure == null) return;
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Create treasure rewards menu with collection context for switching
                    RewardsMenu menu = new RewardsMenu(player, plugin, new TreasureRewardHolder(plugin, treasure));
                    
                    // Add collection context if available, allowing switch between treasure and collection rewards
                    collectionManager.getCollectionById(treasure.getCollectionId()).ifPresent(collection -> {
                        menu.setAlternateContext(
                            new CollectionRewardHolder(plugin, collection),
                            "gui.rewards.collection_title"
                        );
                    });
                    
                    menu.open();
                });
            });
            return;
        }

        if(placeModeManager.isInPlaceMode(player)){
            player.sendMessage(plugin.getMessageManager().getMessage("treasure.placemode"));
            plugin.getSoundManager().playPlaceModeCollectDeny(player);
            return;
        }

        PlayerData data = playerManager.getPlayerData(player.getUniqueId());

        // Check if collection is currently available
        collectionManager.getCollectionById(treasureCore.getCollectionId()).ifPresent(collection -> {
            if (!collectionManager.isCollectionAvailable(collection)) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getSoundManager().playCollectionUnavailable(player);
                    player.sendMessage(plugin.getMessageManager().getMessage("collection.unavailable", 
                        "%collection%", collection.getName()));
                    
                    // Show when it will be available next (if applicable)
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
            
            // Collection is available, proceed with normal checks
            if (collection.isSinglePlayerFind()) {
                // Check if anyone has already claimed this treasure
                plugin.getDataRepository().getPlayersWhoFound(treasureCore.getId()).thenAccept(claimers -> {
                    if (!claimers.isEmpty()) {
                        // Someone already found it
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(plugin.getMessageManager().getMessage("treasure.already_claimed_global"));
                            plugin.getSoundManager().playTreasureClaimedByOther(player);
                        });
                        return;
                    }
                    // No one has claimed it yet, proceed with claiming
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        claimTreasure(player, treasureCore, data);
                    });
                });
            } else {
                // Normal per-player treasure
                claimTreasure(player, treasureCore, data);
            }
        });
    }

    private void claimTreasure(Player player, TreasureCore treasureCore, PlayerData data) {
        // Mark as found
        data.addFoundTreasure(treasureCore.getId());
        playerManager.savePlayerData(player.getUniqueId()); // Async save triggered
        player.sendMessage(plugin.getMessageManager().getMessage("treasure.found"));

        // Update particle manager cache for single-player-find collections
        collectionManager.getCollectionById(treasureCore.getCollectionId()).ifPresent(collection -> {
            if (collection.isSinglePlayerFind()) {
                plugin.getParticleManager().markTreasureAsGloballyClaimed(treasureCore.getId());
            }
        });

        // Give Rewards - need full treasure for this
        treasureManager.getFullTreasureAsync(treasureCore.getId()).thenAccept(treasure -> {
            if (treasure != null && treasure.getRewards() != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    rewardManager.giveRewards(player, treasure.getRewards());
                });
            }
        });

        // Check Collection Completion
        collectionManager.getCollectionById(treasureCore.getCollectionId()).ifPresent(c -> 
            collectionManager.checkCompletion(player, c)
        );
    }
}
