package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.PlaceModeManager;
import de.theredend2000.advancedhunt.managers.TreasureManager;
import de.theredend2000.advancedhunt.model.TreasureCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to   = event.getTo();


        if (to == null) return;
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        for(Player p : Bukkit.getOnlinePlayers()) {
            Bukkit.broadcastMessage(p.getDisplayName()+" "+p.getAllowFlight());
        }

        checkPlayer(event.getPlayer());
    }

    private final Set<UUID> flightGranted = ConcurrentHashMap.newKeySet();

    private void checkPlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        if (!plugin.getTreasureVisibilityManager().isBypassEnabled(player)) return;

        Location feet = player.getLocation();
        Location blockBelow = feet.clone().subtract(0, 1, 0);

        TreasureCore core = plugin.getTreasureManager().getTreasureCoreAt(blockBelow);
        boolean onHiddenTreasure = core != null && isCollectionHidden(core.getCollectionId());



        if (onHiddenTreasure) {
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
                flightGranted.add(player.getUniqueId());
                Bukkit.broadcastMessage("added "+player.getName());
            }
        } else {
            if (flightGranted.remove(player.getUniqueId())) {
                player.setAllowFlight(false);
                Bukkit.broadcastMessage("removed "+player.getName());
            }
        }
    }

    private boolean isCollectionHidden(UUID collectionId) {
        return plugin.getCollectionManager().getCollectionById(collectionId)
                .map(collection -> {
                    if (!collection.isHideWhenNotAvailable()) return false;
                    if (!collection.isEnabled()) return true;
                    return !plugin.getCollectionManager().isCollectionAvailable(collection);
                })
                .orElse(false);
    }
}
