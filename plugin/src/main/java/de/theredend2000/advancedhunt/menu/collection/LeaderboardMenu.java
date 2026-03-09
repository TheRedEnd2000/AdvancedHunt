package de.theredend2000.advancedhunt.menu.collection;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.LeaderboardManager;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.menu.PagedMenu;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a paginated leaderboard showing top players for a specific collection.
 * Shows player rankings, scores, and highlights the current viewer's position.
 * Optimized with caching to minimize database queries and name resolution overhead.
 */
public class LeaderboardMenu extends PagedMenu {

    private final Collection collection;
    private List<LeaderboardManager.LeaderboardEntry> entries;
    private boolean isLoading = true;
    private int playerRank = -1;
    private int playerScore = 0;
    private int totalTreasures = 0;

    public LeaderboardMenu(Player player, Main plugin, Collection collection, Menu previousMenu) {
        super(player, plugin);
        this.collection = collection;
        this.previousMenu = previousMenu;
        this.entries = new ArrayList<>();
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.leaderboard.title", false,
                "%collection%", collection.getName());
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
        int displaylimit = plugin.getConfig().getInt("leaderboard.display-limit");
        int refresh = plugin.getConfig().getInt("leaderboard.cache-refresh-interval");
        double minutes = Math.round((refresh / 60.0) * 10.0) / 10.0;

        addStaticItem(8,new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.leaderboard.info.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.leaderboard.info.lore", false
                        , "%display_limit%", String.valueOf(displaylimit), "%refresh_interval%",String.valueOf(refresh), "%minutes%",String.valueOf(minutes)))
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTY0MzlkMmUzMDZiMjI1NTE2YWE5YTZkMDA3YTdlNzVlZGQyZDUwMTVkMTEzYjQyZjQ0YmU2MmE1MTdlNTc0ZiJ9fX0=")
                .build()
        );
        addMenuBorder();
        if (isLoading) {
            // Show loading indicator
            ItemStack loadingItem = new ItemBuilder(XMaterial.HOPPER)
                    .setDisplayName(plugin.getMessageManager().getMessage("gui.leaderboard.loading.name", false))
                    .setLore(plugin.getMessageManager().getMessageList("gui.leaderboard.loading.lore", false))
                    .build();
            addStaticItem(22, loadingItem);
            
            // Fetch data asynchronously
            fetchLeaderboardData();
            return;
        }
        
        // Add "Your Rank" indicator at slot 4
        addYourRankIndicator();

        if (entries.isEmpty()) {
            // Show "no data" message
            addStaticItem(22, getWarningIcon(plugin.getMessageManager().getMessage("gui.leaderboard.empty.name", false), plugin.getMessageManager().getMessageList("gui.leaderboard.empty.lore", false)));
            return;
        }

        addPagedButtons(entries.size());

        // Calculate pagination
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, entries.size());
        
        this.hasNextPage = endIndex < entries.size();

        // Display leaderboard entries
        for (int i = startIndex; i < endIndex; i++) {
            LeaderboardManager.LeaderboardEntry entry = entries.get(i);
            int displayIndex = i - startIndex;
            
            boolean isCurrentPlayer = entry.getPlayerId().equals(playerMenuUtility.getUniqueId());
            ItemStack playerItem = createLeaderboardItem(entry, isCurrentPlayer);
            
            addPagedItem(displayIndex, playerItem, e -> {
                // Optional: Could add click action like viewing player stats
            });
        }

        // Update index for pagination buttons
        this.index = endIndex - 1;
    }

    /**
     * Fetches leaderboard data from the cache asynchronously.
     * Also calculates the player's rank and score.
     */
    private void fetchLeaderboardData() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Get cached leaderboard entries
            List<LeaderboardManager.LeaderboardEntry> cachedEntries = 
                    plugin.getLeaderboardManager().getEntries(collection.getName());
            
            this.entries = cachedEntries != null ? cachedEntries : new ArrayList<>();
            
            // Get player's data
            plugin.getDataRepository().loadPlayerData(playerMenuUtility.getUniqueId()).thenAccept(playerData -> {
                if (playerData != null) {
                    // Use lightweight TreasureCore indexes for counting; avoid loading full treasures (NBT/rewards).
                    this.totalTreasures = plugin.getTreasureManager().getTreasureCoresInCollection(collection.getId()).size();
                    this.playerScore = plugin.getTreasureManager()
                        .filterFoundByCollection(playerData.getFoundTreasures(), collection.getId())
                        .size();
                    
                    // Find player's rank
                    calculatePlayerRank();

                    // Refresh menu on the main thread
                } else {
                    this.totalTreasures = 0;
                    this.playerScore = 0;
                    this.playerRank = -1;
                }
                this.isLoading = false;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (inventory != null && playerMenuUtility.getOpenInventory().getTopInventory().equals(inventory)) {
                        refresh();
                    }
                });
            });
        });
    }

    /**
     * Calculates the player's rank by comparing their score against the leaderboard.
     */
    private void calculatePlayerRank() {
        if (playerScore == 0) {
            playerRank = -1;
            return;
        }
        
        // Check if player is in the cached leaderboard
        for (LeaderboardManager.LeaderboardEntry entry : entries) {
            if (entry.getPlayerId().equals(playerMenuUtility.getUniqueId())) {
                playerRank = entry.getRank();
                return;
            }
        }
        
        // Player not in top N, calculate rank by counting entries with higher scores
        int rank = 1;
        for (LeaderboardManager.LeaderboardEntry entry : entries) {
            if (entry.getScore() > playerScore) {
                rank++;
            }
        }
        playerRank = rank;
    }

    /**
     * Adds the "Your Rank" indicator item at slot 4.
     */
    private void addYourRankIndicator() {
        ItemStack indicatorItem;
        
        if (playerScore == 0 || playerRank == -1) {
            // Player has not found any treasures
            indicatorItem = new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE)
                    .setDisplayName(plugin.getMessageManager().getMessage("gui.leaderboard.not_ranked.name", false))
                    .setLore(plugin.getMessageManager().getMessageList("gui.leaderboard.not_ranked.lore", false))
                    .build();
        } else {
            // Player is ranked
            double percentage = totalTreasures > 0 ? (playerScore * 100.0 / totalTreasures) : 0;
            
            indicatorItem = new ItemBuilder(XMaterial.LIME_STAINED_GLASS_PANE)
                    .setDisplayName(plugin.getMessageManager().getMessage("gui.leaderboard.your_rank.name", false))
                    .setLore(plugin.getMessageManager().getMessageList("gui.leaderboard.your_rank.lore", false,
                            "%rank%", String.valueOf(playerRank),
                            "%score%", String.valueOf(playerScore),
                            "%total%", String.valueOf(totalTreasures),
                            "%percentage%", String.format("%.1f", percentage)))
                    .build();
        }
        
        addStaticItem(4, indicatorItem);
    }

    /**
     * Creates a player head item for a leaderboard entry.
     * Highlights the current player with an enchantment glow effect.
     */
    private ItemStack createLeaderboardItem(LeaderboardManager.LeaderboardEntry entry, boolean isCurrentPlayer) {
        String playerName = entry.getPlayerName();
        
        // Create player head
        ItemStack head = XMaterial.PLAYER_HEAD.parseItem();
        
        // Choose message key based on whether this is the current player
        String nameKey = isCurrentPlayer ? "gui.leaderboard.player_entry_highlighted.name" : "gui.leaderboard.player_entry.name";
        String loreKey = isCurrentPlayer ? "gui.leaderboard.player_entry_highlighted.lore" : "gui.leaderboard.player_entry.lore";
        
        ItemBuilder builder = new ItemBuilder(head)
                .setSkullOwner(entry.getPlayerId())
                .setDisplayName(plugin.getMessageManager().getMessage(nameKey, false,
                        "%rank%", String.valueOf(entry.getRank()),
                        "%player%", playerName));
        
        List<String> lore = plugin.getMessageManager().getMessageList(loreKey, false,
                "%rank%", String.valueOf(entry.getRank()),
                "%player%", playerName,
                "%score%", String.valueOf(entry.getScore()));
        
        for (String line : lore) {
            builder.addLoreLine(line);
        }
        
        // Add glow effect for current player
        if (isCurrentPlayer) {
            builder.addEnchant(Enchantment.UNBREAKING, 1);
            builder.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        return builder.build();
    }

    // Do not override getMaxItemsPerPage() — use the parent class's per-page limit so
    // pagination works correctly across multiple pages.
}
