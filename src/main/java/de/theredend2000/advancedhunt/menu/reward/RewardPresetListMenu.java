package de.theredend2000.advancedhunt.menu.reward;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.PagedMenu;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.model.RewardPreset;
import de.theredend2000.advancedhunt.model.RewardPresetType;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lists reward presets for either TREASURE or COLLECTION.
 * If onSelect is provided, clicking a preset selects it.
 * Otherwise, clicking a preset opens the preset editor.
 */
public class RewardPresetListMenu extends PagedMenu {

    private final RewardPresetType type;
    private final Consumer<RewardPreset> onSelect;
    private final Collection collectionContext;
    private boolean fromCollection = false;

    public RewardPresetListMenu(Player player, Main plugin, RewardPresetType type, Consumer<RewardPreset> onSelect) {
        super(player, plugin);
        this.type = type;
        this.onSelect = onSelect;
        this.collectionContext = null;
        this.maxItemsPerPage = 28;
    }

    public RewardPresetListMenu(Player player, Main plugin, RewardPresetType type, Consumer<RewardPreset> onSelect,
                                Collection collectionContext, boolean fromCollection) {
        super(player, plugin);
        this.type = type;
        this.onSelect = onSelect;
        this.collectionContext = collectionContext;
        this.maxItemsPerPage = 28;
        this.fromCollection = fromCollection;
    }

    public RewardPresetListMenu(Player player, Main plugin, RewardPresetType type, Consumer<RewardPreset> onSelect,
                                Collection collectionContext) {
        super(player, plugin);
        this.type = type;
        this.onSelect = onSelect;
        this.collectionContext = collectionContext;
        this.maxItemsPerPage = 28;
    }

    @Override
    public String getMenuName() {
        String key = type == RewardPresetType.COLLECTION ? "gui.presets.collection.title" : "gui.presets.treasure.title";
        return plugin.getMessageManager().getMessage(key, false);
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        // handled by buttons
    }

    @Override
    public void setMenuItems() {
        index = 0;
        addMenuBorder();

        List<RewardPreset> presets = plugin.getRewardPresetManager().getPresets(type);
        presets.sort(Comparator.comparing(RewardPreset::getName, String.CASE_INSENSITIVE_ORDER));

        if (presets.isEmpty()) {
            addStaticItem(22, getWarningIcon(plugin.getMessageManager().getMessage("gui.presets.none.name", false),plugin.getMessageManager().getMessageList("gui.presets.none.lore", false)));
        } else {
            addPagedButtons(presets.size());
            int startIndex = page * maxItemsPerPage;
            int endIndex = Math.min(startIndex + maxItemsPerPage, presets.size());
            this.hasNextPage = endIndex < presets.size();

            for (int i = startIndex; i < endIndex; i++) {
                RewardPreset preset = presets.get(i);

                ItemStack icon = new ItemBuilder(XMaterial.WRITTEN_BOOK.get())
                        .setDisplayName(plugin.getMessageManager().getMessage("gui.presets.preset.name", false,
                                "%name%", preset.getName()))
                        .setLore(plugin.getMessageManager().getMessageList("gui.presets.preset.lore", false,
                                "%count%", String.valueOf(preset.getRewards().size()),
                                "%action%", onSelect != null
                                        ? plugin.getMessageManager().getMessage("gui.presets.preset.action."+(fromCollection ? "select": "load"), false)
                                        : plugin.getMessageManager().getMessage("gui.presets.preset.action.edit", false)))
                        .build();

                addPagedItem(index++, icon, click -> {
                    if (onSelect != null) {
                        // Select on left-click, open editor on right-click
                        if (click.isRightClick()) {
                            if (!playerMenuUtility.hasPermission("advancedhunt.admin")) {
                                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                                return;
                            }
                            new RewardPresetActionsMenu(playerMenuUtility, plugin, preset, collectionContext)
                                    .setPreviousMenu(this)
                                    .open();
                            return;
                        }
                        onSelect.accept(preset);
                        return;
                    }

                    new RewardPresetActionsMenu(playerMenuUtility, plugin, preset, collectionContext)
                            .setPreviousMenu(this)
                            .open();
                });
            }
        }
    }
}
