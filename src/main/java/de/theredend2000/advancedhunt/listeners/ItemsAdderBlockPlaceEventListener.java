package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.SoundManager;
import de.theredend2000.advancedhunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedhunt.util.enums.Permission;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent;
import dev.lone.itemsadder.api.Events.FurniturePlacedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ItemsAdderBlockPlaceEventListener implements Listener {


    public ItemsAdderBlockPlaceEventListener(){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onCustomBlockPlace(FurniturePlacedEvent event) {
        Player player = event.getPlayer();
        if (!Main.getInstance().getPlacePlayers().contains(player)) {
            return;
        }

        EggManager eggManager = Main.getInstance().getEggManager();
        SoundManager soundManager = Main.getInstance().getSoundManager();

        if(Main.getInstance().getPermissionManager().checkPermission(player, Permission.PlaceTreasure)){
            String collection = eggManager.getEggCollectionFromPlayerData(player.getUniqueId());
            eggManager.saveEgg(player, event.getFurniture().getEntity().getLocation(), collection);
            player.playSound(player.getLocation(), soundManager.playEggPlaceSound(), soundManager.getSoundVolume(), 1);
        }else
            player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.PlaceTreasure.toString()));
    }

    @EventHandler
    public void onCustomBlockPlace(CustomBlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!Main.getInstance().getPlacePlayers().contains(player)) {
            return;
        }

        EggManager eggManager = Main.getInstance().getEggManager();
        SoundManager soundManager = Main.getInstance().getSoundManager();

        if(Main.getInstance().getPermissionManager().checkPermission(player, Permission.PlaceTreasure)){
            String collection = eggManager.getEggCollectionFromPlayerData(player.getUniqueId());
            eggManager.saveEgg(player, event.getBlock().getLocation(), collection);
            player.playSound(player.getLocation(), soundManager.playEggPlaceSound(), soundManager.getSoundVolume(), 1);
        }else
            player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.PlaceTreasure.toString()));
    }
}