package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.SoundManager;
import de.theredend2000.advancedhunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedhunt.util.VersionComparator;
import de.theredend2000.advancedhunt.util.enums.Permission;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

import java.util.List;


public class BlockBreakEventListener implements Listener {

    private MessageManager messageManager;

    public BlockBreakEventListener(){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
        messageManager = Main.getInstance().getMessageManager();
    }

    @EventHandler
    public void onDestroyEgg(BlockBreakEvent event){
        Player player = event.getPlayer();
        Block block = event.getBlock();

        EggManager eggManager = Main.getInstance().getEggManager();
        SoundManager soundManager = Main.getInstance().getSoundManager();

        if (!eggManager.containsEgg(block)) {
            return;
        }
        String collection = eggManager.getEggCollection(block);
        if(Main.getInstance().getPlacePlayers().contains(player)) {
            if(Main.getInstance().getPermissionManager().checkPermission(player, Permission.BreakTreasure)){
                eggManager.removeEgg(player, block, collection);
                player.playSound(player.getLocation(), soundManager.playEggBreakSound(), soundManager.getSoundVolume(), 1);
            }else {
                player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.BreakTreasure.toString()));
                event.setCancelled(true);
            }
        }else {
            if(Main.getInstance().getPermissionManager().checkPermission(player, Permission.BreakTreasure))
                player.sendMessage(messageManager.getMessage(MessageKey.ONLY_IN_PLACEMODE));
            event.setCancelled(true);
        }
        eggManager.updateMaxEggs(collection);
    }

    @EventHandler
    public void onBlockFromToEvent(BlockFromToEvent event) {
        Block toblock = event.getToBlock();
        EggManager eggManager = Main.getInstance().getEggManager();

        if(eggManager.containsEgg(toblock))
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerBucketFillEvent(PlayerBucketEmptyEvent event) {
        var version = Bukkit.getBukkitVersion().split("-", 2);

        if (VersionComparator.isLessThan(version[0], "1.14.4")) return;
        Block block = event.getBlock();
        EggManager eggManager = Main.getInstance().getEggManager();

        if(eggManager.containsEgg(block))
            event.setCancelled(true);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event){
        EggManager eggManager = Main.getInstance().getEggManager();
        event.blockList().removeIf(eggManager::containsEgg);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        EggManager eggManager = Main.getInstance().getEggManager();
        List<Block> blocks = event.getBlocks();
        if (blocks.stream().anyMatch(eggManager::containsEgg)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        EggManager eggManager = Main.getInstance().getEggManager();
        List<Block> blocks = event.getBlocks();
        if (blocks.stream().anyMatch(eggManager::containsEgg)) {
            event.setCancelled(true);
        }
    }
}
