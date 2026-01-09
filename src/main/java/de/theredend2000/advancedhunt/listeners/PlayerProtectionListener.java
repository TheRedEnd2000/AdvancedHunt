package de.theredend2000.advancedhunt.listeners;

import com.cryptomorin.xseries.XEntityType;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.FireworkManager;
import de.theredend2000.advancedhunt.managers.PlaceModeManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class PlayerProtectionListener implements Listener {

    private final Main plugin;
    private final FireworkManager fireworkManager;
    private final PlaceModeManager placeModeManager;

    public PlayerProtectionListener(Main plugin){
        this.plugin = plugin;
        this.fireworkManager = plugin.getFireworkManager();
        this.placeModeManager = plugin.getPlaceModeManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event){
        Entity damager = event.getDamager();

        if (damager.getType().equals(XEntityType.FIREWORK_ROCKET.get())) {
            Firework firework = (Firework) damager;
            if (fireworkManager.getFireworkUUIDs().contains(firework.getUniqueId())) {
                event.setCancelled(true);
                fireworkManager.getFireworkUUIDs().remove(firework.getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (placeModeManager.isInPlaceMode(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (placeModeManager.isInPlaceMode(player)) {
            event.setCancelled(true);
        }
    }

}
