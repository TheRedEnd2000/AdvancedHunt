package de.theredend2000.advancedhunt.menu.place;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.PagedMenu;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Displays a paged list of players who have found a specific treasure.
 * Fetches data asynchronously to avoid blocking the main thread.
 */
public class ViewPlacePresetsMenu extends PagedMenu {

    public Main plugin;

    private final List<PlacePreset> PRESETS = List.of(
            new PlacePreset("easter", new ItemBuilder(Material.PLAYER_HEAD).setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWUzZjNmNTlhNDVlMDE4Nzc0NjQwOGU5NWZjNjYyN2EyNDRjY2IxNDJmYzFlM2E2OGQxYTk2OTIxZDE4MmYwOCJ9fX0=")),
            new PlacePreset("halloween",  new ItemBuilder(Material.CARVED_PUMPKIN)),
            new PlacePreset("christmas", new ItemBuilder(Material.PLAYER_HEAD).setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzk1ZjBjMGU3ZTg3NzE2YjIwYTBkNzMzOGI2NTRkNmVmODY2MThlMDA5MzUwZDgyZmZlYWE3M2M0OTdiNWI3NiJ9fX0="))
    );

    public ViewPlacePresetsMenu(Player player, Main plugin) {
        super(player, plugin);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Place presets";
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
        addMenuBorder();

        addPagedButtons(PRESETS.size());
        // Calculate pagination
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, PRESETS.size());
        
        this.hasNextPage = endIndex < PRESETS.size();

        // Display player heads
        for (int i = startIndex; i < endIndex; i++) {
            PlacePreset preset = PRESETS.get(i);
            int displayIndex = i - startIndex;
            
            ItemStack playerHead = preset.getItemStack();
            addPagedItem(displayIndex, playerHead, e -> {
                // Optional: Could add click action like teleporting to player or showing stats
            });
        }

        // Update index for pagination buttons
        this.index = endIndex - 1;
    }


    private final class PlacePreset {
        final String key;
        final ItemBuilder itemBuilder;

        PlacePreset(String key, ItemBuilder itemBuilder) {
            this.key = key;
            this.itemBuilder = itemBuilder;
        }

        String getName() {
            return plugin.getMessageManager().getMessage("gui.place.presets.list." + key + ".name", false);
        }

        String[] getDescription() {
            return plugin.getMessageManager().getMessageList("gui.place.presets.list." + key + ".lore", false).toArray(new String[0]);
        }
        ItemStack getItemStack(){
            return itemBuilder.setDisplayName(getName()).setLore(getDescription()).build();
        }
    }
}
