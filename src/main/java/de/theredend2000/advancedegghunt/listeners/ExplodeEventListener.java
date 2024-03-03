package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public class ExplodeEventListener implements Listener {

    public ExplodeEventListener(){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event){
        EggManager eggManager = Main.getInstance().getEggManager();
        event.blockList().removeIf(eggManager::containsEgg);
    }
}
