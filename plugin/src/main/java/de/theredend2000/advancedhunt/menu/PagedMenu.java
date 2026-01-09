package de.theredend2000.advancedhunt.menu;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public abstract class PagedMenu extends Menu {

    protected int page = 0;
    protected int maxItemsPerPage = 28;
    protected int index = 0;
    protected boolean hasNextPage = false;

    public PagedMenu(Player playerMenuUtility, Main plugin) {
        super(playerMenuUtility, plugin);
    }

    public void addMenuBorder() {
        for (int i = 0; i < 10; i++) {
            if (inventory.getItem(i) == null) {
                addStaticItem(i, super.FILLER_GLASS);
            }
        }

        addStaticItem(17, super.FILLER_GLASS);
        addStaticItem(18, super.FILLER_GLASS);
        addStaticItem(26, super.FILLER_GLASS);
        addStaticItem(27, super.FILLER_GLASS);
        addStaticItem(35, super.FILLER_GLASS);
        addStaticItem(36, super.FILLER_GLASS);

        for (int i = 44; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                addStaticItem(i, super.FILLER_GLASS);
            }
        }
    }

    public void addPagedButtons(int size){
        addButton(48, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19")
                .setDisplayName(plugin.getMessageManager().getMessage("gui.common.previous_page")+getPageIndicator(size))
                .build(), (e) -> {
            if (page == 0) {
                e.getWhoClicked().sendMessage(plugin.getMessageManager().getMessage("feedback.gui.first_page"));
            } else {
                page = page - 1;
                open();
            }
        });

        addButton(50, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ==")
                .setDisplayName(plugin.getMessageManager().getMessage("gui.common.next_page")+getPageIndicator(size))
                .build(), (e) -> {
            if (hasNextPage) {
                page = page + 1;
                open();
            } else {
                e.getWhoClicked().sendMessage(plugin.getMessageManager().getMessage("feedback.gui.last_page"));
            }
        });
    }

    public int getMaxItemsPerPage() {
        return maxItemsPerPage;
    }

    /**
     * Calculates the actual inventory slot for a paged item based on its display index.
     * PagedMenu uses slots 10-16, 19-25, 28-34, 37-43 (4 rows × 7 columns).
     * 
     * @param displayIndex The index of the item in the current page (0-based)
     * @return The actual slot number in the inventory
     */
    protected int getSlotForPagedIndex(int displayIndex) {
        int row = displayIndex / 7;
        int col = displayIndex % 7;
        return 10 + (row * 9) + col;
    }

    public void addPagedItem(int index, ItemStack item, Consumer<InventoryClickEvent> action) {
        int slot = getSlotForPagedIndex(index);
        addButton(slot, item, action);
    }

    public String getPageIndicator(int size){
        String title = "";
        if (getTotalPages(size) > 1) {
            title += " " + plugin.getMessageManager().getMessage("gui.common.page_indicator", false,
                    "%page%", String.valueOf(page + 1),
                    "%total%", String.valueOf(getTotalPages(size)));
        }
        return title;
    }

    /**
     * Returns the total number of pages needed to display all rewards.
     */
    public int getTotalPages(int size) {
        if (size <= maxItemsPerPage) return 1;
        return (int) Math.ceil((double) size / maxItemsPerPage);
    }
}
