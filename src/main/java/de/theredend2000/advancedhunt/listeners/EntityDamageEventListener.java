package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class EntityDamageEventListener implements Listener {

    private Main plugin;

    public EntityDamageEventListener(){
        plugin = Main.getInstance();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event){
        Entity damager = event.getDamager();

        if (damager.getType().equals(EntityType.FIREWORK)) {
            Firework firework = (Firework) damager;
            if (plugin.getExtraManager().getFireworkUUID().contains(firework.getUniqueId())) {
                event.setCancelled(true);
                plugin.getExtraManager().getFireworkUUID().remove(firework.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (Main.getInstance().getPlacePlayers().contains(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (Main.getInstance().getPlacePlayers().contains(player)) {
                event.setCancelled(true);
            }
        }
    }

}
