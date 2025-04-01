package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.SoundManager;
import de.theredend2000.advancedhunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedhunt.util.enums.Permission;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent;
import dev.lone.itemsadder.api.Events.FurniturePlacedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceEventListener implements Listener {

    private boolean itemsAdderEnabled;

    public BlockPlaceEventListener(){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
        this.itemsAdderEnabled = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
    }

    @EventHandler
    public void onPlaceEggEvent(BlockPlaceEvent event){
        // Skip if ItemsAdder is enabled as its event will handle custom blocks
        if (itemsAdderEnabled && event.getBlock().getType() == Material.AIR) {
            return;
        }

        Player player = event.getPlayer();
        if (!Main.getInstance().getPlacePlayers().contains(player)) {
            return;
        }

        EggManager eggManager = Main.getInstance().getEggManager();
        SoundManager soundManager = Main.getInstance().getSoundManager();

        if(Main.getInstance().getPermissionManager().checkPermission(player, Permission.PlaceTreasure)){
            String collection = eggManager.getEggCollectionFromPlayerData(player.getUniqueId());
            eggManager.saveEgg(player, event.getBlockPlaced().getLocation(), collection);
            player.playSound(player.getLocation(), soundManager.playEggPlaceSound(), soundManager.getSoundVolume(), 1);
        }else
            player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.PlaceTreasure.toString()));
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