package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.SoundManager;
import de.theredend2000.advancedhunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedhunt.util.enums.Permission;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BlockPlaceEventListener implements Listener {

    private boolean itemsAdderEnabled;

    public BlockPlaceEventListener(){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
        this.itemsAdderEnabled = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
    }

    @EventHandler
    public void onPlaceEggEvent(BlockPlaceEvent event) {
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

        if(Main.getInstance().getPermissionManager().checkPermission(player, Permission.PlaceTreasure)) {
            String collection = eggManager.getEggCollectionFromPlayerData(player.getUniqueId());
            event.setCancelled(true);
            eggManager.saveEgg(player, event.getBlock().getLocation(), collection);
            player.playSound(player.getLocation(), soundManager.playEggPlaceSound(), soundManager.getSoundVolume(), 1);

            // Get the placed block with all its properties
            Block placedBlock = event.getBlockPlaced();
            ItemStack itemInHand = event.getItemInHand();
            ItemMeta itemMeta = itemInHand.getItemMeta();

            // Use the new method that preserves all block properties
            /*Main.getInstance().getBlockChangingManager().sendBlockChangePacket(
                    placedBlock.getLocation(),
                    player,
                    placedBlock,
                    itemMeta
            );*/ //Take out for release
        } else {
            player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.PERMISSION_ERROR)
                    .replaceAll("%PERMISSION%", Permission.PlaceTreasure.toString()));
        }
    }
}