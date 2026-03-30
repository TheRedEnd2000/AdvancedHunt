package de.theredend2000.advancedhunt.menu.treasure;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.menu.PagedMenu;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Displays a paged list of players who have found a specific treasure.
 * Fetches data asynchronously to avoid blocking the main thread.
 */
public class WhoFoundMenu extends PagedMenu {

    private final UUID treasureId;
    private List<UUID> finderUuids;
    private boolean isLoading = true;

    public WhoFoundMenu(Player player, Main plugin, UUID treasureId, Menu previousMenu) {
        super(player, plugin);
        this.treasureId = treasureId;
        this.previousMenu = previousMenu;
        this.finderUuids = new ArrayList<>();
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.finders.title", false);
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        // All handled via buttons
    }

    @Override
    public void setMenuItems() {
        if (isLoading) {
            // Show loading indicator
            addMenuBorder();
            ItemStack loadingItem = new ItemBuilder(XMaterial.HOPPER)
                    .setDisplayName(plugin.getMessageManager().getMessage("gui.finders.loading", false))
                    .build();
            addStaticItem(22, loadingItem);
            
            // Fetch data asynchronously
            fetchFinderData();
            return;
        }

        addMenuBorder();

        if (finderUuids.isEmpty()) {
            // Show "no one found" message
            addStaticItem(22, getWarningIcon(plugin.getMessageManager().getMessage("gui.finders.empty.name", false),plugin.getMessageManager().getMessageList("gui.finders.empty.lore", false)));
            return;
        }

        addPagedButtons(finderUuids.size());
        // Calculate pagination
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, finderUuids.size());
        
        this.hasNextPage = endIndex < finderUuids.size();

        // Display player heads
        for (int i = startIndex; i < endIndex; i++) {
            UUID playerUuid = finderUuids.get(i);
            int displayIndex = i - startIndex;
            
            ItemStack playerHead = createPlayerHeadItem(playerUuid);
            addPagedItem(displayIndex, playerHead, e -> {
                // Optional: Could add click action like teleporting to player or showing stats
            });
        }

        // Update index for pagination buttons
        this.index = endIndex - 1;
    }

    /**
     * Fetches the list of players who found this treasure asynchronously.
     * Once complete, refreshes the menu on the main thread.
     */
    private void fetchFinderData() {
        plugin.getDataRepository().getPlayersWhoFound(treasureId).thenAccept(uuids -> {
            this.finderUuids = uuids != null ? uuids : new ArrayList<>();
            this.isLoading = false;
            
            // Refresh menu on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (inventory != null && playerMenuUtility.getOpenInventory().getTopInventory().equals(inventory)) {
                    refresh();
                }
            });
        }).exceptionally(ex -> {
            plugin.getLogger().warning("Failed to fetch finder data for treasure " + treasureId + ": " + ex.getMessage());
            this.finderUuids = new ArrayList<>();
            this.isLoading = false;
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (inventory != null && playerMenuUtility.getOpenInventory().getTopInventory().equals(inventory)) {
                    refresh();
                }
            });
            return null;
        });
    }

    /**
     * Creates a player head item with the player's name and basic info.
     * Uses OfflinePlayer to avoid blocking calls for online status.
     */
    private ItemStack createPlayerHeadItem(UUID playerUuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
        String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : playerUuid.toString();

        // Create player head
        ItemStack head = XMaterial.PLAYER_HEAD.parseItem();

        // Build with lore
        ItemBuilder builder = new ItemBuilder(head)
                .setSkullOwner(playerUuid)
                .setDisplayName(plugin.getMessageManager().getMessage(
                "gui.finders.entry.name", false,
                "%player%", playerName));
        
        List<String> lore = plugin.getMessageManager().getMessageList(
                "gui.finders.entry.lore", false,
                "%player%", playerName);
        
        for (String line : lore) {
            builder.addLoreLine(line);
        }

        return builder.build();
    }
}
