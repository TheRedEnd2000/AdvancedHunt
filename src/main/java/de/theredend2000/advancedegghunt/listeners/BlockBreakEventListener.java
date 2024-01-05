package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedegghunt.managers.soundmanager.SoundManager;
import org.bukkit.Bukkit;
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
        EggManager eggManager = Main.getInstance().getEggManager();
        SoundManager soundManager = Main.getInstance().getSoundManager();
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if(eggManager.containsEgg(event.getBlock())){
            String permission = Main.getInstance().getConfig().getString("Permissions.BreakEggPermission");
            if(Main.getInstance().getPlaceEggsPlayers().contains(player)) {
                if(player.hasPermission(permission)){
                    eggManager.removeEgg(player, block);
                    player.playSound(player.getLocation(), soundManager.playEggBreakSound(), soundManager.getSoundVolume(), 1);
                }else {
                    player.sendMessage(Main.getInstance().getMessage("NoPermissionMessage").replaceAll("%PERMISSION%", permission));
                    event.setCancelled(true);
                }
            }else {
                if(player.hasPermission(permission))
                    player.sendMessage(Main.getInstance().getMessage("OnlyInPlaceMode"));
                event.setCancelled(true);
            }
            eggManager.updateMaxEggs();
        }
    }

}
