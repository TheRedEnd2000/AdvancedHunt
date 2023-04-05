package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.versions.VersionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerInteractEventListener implements Listener {

    public PlayerInteractEventListener(){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onInteractEvent(PlayerInteractEvent event){
        Player player = event.getPlayer();
        if(event.getHand() == EquipmentSlot.HAND){
            return;
        }
        if(event.getClickedBlock() != null) {
            if (VersionManager.getEggManager().containsEgg(event.getClickedBlock()) && !Main.getInstance().getPlaceEggsPlayers().contains(player)) {
                String id = VersionManager.getEggManager().getEggID(event.getClickedBlock());
                if (!VersionManager.getEggManager().hasFound(player, id)) {
                    VersionManager.getEggManager().saveFoundEggs(player, event.getClickedBlock(), id);
                    Location loc = new Location(event.getClickedBlock().getWorld(),event.getClickedBlock().getLocation().getX(),event.getClickedBlock().getLocation().getY(),event.getClickedBlock().getLocation().getZ());
                    VersionManager.getExtraManager().spawnFireworkRocket(loc.add(0.5,0.5,0.5));
                    if(VersionManager.getEggManager().checkFoundAll(player)){
                        player.playSound(player.getLocation(),VersionManager.getSoundManager().playAllEggsFound(), 1, 1);
                        player.sendMessage("All Eggs Found");
                    }else
                        player.playSound(player.getLocation(),VersionManager.getSoundManager().playEggFoundSound(), VersionManager.getSoundManager().getSoundVolume(),1);
                } else {
                    player.sendMessage(Main.getInstance().getMessage("EggAlreadyFoundMessage"));
                    player.playSound(player.getLocation(),VersionManager.getSoundManager().playEggAlreadyFoundSound(),VersionManager.getSoundManager().getSoundVolume(),1);
                }
            }
        }
    }
}
