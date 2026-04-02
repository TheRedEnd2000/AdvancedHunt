package de.theredend2000.advancedhunt.menu.collection;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.PagedMenu;
import de.theredend2000.advancedhunt.menu.common.SkullInfo;
import de.theredend2000.advancedhunt.menu.reward.RewardsMenu;
import de.theredend2000.advancedhunt.menu.treasure.TreasureActionMenu;
import de.theredend2000.advancedhunt.menu.treasure.WhoFoundMenu;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.model.CollectionRewardHolder;
import de.theredend2000.advancedhunt.model.TreasureCore;
import de.theredend2000.advancedhunt.model.TreasureRewardHolder;
import de.theredend2000.advancedhunt.platform.PlatformAccess;
import de.theredend2000.advancedhunt.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CollectionListMenu extends PagedMenu {

    private final UUID collectionId;

    public CollectionListMenu(Player playerMenuUtility, UUID collectionId, Main plugin) {
        super(playerMenuUtility, plugin);
        this.collectionId = collectionId;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.collection_content.title", false);
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

        addPagedButtons(treasures.size());

        // Calculate pagination
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, treasures.size());

        this.hasNextPage = endIndex < treasures.size();

        for (int i = startIndex; i < endIndex; i++) {
            TreasureCore treasureCore = treasures.get(i);
            int displayIndex = i - startIndex;

            int slot = getSlotForPagedIndex(displayIndex);
            ItemStack item = createTreasureItem(treasureCore, i, null, null);
            addButton(slot, item, e -> handleTreasureClick(e, treasureCore));

            UUID treasureId = treasureCore.getId();
            CompletableFuture<Integer> foundCountFuture = plugin.getDataRepository().getFoundPlayerCount(treasureId);

            boolean isHead = HeadHelper.isHeadMaterialName(treasureCore.getMaterial()) || HeadHelper.isPlayerHead(item);
            CompletableFuture<SkullInfo> skullInfoFuture = isHead
                    ? plugin.getTreasureManager().getFullTreasureAsync(treasureId).thenApply(fullTreasure -> {
                        if (fullTreasure == null) {
                            return null;
                        }
                        String texture = HeadHelper.getTextureFromNbt(fullTreasure.getNbtData());
                        if (texture != null) {
                            return new SkullInfo(texture, null);
                        }
                        String profileName = HeadHelper.getProfileNameFromNbt(fullTreasure.getNbtData());
                        if (profileName != null) {
                            return new SkullInfo(null, profileName);
                        }
                        return null;
                    })
                    : CompletableFuture.completedFuture(null);

            int finalI = i;
            foundCountFuture
                    .thenCombine(skullInfoFuture, TreasureItemData::new)
                    .thenAccept(data -> Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!isViewingThisMenu()) {
                            return;
                        }
                        ItemStack updated = createTreasureItem(treasureCore, finalI, data.foundCount(), data.skullInfo());
                        updateSlot(slot, updated);
                    }));
        }

        // Update index for pagination buttons
        this.index = endIndex - 1;
    }

    private boolean isViewingThisMenu() {
        if (playerMenuUtility == null) return false;
        if (!playerMenuUtility.isOnline()) return false;
        if (playerMenuUtility.getOpenInventory() == null) return false;
        if (playerMenuUtility.getOpenInventory().getTopInventory() == null) return false;
        return playerMenuUtility.getOpenInventory().getTopInventory().getHolder() == this;
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
                Bukkit.getScheduler().runTask(plugin, () -> {
                    ItemStack leaderboardButton = new ItemBuilder(XMaterial.GOLDEN_HELMET)
                            .setDisplayName(plugin.getMessageManager().getMessage("gui.leaderboard.view_button.name", false))
                            .setLore(plugin.getMessageManager().getMessageList("gui.leaderboard.view_button.lore", false))
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build();

                    addButton(8, leaderboardButton, e -> {
                        new LeaderboardMenu((Player) e.getWhoClicked(), plugin, collection, this).open();
                    });
                });
            }
        });
    }

    private ItemStack createTreasureItem(TreasureCore treasureCore, int index, Integer playerFoundSize, SkullInfo skullInfo) {
        // For menu display, use the lightweight TreasureCore fields.
        // ItemsAdder needs blockState (namespaced ID) to render correct icon.
        ItemStack item = null;
        if ("ITEMS_ADDER".equalsIgnoreCase(treasureCore.getMaterial())) {
            item = ItemsAdderAdapter.getCustomItem(treasureCore.getBlockState());
        }
        if (item != null && MaterialUtils.isAir(item.getType())) {
            item = null;
        }
        if (item == null) {
            item = XMaterialHelper.getItemStack(treasureCore.getMaterial());
            if (item == null || MaterialUtils.isAir(item.getType())) item = new ItemStack(XMaterial.CHEST.get());
        }

        if (HeadHelper.isHeadMaterialName(treasureCore.getMaterial()) && item != null) {
            item = PlatformAccess.get().ensurePlayerHeadItem(item);
        }

        ItemBuilder builder = new ItemBuilder(item);

        if (HeadHelper.isPlayerHead(item) && skullInfo != null) {
            if (skullInfo.texture() != null) {
                builder.setSkullTexture(skullInfo.texture());
            } else if (skullInfo.ownerName() != null) {
                builder.setSkullOwner(skullInfo.ownerName());
            }
        }

        builder.setDisplayName(plugin.getMessageManager().getMessage("gui.collection_content.treasure_item.name", false,
            "%number%", String.valueOf(index + 1)));

        String foundCount = playerFoundSize == null ? "..." : String.valueOf(playerFoundSize);
        List<String> lore = plugin.getMessageManager().getMessageList(
                "gui.collection_content.treasure_item.lore",
                false,
                "%count%", foundCount,
                "%x%", String.valueOf(treasureCore.getLocation().getBlockX()),
                "%y%", String.valueOf(treasureCore.getLocation().getBlockY()),
                "%z%", String.valueOf(treasureCore.getLocation().getBlockZ()),
                "%world%", treasureCore.getLocation().getWorld().getName()
        );
        
        if (playerMenuUtility.hasPermission("advancedhunt.admin.teleport")) {
            lore.add(plugin.getMessageManager().getMessage("gui.collection_content.treasure_item.lore_teleport", false));
        }
        if (playerMenuUtility.hasPermission("advancedhunt.admin.view_finders")) {
            lore.add(plugin.getMessageManager().getMessage("gui.collection_content.treasure_item.lore_who_found", false));
        }
        if (playerMenuUtility.hasPermission("advancedhunt.admin.rewards")) {
            lore.add(plugin.getMessageManager().getMessage("gui.collection_content.treasure_item.lore_reward", false));
        }

        if (playerMenuUtility.hasPermission("advancedhunt.admin.teleport")
                || playerMenuUtility.hasPermission("advancedhunt.admin.view_finders")
                || playerMenuUtility.hasPermission("advancedhunt.admin.rewards")) {
            lore.add(plugin.getMessageManager().getMessage("gui.collection_content.treasure_item.lore_action_menu_bedrock", false));
        }

        builder.setLore(lore);

        return builder.build();
    }

    private static final class TreasureItemData {
        private final int foundCount;
        private final SkullInfo skullInfo;

        private TreasureItemData(int foundCount, SkullInfo skullInfo) {
            this.foundCount = foundCount;
            this.skullInfo = skullInfo;
        }

        private int foundCount() {
            return foundCount;
        }

        private SkullInfo skullInfo() {
            return skullInfo;
        }
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
        if (clickType == ClickType.LEFT) {
            if (!p.hasPermission("advancedhunt.admin.teleport")) {
                p.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                return;
            }
            p.closeInventory();
            teleportToTreasure(p, treasureCore);
            return;
        }

        if(clickType == ClickType.SHIFT_LEFT){
            if (!p.hasPermission("advancedhunt.admin.rewards")) {
                p.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                return;
            }
            plugin.getTreasureManager().getFullTreasureAsync(treasureCore.getId()).thenAccept(treasure -> {
                if (treasure == null) {
                    return;
                }
                Collection collectionContext = plugin.getCollectionManager().getCollectionById(collectionId).orElse(null);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    RewardsMenu menu = (RewardsMenu) new RewardsMenu(p, plugin, new TreasureRewardHolder(plugin, treasure))
                            .setPreviousMenu(this);

                    if (collectionContext != null) {
                        menu.setAlternateContext(new CollectionRewardHolder(plugin, collectionContext));
                    }

                    menu.open();
                });
            });
            return;
        }

        // Other click types (Bedrock fallback): Open an action menu - need full treasure
        plugin.getTreasureManager().getFullTreasureAsync(treasureCore.getId()).thenAccept(fullTreasure -> {
            if (fullTreasure == null) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> new TreasureActionMenu(p, plugin, fullTreasure, this).open());
        });
    }

    /**
     * Safely teleports a player to a treasure location.
     * Includes chunk loading, safe spawn position finding, and error handling.
     */
    private void teleportToTreasure(Player player, TreasureCore treasureCore) {
        Location location = treasureCore.getLocation();
        
        if (location == null || location.getWorld() == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("feedback.teleport.invalid_location"));
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
                player.sendMessage(plugin.getMessageManager().getMessage("feedback.teleport.adjusted"));
            } else {
                player.sendMessage(plugin.getMessageManager().getMessage("feedback.teleport.unsafe"));
                return;
            }
        }

        // Perform teleport
        player.teleport(safeLoc);
        player.sendMessage(plugin.getMessageManager().getMessage("feedback.teleport.success"));
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
