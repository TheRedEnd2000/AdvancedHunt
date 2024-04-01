package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.eggmanager.EggManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

public class EntityChangeListener implements Listener {

    public EntityChangeListener(){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event){
        EggManager eggManager = Main.getInstance().getEggManager();
        if(eggManager.containsEgg(event.getBlock())) event.setCancelled(true);
    }
}
