package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.PlaceModeManager;
import de.theredend2000.advancedhunt.managers.TreasureManager;
import de.theredend2000.advancedhunt.model.TreasureCore;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;

public class TreasureProtectListener implements Listener {

    private final Main plugin;
    private final TreasureManager treasureManager;
    private final PlaceModeManager placeModeManager;

    public TreasureProtectListener(Main plugin) {
        this.plugin = plugin;
        this.treasureManager = plugin.getTreasureManager();
        this.placeModeManager = plugin.getPlaceModeManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        TreasureCore core = treasureManager.getTreasureCoreAt(event.getBlock().getLocation());
        if (core == null) return;

        Player player = event.getPlayer();

        // Allow breaking while in place mode (PlaceModeListener handles deletion)
        if (placeModeManager.isInPlaceMode(player)) return;

        // Prevent breaking treasures by normal players
        event.setCancelled(true);
        player.sendMessage(plugin.getMessageManager().getMessage("treasure.protected"));
        plugin.getSoundManager().playBlockProtected(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        TreasureCore core = treasureManager.getTreasureCoreAt(event.getBlock().getLocation());
        if (core == null) return;

        Player player = event.getPlayer();

        event.setCancelled(true);
        player.sendMessage(plugin.getMessageManager().getMessage("treasure.protected"));
        plugin.getSoundManager().playBlockProtected(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(List<Block> blocks) {
        blocks.removeIf(block -> treasureManager.getTreasureCoreAt(block.getLocation()) != null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (treasureManager.getTreasureCoreAt(block.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (treasureManager.getTreasureCoreAt(block.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBurn(BlockBurnEvent event) {
        if (treasureManager.getTreasureCoreAt(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (treasureManager.getTreasureCoreAt(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }
}
