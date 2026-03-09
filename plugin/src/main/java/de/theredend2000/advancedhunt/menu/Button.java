package de.theredend2000.advancedhunt.menu;

import com.cryptomorin.xseries.XSound;
import de.theredend2000.advancedhunt.managers.SoundManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Button {

    private static final Map<UUID, Long> clickCooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 150; // milliseconds

    private ItemStack icon;
    private Consumer<InventoryClickEvent> action;
    private Sound clickSound;

    public Button(ItemStack icon) {
        this(icon, null);
    }

    public Button(ItemStack icon, Consumer<InventoryClickEvent> action) {
        this.icon = icon;
        this.action = action;
        this.clickSound = XSound.UI_BUTTON_CLICK.get(); // Default sound
    }

    public void onClick(InventoryClickEvent event) {
        if (action != null) {
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                UUID playerId = player.getUniqueId();
                long now = System.currentTimeMillis();
                Long lastClick = clickCooldowns.get(playerId);
                if (lastClick != null && (now - lastClick) < COOLDOWN_MS) {
                    return; // Still on cooldown, ignore click
                }
                clickCooldowns.put(playerId, now);
                if(SoundManager.isEnabledStatic())
                    player.playSound(player.getLocation(), clickSound, 1f, 1f);
            }
            action.accept(event);
        }
    }

    public ItemStack getIcon() {
        return icon;
    }

    public void setIcon(ItemStack icon) {
        this.icon = icon;
    }

    public void setAction(Consumer<InventoryClickEvent> action) {
        this.action = action;
    }

    public void setClickSound(Sound clickSound) {
        this.clickSound = clickSound;
    }

    /**
     * Removes the cooldown entry for the given player.
     * Call on player quit to prevent the static map from growing unboundedly.
     */
    public static void removeClickCooldown(UUID playerId) {
        clickCooldowns.remove(playerId);
    }
}
