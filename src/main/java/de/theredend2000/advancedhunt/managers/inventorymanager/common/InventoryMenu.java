package de.theredend2000.advancedhunt.managers.inventorymanager.common;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.PlayerMenuUtility;
import de.theredend2000.advancedhunt.util.messages.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;


public abstract class InventoryMenu implements IInventoryMenu {
    protected MenuManager menuMessageManager;
    protected PlayerMenuUtility playerMenuUtility;
    private Inventory inventory;
    protected final ItemStack FILLER_GLASS;
    private final String inventoryName;
    protected final short slots;
    protected ItemStack[] inventoryContent;

    public InventoryMenu(PlayerMenuUtility playerMenuUtility, String inventoryName, short slots) {
        this(playerMenuUtility, inventoryName, slots, XMaterial.GRAY_STAINED_GLASS_PANE);
    }

    public InventoryMenu(PlayerMenuUtility playerMenuUtility, String inventoryName, short slots, XMaterial fillerMaterial) {
        menuMessageManager = Main.getInstance().getMenuManager();
        this.playerMenuUtility = playerMenuUtility;
        this.slots = slots % 9 == 0? slots : (short) (slots - (slots % 9));
        this.inventoryContent = new ItemStack[this.slots];
        this.inventoryName = inventoryName;

        this.FILLER_GLASS = new ItemBuilder(fillerMaterial)
                .setDisplayName(" ")
                .build();
    }

    protected void addMenuBorder()
    {
        int lastRowStartIndex = 9 * ((slots / 9) - 1);
        for (int column = 1; column < 9; column++) {
            inventoryContent[column] = FILLER_GLASS;
            inventoryContent[column + lastRowStartIndex] = FILLER_GLASS;
        }

        for (int row = 0; row < slots / 9 ; row++) {
            inventoryContent[row * 9] = FILLER_GLASS;
            inventoryContent[(row * 9) + 8] = FILLER_GLASS;
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        ItemStack itemStack = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        if (itemStack instanceof InventoryButton) {
            ((InventoryButton)itemStack).onClick(event.getClick(), player);
        }
        player.sendMessage("clicked2");
        if (event.getClickedInventory() == getInventory()) {
            player.sendMessage("clicked");
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
                player.sendMessage("cancel");
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), player::updateInventory, 4L);
            }
        }
    }

    @NotNull
    @Override
    public final Inventory getInventory() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, slots, inventoryName);
            inventory.setContents(inventoryContent);
        }

        return inventory;
    }
}
