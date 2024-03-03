package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedegghunt.managers.soundmanager.SoundManager;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceEventListener implements Listener {

    public BlockPlaceEventListener(){
        Bukkit.getPluginManager().registerEvents(this,Main.getInstance());
    }


    @EventHandler
    public void onPlaceEggEvent(BlockPlaceEvent event){
        EggManager eggManager = Main.getInstance().getEggManager();
        SoundManager soundManager = Main.getInstance().getSoundManager();
        Player player = event.getPlayer();
        String permission = Main.getInstance().getConfig().getString("Permissions.PlaceEggPermission");
        if(Main.getInstance().getPlaceEggsPlayers().contains(player)) {
            if(player.hasPermission(permission)){
                String section = eggManager.getEggSectionFromPlayerData(player.getUniqueId());
                eggManager.saveEgg(player, event.getBlockPlaced().getLocation(),section);
                player.playSound(player.getLocation(), soundManager.playEggPlaceSound(), soundManager.getSoundVolume(), 1);
            }else
                player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", permission));
        }
    }
}
