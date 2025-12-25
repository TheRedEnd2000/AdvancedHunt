package de.theredend2000.advancedHunt.menu.minigame;

import de.theredend2000.advancedHunt.Main;
import de.theredend2000.advancedHunt.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.function.Consumer;

public abstract class MinigameMenu extends Menu {

    protected final Consumer<Boolean> onFinish;
    protected boolean finished = false;

    public MinigameMenu(Player player, Main plugin, Consumer<Boolean> onFinish) {
        super(player, plugin);
        this.onFinish = onFinish;
    }

    protected void finish(boolean success) {
        if (finished) return;
        finished = true;
        playerMenuUtility.closeInventory();
        if (onFinish != null) {
            onFinish.accept(success);
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (!finished) {
            // If closed manually/abruptly, consider it a failure or just cancel
            // Depending on game design, we might want to fail.
            // For now, let's assume closing without finishing is a fail/cancel.
            if (onFinish != null) {
                onFinish.accept(false);
            }
        }
    }
    
    @Override
    public void handleMenu(InventoryClickEvent e) {
        // Default implementation, can be overridden
    }
}
