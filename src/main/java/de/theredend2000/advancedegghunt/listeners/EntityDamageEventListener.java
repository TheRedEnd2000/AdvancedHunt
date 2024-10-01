package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

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

}
