package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.SoundManager;
import de.theredend2000.advancedhunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedhunt.util.enums.Permission;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceEventListener implements Listener {

    public BlockPlaceEventListener(){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }


    @EventHandler
    public void onPlaceEggEvent(BlockPlaceEvent event){
        Player player = event.getPlayer();
        if (!Main.getInstance().getPlacePlayers().contains(player)) {
            return;
        }

        EggManager eggManager = Main.getInstance().getEggManager();
        SoundManager soundManager = Main.getInstance().getSoundManager();

        if(Main.getInstance().getPermissionManager().checkPermission(player, Permission.PlaceEgg)){
            String collection = eggManager.getEggCollectionFromPlayerData(player.getUniqueId());
            eggManager.saveEgg(player, event.getBlockPlaced().getLocation(), collection);
            player.playSound(player.getLocation(), soundManager.playEggPlaceSound(), soundManager.getSoundVolume(), 1);
        }else
            player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.PlaceEgg.toString()));
    }
}
