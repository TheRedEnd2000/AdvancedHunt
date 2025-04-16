package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.SoundManager;
import de.theredend2000.advancedhunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedhunt.protocollib.BlockChangingManager;
import de.theredend2000.advancedhunt.util.enums.Permission;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
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
            event.setCancelled(true);
            eggManager.saveEgg(player, event.getBlock().getLocation(), collection);
            player.playSound(player.getLocation(), soundManager.playEggPlaceSound(), soundManager.getSoundVolume(), 1);
            BlockChangingManager changingManager = new BlockChangingManager();
            changingManager.registerListener();
            Bukkit.getScheduler().runTaskLater(Main.getInstance(),()-> changingManager.sendBlockChangePacket(event.getBlockPlaced(),player),10L);
        }else
            player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.PlaceTreasure.toString()));
    }
}