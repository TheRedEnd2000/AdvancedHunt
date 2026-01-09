package de.theredend2000.advancedhunt.menu.collection;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.PagedMenu;
import de.theredend2000.advancedhunt.model.PlayerData;
import de.theredend2000.advancedhunt.model.TreasureCore;
import de.theredend2000.advancedhunt.util.HeadHelper;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemsAdderAdapter;
import de.theredend2000.advancedhunt.util.XMaterialHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class ProgressMenu extends PagedMenu {

    private final UUID collectionId;
    private final String collectionName;
    private boolean isLoading = true;
    private List<TreasureCore> treasures;
    private PlayerData playerData;

    public ProgressMenu(Player playerMenuUtility, UUID collectionId, String collectionName, Main plugin) {
        super(playerMenuUtility, plugin);
        this.collectionId = collectionId;
        this.collectionName = collectionName;
    }

    @Override
    public String getMenuName() {
        if (isLoading || treasures == null || treasures.isEmpty()) {
            return ChatColor.translateAlternateColorCodes('&',
                plugin.getMessageManager().getMessage("gui.progress.title", false,
                    "%collection%", collectionName,
                    "%progress%", "0",
                    "%total%", "0",
                    "%percentage%", "0"));
        }

        int foundCount = (int) treasures.stream()
            .filter(treasure -> playerData.getFoundTreasures().contains(treasure.getId()))
            .count();
        
        int total = treasures.size();
        double percentage = total > 0 ? (foundCount * 100.0 / total) : 0;

        return ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage("gui.progress.title", false,
                "%collection%", collectionName,
                "%progress%", String.valueOf(foundCount),
                "%total%", String.valueOf(total),
                "%percentage%", String.format(java.util.Locale.US, "%.1f", percentage)));
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
        if (isLoading) {
            addMenuBorder();
            ItemStack loadingItem = new ItemBuilder(XMaterial.HOPPER)
                    .setDisplayName(plugin.getMessageManager().getMessage("gui.progress.loading", false))
                    .build();
            addStaticItem(22, loadingItem);
            fetchData();
            return;
        }

        addMenuBorder();

        // Add stats button at slot 8 (top right corner)
        addStatsButton();

        if (treasures == null || treasures.isEmpty()) {
            addStaticItem(22, getWarningIcon(plugin.getMessageManager().getMessage("gui.progress.no_treasures.name", false),plugin.getMessageManager().getMessageList("gui.progress.no_treasures.lore", false)));
            return;
        }

        addPagedButtons(treasures.size());

        // WICHTIG: Berechne Start und End Index für diese Seite
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, treasures.size());

        // WICHTIG: Setze hasNextPage Flag
        this.hasNextPage = endIndex < treasures.size();

        // Zeige nur Items für die aktuelle Seite
        for (int i = startIndex; i < endIndex; i++) {
            TreasureCore treasureCore = treasures.get(i);
            boolean isFound = playerData.getFoundTreasures().contains(treasureCore.getId());

            // Page index ist relativ zur aktuellen Seite
            int pageIndex = i - startIndex;

            int slot = getSlotForPagedIndex(pageIndex);
            ItemStack item = createTreasureItem(treasureCore, i, isFound, null);
            addButton(slot, item, e -> handleTreasureClick(e, treasureCore, isFound));

            if (isFound && HeadHelper.isPlayerHead(item)) {
                UUID treasureId = treasureCore.getId();
                int finalI = i;
                plugin.getTreasureManager().getFullTreasureAsync(treasureId)
                        .thenApply(fullTreasure -> {
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
                        .thenAccept(skullInfo -> {
                            if (skullInfo == null) {
                                return;
                            }
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (!isViewingThisMenu()) {
                                    return;
                                }
                                ItemStack updated = createTreasureItem(treasureCore, finalI, true, skullInfo);
                                updateSlot(slot, updated);
                            });
                        });
            }
        }

        // Update index for pagination
        this.index = endIndex - 1;
    }

    private boolean isViewingThisMenu() {
        if (playerMenuUtility == null) return false;
        if (!playerMenuUtility.isOnline()) return false;
        if (playerMenuUtility.getOpenInventory() == null) return false;
        if (playerMenuUtility.getOpenInventory().getTopInventory() == null) return false;
        return playerMenuUtility.getOpenInventory().getTopInventory().getHolder() == this;
    }

    private void fetchData() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            this.playerData = plugin.getPlayerManager().getPlayerData(playerMenuUtility.getUniqueId());
            this.treasures = plugin.getTreasureManager().getTreasureCoresInCollection(collectionId);
            this.isLoading = false;
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (playerMenuUtility.getOpenInventory().getTopInventory().getHolder() == this) {
                    open();
                }
            });
        });
    }

    private void addStatsButton() {
        if (treasures == null || treasures.isEmpty()) {
            return;
        }

        int foundCount = (int) treasures.stream()
            .filter(treasure -> playerData.getFoundTreasures().contains(treasure.getId()))
            .count();
        
        int total = treasures.size();
        double percentage = total > 0 ? (foundCount * 100.0 / total) : 0;

        ItemStack statsItem = new ItemBuilder(XMaterial.BOOK)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.progress.stats.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.progress.stats.lore", false,
                "%found%", String.valueOf(foundCount),
                "%total%", String.valueOf(total),
                "%percentage%", String.format(java.util.Locale.US, "%.1f", percentage),
                "%collection%", collectionName))
            .build();
        
        addStaticItem(8, statsItem);
    }

    private ItemStack createTreasureItem(TreasureCore treasureCore, int index, boolean isFound, SkullInfo skullInfo) {
        ItemBuilder builder;
        String statusColor;

        if (isFound) {
            statusColor = ChatColor.GREEN.toString();
            ItemStack item = null;
            if ("ITEMS_ADDER".equalsIgnoreCase(treasureCore.getMaterial())) {
                item = ItemsAdderAdapter.getCustomItem(treasureCore.getBlockState());
            }

            if (item == null) {
                Material material = Material.matchMaterial(treasureCore.getMaterial());
                if (material == null) material = XMaterial.CHEST.get();

                XMaterial xMaterial = XMaterial.matchXMaterial(material);
                item = XMaterialHelper.getItemStack(xMaterial);
            }

            if (item != null) {
                builder = new ItemBuilder(item);
            } else {
                builder = new ItemBuilder(XMaterial.CHEST);
            }

            if (HeadHelper.isPlayerHead(item) && skullInfo != null) {
                if (skullInfo.texture() != null) {
                    builder.setSkullTexture(skullInfo.texture());
                } else if (skullInfo.ownerName() != null) {
                    builder.setSkullOwner(skullInfo.ownerName());
                }
            }
        } else {
            statusColor = ChatColor.RED.toString();
            builder = new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE);
        }
        
        String status = isFound 
            ? plugin.getMessageManager().getMessage("gui.progress.treasure.status_found", false)
            : plugin.getMessageManager().getMessage("gui.progress.treasure.status_not_found", false);

        builder.setDisplayName(statusColor + ChatColor.translateAlternateColorCodes('&',
            plugin.getMessageManager().getMessage("gui.progress.treasure.name", false,
                "%number%", String.valueOf(index + 1),
                "%status%", status)));

        List<String> lore = plugin.getMessageManager().getMessageList(
            isFound ? "gui.progress.treasure.lore_found" : "gui.progress.treasure.lore_not_found",
            false,
            "%x%", String.valueOf(treasureCore.getLocation().getBlockX()),
            "%y%", String.valueOf(treasureCore.getLocation().getBlockY()),
            "%z%", String.valueOf(treasureCore.getLocation().getBlockZ()),
            "%world%", treasureCore.getLocation().getWorld().getName()
        );
        
        builder.setLore(lore);

        return builder.build();
    }

    private static final class SkullInfo {
        private final String texture;
        private final String ownerName;

        private SkullInfo(String texture, String ownerName) {
            this.texture = texture;
            this.ownerName = ownerName;
        }

        private String texture() {
            return texture;
        }

        private String ownerName() {
            return ownerName;
        }
    }

    private void handleTreasureClick(InventoryClickEvent e, TreasureCore treasureCore, boolean isFound) {
        // Optional: Add click functionality here, such as teleportation or showing more details
        // For now, clicks do nothing - this is just a view-only progress menu
    }
}
