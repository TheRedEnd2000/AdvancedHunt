package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.versions.VersionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;


public class BlockBreakEventListener implements Listener {

    public BlockBreakEventListener(){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onDestroyEgg(BlockBreakEvent event){
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if(VersionManager.getEggManager().containsEgg(event.getBlock())){
            String permission = Main.getInstance().getConfig().getString("Permissions.BreakEggPermission");
            if(Main.getInstance().getPlaceEggsPlayers().contains(player)) {
                if(player.hasPermission(permission)){
                    VersionManager.getEggManager().removeEgg(player, block);
                    player.playSound(player.getLocation(), VersionManager.getSoundManager().playEggBreakSound(), VersionManager.getSoundManager().getSoundVolume(), 1);
                }else {
                    player.sendMessage(Main.getInstance().getMessage("NoPermissionMessage").replaceAll("%PERMISSION%", permission));
                    event.setCancelled(true);
                }
            }else {
                if(player.hasPermission(permission))
                    player.sendMessage(Main.getInstance().getMessage("OnlyInPlaceMode"));
                event.setCancelled(true);
            }
            VersionManager.getEggManager().updateMaxEggs();
        }
    }

}
