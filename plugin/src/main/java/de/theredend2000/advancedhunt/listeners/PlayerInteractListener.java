package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
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

    public PlayerInteractListener(Main plugin) {
        this.plugin = plugin;
        this.treasureManager = plugin.getTreasureManager();
        this.treasureInteractionHandler = TreasureInteractionHandler.getInstance(plugin);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!PlatformAccess.get().isMainHandInteract(event)) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        // Skip ItemsAdder blocks - let ItemsAdderIntegrationListener handle them
        if (isItemsAdderBlock(block)) return;

        // Use lightweight core first - fast O(1) lookup
        TreasureCore treasureCore = treasureManager.getTreasureCoreAt(block.getLocation());
        if (treasureCore == null) return;

        Player player = event.getPlayer();

        // Shift + Right-Click: Admin reward editor (requires permission)
        if (player.isSneaking()) {
            event.setCancelled(true);
            treasureInteractionHandler.handleSneakRewardsEditor(player, treasureCore);
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
