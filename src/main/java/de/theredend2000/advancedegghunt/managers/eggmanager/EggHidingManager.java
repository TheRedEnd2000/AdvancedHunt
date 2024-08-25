package de.theredend2000.advancedegghunt.managers.eggmanager;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import de.theredend2000.advancedegghunt.Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;

public class EggHidingManager {

    private Main plugin;
    private HashMap<Player,Location> blocks;

    public EggHidingManager(){
        plugin = Main.getInstance();
        blocks = new HashMap<>();

        //hide();
    }

    public void hideEggForPlayer(Player player, Location location) {
        blocks.put(player,location);
    }

    private void hide(){
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : blocks.keySet()) {
                    Location location = blocks.get(player);
                    PacketContainer packet = plugin.getProtocolManager().createPacket(PacketType.Play.Server.BLOCK_CHANGE);

                    BlockPosition position = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    WrappedBlockData blockData = WrappedBlockData.createData(Material.AIR);

                    packet.getBlockPositionModifier().write(0, position);
                    packet.getBlockData().write(0, blockData);

                    try {
                        plugin.getProtocolManager().sendServerPacket(player, packet);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

}
