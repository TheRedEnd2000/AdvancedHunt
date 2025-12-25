package de.theredend2000.advancedhunt.menu;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.Treasure;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * A Bedrock/GeyserMC-compatible action menu for treasure interactions.
 * Provides explicit buttons for all actions since Bedrock players can only left-click.
 */
public class TreasureActionMenu extends Menu {

    private final UUID treasureId;
    private final Treasure treasure;

    public TreasureActionMenu(Player player, Main plugin, Treasure treasure, Menu previousMenu) {
        super(player, plugin);
        this.treasureId = treasure.getId();
        this.treasure = treasure;
        this.previousMenu = previousMenu;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.treasure_action.title", false);
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        // All handled via buttons
    }

    @Override
    public void setMenuItems() {
        fillBorders(FILLER_GLASS);

        // Teleport Button
        ItemStack teleportItem = new ItemBuilder(Material.ENDER_PEARL)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.treasure_action.teleport.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.treasure_action.teleport.lore", false))
                .build();

        addButton(11, teleportItem, e -> {
            Player p = (Player) e.getWhoClicked();
            p.closeInventory();
            teleportToTreasure(p, treasure);
        }, "advancedhunt.admin.teleport");

        // View Finders Button
        ItemStack viewFindersItem = new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.treasure_action.view_finders.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.treasure_action.view_finders.lore", false))
                .build();

        addButton(13, viewFindersItem, e -> {
            Player p = (Player) e.getWhoClicked();
            new WhoFoundMenu(p, plugin, treasureId, this).open();
        }, "advancedhunt.admin.view_finders");

        // Back Button
        ItemStack backItem = new ItemBuilder(Material.BARRIER)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.common.back", false))
                .build();

        addButton(15, backItem, e -> {
            openPreviousMenu();
        });
    }

    /**
     * Safely teleports a player to a treasure location.
     * Includes chunk loading, safe spawn position finding, and error handling.
     */
    private void teleportToTreasure(Player player, Treasure treasure) {
        Location location = treasure.getLocation();
        
        if (location == null || location.getWorld() == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("gui.treasure_action.teleport.invalid_location"));
            return;
        }

        // Load chunk if needed (synchronous for compatibility)
        if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            // Load chunk with a 1-tick delay for safety
            location.getChunk().load();
            Bukkit.getScheduler().runTaskLater(plugin, () -> performSafeTeleport(player, location), 1L);
        } else {
            performSafeTeleport(player, location);
        }
    }

    /**
     * Performs the actual teleport with safety checks.
     * Adds vertical offset and checks for safe spawn.
     */
    private void performSafeTeleport(Player player, Location treasureLocation) {
        // Create safe teleport location - above the treasure block
        Location safeLoc = treasureLocation.clone().add(0.5, 1.5, 0.5);
        safeLoc.setYaw(player.getLocation().getYaw());
        safeLoc.setPitch(player.getLocation().getPitch());

        // Check if location above is safe (not solid)
        Location checkLoc = safeLoc.clone();
        Material blockAbove = checkLoc.getBlock().getType();
        Material blockAbove2 = checkLoc.clone().add(0, 1, 0).getBlock().getType();

        // If not safe, try to find a safe location nearby
        if (blockAbove.isSolid() || blockAbove2.isSolid()) {
            Location safeLocation = findSafeLocation(treasureLocation);
            if (safeLocation != null) {
                safeLoc = safeLocation;
                safeLoc.setYaw(player.getLocation().getYaw());
                safeLoc.setPitch(player.getLocation().getPitch());
                player.sendMessage(plugin.getMessageManager().getMessage("gui.treasure_action.teleport.adjusted"));
            } else {
                player.sendMessage(plugin.getMessageManager().getMessage("gui.treasure_action.teleport.unsafe"));
                return;
            }
        }

        // Perform teleport
        player.teleport(safeLoc);
        player.sendMessage(plugin.getMessageManager().getMessage("gui.treasure_action.teleport.success"));
    }

    /**
     * Finds a safe location near the treasure if the default position is blocked.
     * Searches in a 3x3 horizontal area and up to 5 blocks vertically.
     */
    private Location findSafeLocation(Location center) {
        for (int y = 0; y <= 5; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location testLoc = center.clone().add(x + 0.5, y + 1.5, z + 0.5);
                    Material below = testLoc.clone().subtract(0, 1, 0).getBlock().getType();
                    Material at = testLoc.getBlock().getType();
                    Material above = testLoc.clone().add(0, 1, 0).getBlock().getType();

                    // Need solid ground below, and air at player position and above
                    if (below.isSolid() && !at.isSolid() && !above.isSolid()) {
                        return testLoc;
                    }
                }
            }
        }
        return null;
    }
}
