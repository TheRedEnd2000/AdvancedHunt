package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.PlaceModeManager;
import de.theredend2000.advancedhunt.managers.TreasureManager;
import de.theredend2000.advancedhunt.model.Treasure;
import de.theredend2000.advancedhunt.util.BlockUtils;
import de.theredend2000.advancedhunt.util.ItemsAdderAdapter;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.UUID;

public class PlaceModeListener implements Listener {

    private final Main plugin;
    private final PlaceModeManager placeModeManager;
    private final TreasureManager treasureManager;

    public PlaceModeListener(Main plugin) {
        this.plugin = plugin;
        this.placeModeManager = plugin.getPlaceModeManager();
        this.treasureManager = plugin.getTreasureManager();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!placeModeManager.isInPlaceMode(player)) return;

        UUID collectionId = placeModeManager.getCollectionId(player);
        if (collectionId == null) return;

        Block block = event.getBlock();

        String nbtData;
        try {
            nbtData = NBT.get(block.getState(), ReadableNBT::toString);
        } catch (Exception ignored) {
            nbtData = null;
        }

        String material;
        String blockState;

        if (ItemsAdderAdapter.isItemsAdderBlock(block)) {
            material = "ITEMS_ADDER";
            blockState = ItemsAdderAdapter.getCustomBlockId(block);
        } else {
            material = block.getType().name();
            blockState = BlockUtils.getBlockStateString(block);
        }

        Treasure treasure = new Treasure(
                treasureManager.generateUniqueTreasureId(),
                collectionId,
                block.getLocation(),
                new ArrayList<>(),
                nbtData,
                material,
                blockState
        );

        treasureManager.addTreasure(treasure);
        player.sendMessage(plugin.getMessageManager().getMessage("treasure.added"));
        plugin.getSoundManager().playTreasurePlaced(player);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!placeModeManager.isInPlaceMode(event.getPlayer())) return;

        Treasure treasure = treasureManager.getTreasureAt(event.getBlock().getLocation());
        if (treasure != null) {
            treasureManager.deleteTreasure(treasure);
            event.getPlayer().sendMessage(plugin.getMessageManager().getMessage("treasure.removed"));
            plugin.getSoundManager().playTreasureBroken(event.getPlayer());
        } else {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getMessageManager().getMessage("command.place_mode.no_break"));
            plugin.getSoundManager().playPlaceModeBreakDeny(event.getPlayer());
        }
    }
}
