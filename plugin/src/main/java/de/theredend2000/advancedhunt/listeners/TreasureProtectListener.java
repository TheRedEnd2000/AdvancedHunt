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
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.inventory.ItemStack;

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

        if(!player.hasPermission("advancedhunt.admin.place")) return;
        player.sendMessage(plugin.getMessageManager().getMessage("treasure.protected"));
        plugin.getSoundManager().playBlockProtected(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        TreasureCore core = treasureManager.getTreasureCoreAt(event.getBlock().getLocation());
        if (core == null) return;

        Player player = event.getPlayer();

        if (placeModeManager.isInPlaceMode(player)) return;

        event.setCancelled(true);
        if(!player.hasPermission("advancedhunt.admin.place")) return;
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (treasureManager.getTreasureCoreAt(event.getToBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockFade(BlockFadeEvent event) {
        if (treasureManager.getTreasureCoreAt(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (treasureManager.getTreasureCoreAt(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (treasureManager.getTreasureCoreAt(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Block targetBlock = event.getBlockClicked().getRelative(event.getBlockFace());
        if (treasureManager.getTreasureCoreAt(targetBlock.getLocation()) != null ||
            treasureManager.getTreasureCoreAt(event.getBlockClicked().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Block targetBlock = event.getBlockClicked().getRelative(event.getBlockFace());
        if (treasureManager.getTreasureCoreAt(targetBlock.getLocation()) != null ||
            treasureManager.getTreasureCoreAt(event.getBlockClicked().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockDispense(BlockDispenseEvent event) {
        ItemStack item = event.getItem();
        if (item.getType().toString().contains("BUCKET")) {
            org.bukkit.block.data.BlockData data = event.getBlock().getBlockData();
            if (data instanceof org.bukkit.block.data.Directional) {
                Block targetBlock = event.getBlock().getRelative(((org.bukkit.block.data.Directional) data).getFacing());
                if (treasureManager.getTreasureCoreAt(targetBlock.getLocation()) != null) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
