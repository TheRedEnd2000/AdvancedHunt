package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.CollectionManager;
import de.theredend2000.advancedhunt.managers.TreasureInteractionHandler;
import de.theredend2000.advancedhunt.managers.TreasureManager;
import de.theredend2000.advancedhunt.model.TreasureCore;
import de.theredend2000.advancedhunt.platform.PlatformAccess;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractListener implements Listener {

    private final Main plugin;
    private final TreasureManager treasureManager;
    private final TreasureInteractionHandler treasureInteractionHandler;
    private final CollectionManager collectionManager;

    public PlayerInteractListener(Main plugin) {
        this.plugin = plugin;
        this.treasureManager = plugin.getTreasureManager();
        this.treasureInteractionHandler = TreasureInteractionHandler.getInstance(plugin);
        this.collectionManager = plugin.getCollectionManager();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!PlatformAccess.get().isMainHandInteract(event)) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (isItemsAdderBlock(block)) return;

        TreasureCore treasureCore = treasureManager.getTreasureCoreAt(block.getLocation());
        if (treasureCore == null) return;

        Player player = event.getPlayer();

        if (player.isSneaking()) {
            event.setCancelled(true);
            treasureInteractionHandler.handleSneakRewardsEditor(player, treasureCore);
            return;
        }

        if (plugin.getTreasureVisibilityManager().shouldHideFoundForPlayer(treasureCore, player)) {
            event.setCancelled(true);
            plugin.getTreasureVisibilityManager().hideFoundTreasureForPlayer(player, treasureCore);

            collectionManager.getCollectionById(treasureCore.getCollectionId()).ifPresent(col -> {
                if (col.isSinglePlayerFind()) {
                    player.sendMessage(plugin.getMessageManager().getMessage("treasure.already_claimed_global"));
                    plugin.getSoundManager().playTreasureClaimedByOther(player);
                } else {
                    player.sendMessage(plugin.getMessageManager().getMessage("treasure.already_found"));
                    plugin.getSoundManager().playTreasureAlreadyFound(player);
                }
            });

            return;
        }

        treasureInteractionHandler.handleTreasureCollect(player, treasureCore);
    }

    /**
     * Checks if the given block is an ItemsAdder custom block.
     * Returns false if ItemsAdder is not installed or an error occurs.
     *
     * @param block the block to check
     * @return true if this is an ItemsAdder custom block, false otherwise
     */
    private boolean isItemsAdderBlock(Block block) {
        try {
            if (Bukkit.getPluginManager().getPlugin("ItemsAdder") == null) {
                return false;
            }
            return dev.lone.itemsadder.api.CustomBlock.byAlreadyPlaced(block) != null;
        } catch (NoClassDefFoundError | Exception e) {
            return false;
        }
    }
}
