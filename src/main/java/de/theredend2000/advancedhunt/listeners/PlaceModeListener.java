package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.PlaceModeManager;
import de.theredend2000.advancedhunt.managers.TreasureManager;
import de.theredend2000.advancedhunt.model.Reward;
import de.theredend2000.advancedhunt.model.Treasure;
import de.theredend2000.advancedhunt.util.BlockUtils;
import de.theredend2000.advancedhunt.util.ItemsAdderAdapter;
import de.theredend2000.advancedhunt.util.RewardPresetUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
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

        // ItemsAdder furniture placement is handled by ItemsAdder events (entity-based).
        // BlockPlaceEvent may still fire with AIR or a base block, which would create invalid treasures.
        ItemStack placedItem = event.getItemInHand();
        if (ItemsAdderAdapter.isCustomFurniture(placedItem)) {
            return;
        }
        if (block.getType().isAir()) {
            return;
        }

        // If a treasure already exists at this location, do nothing.
        if (treasureManager.getTreasureCoreAt(block.getLocation()) != null) {
            return;
        }

        // ItemsAdder custom blocks are registered via ItemsAdder's CustomBlockPlaceEvent.
        // Avoid creating a duplicate treasure here.
        if (ItemsAdderAdapter.isCustomBlockItem(placedItem)) {
            return;
        }

        handlePlacedBlock(player, collectionId, block.getLocation(), placedItem);
    }

    private void handlePlacedBlock(Player player, UUID collectionId, org.bukkit.Location location, ItemStack placedItem) {
        if (player == null || collectionId == null || location == null) return;
        if (!placeModeManager.isInPlaceMode(player)) return;

        Block block = location.getBlock();
        if (block.getType().isAir()) return;

        if (treasureManager.getTreasureCoreAt(location) != null) {
            return;
        }

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
            if ((blockState == null || blockState.isEmpty()) && placedItem != null) {
                blockState = ItemsAdderAdapter.getCustomBlockId(placedItem);
            }
        } else if (placedItem != null && ItemsAdderAdapter.isCustomBlockItem(placedItem)) {
            // Fallback: treat as ItemsAdder custom block even if block inspection fails.
            material = "ITEMS_ADDER";
            blockState = ItemsAdderAdapter.getCustomBlockId(placedItem);
        } else {
            material = block.getType().name();
            blockState = BlockUtils.getBlockStateString(block);
        }

        if ("ITEMS_ADDER".equalsIgnoreCase(material) && (blockState == null || blockState.isEmpty())) {
            plugin.getLogger().warning("Failed to resolve ItemsAdder block ID for placed block at " + location);
            return;
        }

        List<Reward> rewards = new ArrayList<>(RewardPresetUtils.getDefaultTreasureRewards(plugin, collectionId));

        Treasure treasure = new Treasure(
                treasureManager.generateUniqueTreasureId(),
                collectionId,
            location,
            rewards,
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
