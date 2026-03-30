package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.PlaceModeManager;
import de.theredend2000.advancedhunt.managers.TreasureInteractionHandler;
import de.theredend2000.advancedhunt.managers.TreasureManager;
import de.theredend2000.advancedhunt.model.Reward;
import de.theredend2000.advancedhunt.model.Treasure;
import de.theredend2000.advancedhunt.model.TreasureCore;
import de.theredend2000.advancedhunt.util.RewardPresetUtils;
import dev.lone.itemsadder.api.Events.*;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.UUID;

/**
 * ItemsAdder integration listener.
 *
 * This class is only registered when ItemsAdder is installed and the API is present.
 */
public class ItemsAdderIntegrationListener implements Listener {

    private final Main plugin;
    private final TreasureManager treasureManager;
    private final PlaceModeManager placeModeManager;
    private final TreasureInteractionHandler treasureInteractionHandler;

    public ItemsAdderIntegrationListener(Main plugin) {
        this.plugin = plugin;
        this.treasureManager = plugin.getTreasureManager();
        this.placeModeManager = plugin.getPlaceModeManager();
        this.treasureInteractionHandler = TreasureInteractionHandler.getInstance(plugin);
    }

    @EventHandler
    public void onFurniturePlaceSuccess(FurniturePlacedEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        if (!placeModeManager.isInPlaceMode(player)) return;

        UUID collectionId = placeModeManager.getCollectionId(player);
        if (collectionId == null) return;

        Entity entity = event.getBukkitEntity();
        Location loc = toBlockLocation(entity.getLocation());

        if (treasureManager.getTreasureCoreAt(loc) != null) return;

        List<Reward> rewards = RewardPresetUtils.getDefaultTreasureRewards(plugin, collectionId);

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
    public void onCustomBlockPlace(CustomBlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        if (!placeModeManager.isInPlaceMode(player)) return;

        UUID collectionId = placeModeManager.getCollectionId(player);
        if (collectionId == null) return;

        if (event.getBlock() == null) return;
        Location loc = event.getBlock().getLocation();

        if (treasureManager.getTreasureCoreAt(loc) != null) return;

        String namespacedId = event.getNamespacedID();
        if (namespacedId == null || namespacedId.isEmpty()) return;

        List<Reward> rewards = RewardPresetUtils.getDefaultTreasureRewards(plugin, collectionId);

        Treasure treasure = new Treasure(
                treasureManager.generateUniqueTreasureId(),
                collectionId,
                loc,
                rewards,
                null,
                "ITEMS_ADDER",
                namespacedId
        );

        treasureManager.addTreasure(treasure);
        player.sendMessage(plugin.getMessageManager().getMessage("treasure.added"));
        plugin.getSoundManager().playTreasurePlaced(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
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

        if (treasureInteractionHandler.handleSneakRewardsEditor(player, treasureCore)) return;
        treasureInteractionHandler.handleTreasureCollect(player, treasureCore);
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

        if (treasureInteractionHandler.handleSneakRewardsEditor(player, treasureCore)) return;
        treasureInteractionHandler.handleTreasureCollect(player, treasureCore);
    }

    @EventHandler(priority = EventPriority.HIGH)
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

    private static Location toBlockLocation(Location location) {
        return location.getBlock().getLocation();
    }
}
