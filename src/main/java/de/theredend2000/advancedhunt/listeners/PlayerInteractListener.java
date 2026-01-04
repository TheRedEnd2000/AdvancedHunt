package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.TreasureInteractionHandler;
import de.theredend2000.advancedhunt.managers.TreasureManager;
import de.theredend2000.advancedhunt.model.TreasureCore;
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
    private final TreasureInteractionHandler treasureInteractionHandler;

    public PlayerInteractListener(Main plugin) {
        this.plugin = plugin;
        this.treasureManager = plugin.getTreasureManager();
        this.treasureInteractionHandler = new TreasureInteractionHandler(plugin);
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
            treasureInteractionHandler.handleSneakRewardsEditor(player, treasureCore);
            return;
        }

        treasureInteractionHandler.handleTreasureCollect(player, treasureCore);
    }
}
