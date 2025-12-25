package de.theredend2000.advancedhunt.menu;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.model.Treasure;
import de.theredend2000.advancedhunt.model.TreasureCore;
import de.theredend2000.advancedhunt.util.HeadHelper;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class CollectionListMenu extends PagedMenu {

    private final UUID collectionId;

    public CollectionListMenu(Player playerMenuUtility, UUID collectionId, Main plugin) {
        super(playerMenuUtility, plugin);
        this.collectionId = collectionId;
    }

    @Override
    public String getMenuName() {
        return "Collection Content";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
    }

    @Override
    public void setMenuItems() {
        addMenuBorder();
        
        // Add leaderboard button at slot 8 (top right corner)
        addLeaderboardButton();

        // Use lightweight TreasureCore for menu display
        List<TreasureCore> treasures = plugin.getTreasureManager().getTreasureCoresInCollection(collectionId);

        if (treasures == null || treasures.isEmpty()) {
            return;
        }

        // Calculate pagination
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, treasures.size());
        
        this.hasNextPage = endIndex < treasures.size();

        for (int i = startIndex; i < endIndex; i++) {
            TreasureCore treasureCore = treasures.get(i);
            int displayIndex = i - startIndex;
            ItemStack item = createTreasureItem(treasureCore, i);

            addPagedItem(displayIndex, item, e -> handleTreasureClick(e, treasureCore));
        }
        
        // Update index for pagination buttons
        this.index = endIndex - 1;
    }
    
    /**
     * Adds a button to open the leaderboard menu for this collection.
     */
    private void addLeaderboardButton() {
        plugin.getDataRepository().loadCollections().thenAccept(collections -> {
            Collection collection = collections.stream()
                    .filter(c -> c.getId().equals(collectionId))
                    .findFirst()
                    .orElse(null);
            
            if (collection != null) {
                Collection finalCollection = collection;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    ItemStack leaderboardButton = new ItemBuilder(Material.GOLDEN_HELMET)
                            .setDisplayName(plugin.getMessageManager().getMessage("gui.leaderboard.view_button.name", false))
                            .setLore(plugin.getMessageManager().getMessageList("gui.leaderboard.view_button.lore", false))
                            .build();
                    
                    addButton(8, leaderboardButton, e -> {
                        new LeaderboardMenu((Player) e.getWhoClicked(), plugin, finalCollection, this).open();
                    });
                });
            }
        });
    }

    private ItemStack createTreasureItem(TreasureCore treasureCore, int index) {
        // For menu display, we use the material from TreasureCore
        // Only load full treasure if we need NBT data for heads
        Material material = Material.matchMaterial(treasureCore.getMaterial());
        if (material == null) {
            material = Material.CHEST;
        }
        
        ItemStack item = new ItemStack(material);
        ItemBuilder builder = new ItemBuilder(item);
        
        // For player heads, we need to load full treasure for NBT data
        if (HeadHelper.isPlayerHead(item)) {
            Treasure fullTreasure = plugin.getTreasureManager().getFullTreasure(treasureCore.getId());
            if (fullTreasure != null) {
                String texture = HeadHelper.getTextureFromNbt(fullTreasure.getNbtData());
                Bukkit.broadcastMessage("Texture: "+texture);
                if (texture != null) {
                    builder.setSkullTexture(texture);
                }
            }
        }

        int playerFoundSize = plugin.getDataRepository().getPlayersWhoFound(treasureCore.getId()).join().size();
        builder.setDisplayName(ChatColor.GOLD + "Treasure #" + (index + 1));
        builder.addLoreLine(ChatColor.GRAY+"Player size found: "+playerFoundSize);
        builder.addLoreLine("");
        builder.addLoreLine(ChatColor.GRAY + "Location:");
        builder.addLoreLine(ChatColor.GRAY + "X: " + treasureCore.getLocation().getBlockX());
        builder.addLoreLine(ChatColor.GRAY + "Y: " + treasureCore.getLocation().getBlockY());
        builder.addLoreLine(ChatColor.GRAY + "Z: " + treasureCore.getLocation().getBlockZ());
        builder.addLoreLine(ChatColor.GRAY + "World: " + treasureCore.getLocation().getWorld().getName());
        builder.addLoreLine("");
        
        if (playerMenuUtility.hasPermission("advancedhunt.admin.teleport")) {
            builder.addLoreLine(plugin.getMessageManager().getMessage("gui.collection_content.treasure_item.lore_teleport", false));
        }
        if (playerMenuUtility.hasPermission("advancedhunt.admin.view_finders")) {
            builder.addLoreLine(plugin.getMessageManager().getMessage("gui.collection_content.treasure_item.lore_who_found", false));
        }

        return builder.build();
    }

    /**
     * Handles clicks on treasure items with support for left/ right-click actions
     * and Bedrock/GeyserMC compatibility via the fallback action menu.
     */
    private void handleTreasureClick(InventoryClickEvent e, TreasureCore treasureCore) {
        Player p = (Player) e.getWhoClicked();
        ClickType clickType = e.getClick();

        // Right-click: Open the "Who Found" menu
        if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            if (!p.hasPermission("advancedhunt.admin.view_finders")) {
                p.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                return;
            }
            new WhoFoundMenu(p, plugin, treasureCore.getId(), this).open();
            return;
        }

        // Left-click: Teleport to treasure
        if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
            if (!p.hasPermission("advancedhunt.admin.teleport")) {
                p.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                return;
            }
            p.closeInventory();
            teleportToTreasure(p, treasureCore);
            return;
        }

        // Other click types (Bedrock fallback): Open an action menu - need full treasure
        Treasure fullTreasure = plugin.getTreasureManager().getFullTreasure(treasureCore.getId());
        if (fullTreasure != null) {
            new TreasureActionMenu(p, plugin, fullTreasure, this).open();
        }
    }

    /**
     * Safely teleports a player to a treasure location.
     * Includes chunk loading, safe spawn position finding, and error handling.
     */
    private void teleportToTreasure(Player player, TreasureCore treasureCore) {
        Location location = treasureCore.getLocation();
        
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
