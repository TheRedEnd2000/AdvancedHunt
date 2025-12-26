package de.theredend2000.advancedhunt.menu;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.SoundManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class Button {

    private ItemStack icon;
    private Consumer<InventoryClickEvent> action;
    private Sound clickSound;

    public Button(ItemStack icon) {
        this(icon, null);
    }

    public Button(ItemStack icon, Consumer<InventoryClickEvent> action) {
        this.icon = icon;
        this.action = action;
        this.clickSound = Sound.UI_BUTTON_CLICK; // Default sound
    }

    public void onClick(InventoryClickEvent event) {
        if (action != null) {
            if (event.getWhoClicked() instanceof Player) {
                if(SoundManager.isEnabledStatic())
                    ((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), clickSound, 1f, 1f);
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
}
