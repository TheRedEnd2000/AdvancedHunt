package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ConfigLocationUtil;
import de.theredend2000.advancedegghunt.versions.VersionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BlockPlaceEventListener implements Listener {

    public BlockPlaceEventListener(){
        Bukkit.getPluginManager().registerEvents(this,Main.getInstance());
    }


    @EventHandler
    public void onPlaceEggEvent(BlockPlaceEvent event){
        Player player = event.getPlayer();
        String permission = Main.getInstance().getConfig().getString("Permissions.PlaceEggPermission");
        if(Main.getInstance().getPlaceEggsPlayers().contains(player)) {
            if(player.hasPermission(permission)){
                VersionManager.getEggManager().saveEgg(player, event.getBlockPlaced().getLocation());
                player.playSound(player.getLocation(), VersionManager.getSoundManager().playEggPlaceSound(), VersionManager.getSoundManager().getSoundVolume(), 1);
            }else
                player.sendMessage(Main.getInstance().getMessage("NoPermissionMessage").replaceAll("%PERMISSION%", permission));
        }
    }

}
