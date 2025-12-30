package de.theredend2000.advancedhunt.menu.collection;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.PagedMenu;
import de.theredend2000.advancedhunt.model.PlayerData;
import de.theredend2000.advancedhunt.model.Treasure;
import de.theredend2000.advancedhunt.model.TreasureCore;
import de.theredend2000.advancedhunt.util.HeadHelper;
import de.theredend2000.advancedhunt.util.ItemBuilder;
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
            ItemStack loadingItem = new ItemBuilder(Material.HOPPER)
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
            ItemStack emptyItem = new ItemBuilder(Material.BARRIER)
                    .setDisplayName(plugin.getMessageManager().getMessage("gui.progress.no_treasures.name", false))
                    .setLore(plugin.getMessageManager().getMessageList("gui.progress.no_treasures.lore", false))
                    .build();
            addStaticItem(22, emptyItem);
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

            ItemStack item = createTreasureItem(treasureCore, i, isFound);
            addPagedItem(pageIndex, item, e -> handleTreasureClick(e, treasureCore, isFound));
        }

        // Update index for pagination
        this.index = endIndex - 1;
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

        ItemStack statsItem = new ItemBuilder(Material.BOOK)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.progress.stats.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.progress.stats.lore", false,
                "%found%", String.valueOf(foundCount),
                "%total%", String.valueOf(total),
                "%percentage%", String.format(java.util.Locale.US, "%.1f", percentage),
                "%collection%", collectionName))
            .build();
        
        addStaticItem(8, statsItem);
    }

    private ItemStack createTreasureItem(TreasureCore treasureCore, int index, boolean isFound) {
        ItemBuilder builder;
        String statusColor;

        if (isFound) {
            statusColor = ChatColor.GREEN.toString();
            Treasure treasure = plugin.getTreasureManager().getFullTreasure(treasureCore.getId());
            if (treasure != null) {
                XMaterial xMaterial = XMaterial.matchXMaterial(treasure.getMaterial()).orElse(XMaterial.CHEST);
                ItemStack item = XMaterialHelper.getItemStack(xMaterial);

                if (item != null) {
                    builder = new ItemBuilder(item);
                } else {
                    builder = new ItemBuilder(Material.CHEST);
                }

                if (xMaterial == XMaterial.PLAYER_HEAD || xMaterial == XMaterial.PLAYER_WALL_HEAD) {
                    String texture = HeadHelper.getTextureFromNbt(treasure.getNbtData());
                    if (texture != null) {
                        builder.setSkullTexture(texture);
                    } else {
                        // Try profile name if no texture is available
                        String profileName = HeadHelper.getProfileNameFromNbt(treasure.getNbtData());
                        if (profileName != null) {
                            builder.setSkullOwner(profileName);
                        }
                    }
                }
            } else {
                builder = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE);
            }
        } else {
            statusColor = ChatColor.RED.toString();
            builder = new ItemBuilder(Material.RED_STAINED_GLASS_PANE);
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

    private void handleTreasureClick(InventoryClickEvent e, TreasureCore treasureCore, boolean isFound) {
        // Optional: Add click functionality here, such as teleportation or showing more details
        // For now, clicks do nothing - this is just a view-only progress menu
    }
}
