package de.theredend2000.advancedhunt.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemHelper;
import de.theredend2000.advancedhunt.util.LogHelper;
import de.theredend2000.advancedhunt.util.VersionComparator;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockChangingManager extends PacketAdapter implements Listener {

    private Main plugin;
    private LogHelper logHelper;
    private boolean sending = false;
    private final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    private final Map<UUID, Map<String, GhostBlockData>> ghostBlocks = new HashMap<>();

    public static class GhostBlockData {
        public final String worldName;
        public final int x, y, z;
        public final Material material;
        public final BlockData blockData;  // Store complete BlockData instead of just Material

        public GhostBlockData(Location location, Material material, BlockData blockData) {
            this.worldName = location.getWorld().getName();
            this.x = location.getBlockX();
            this.y = location.getBlockY();
            this.z = location.getBlockZ();
            this.material = material;
            this.blockData = blockData;
        }

        public Location getLocation() {
            return new Location(Bukkit.getWorld(worldName), x, y, z);
        }

        public Material getMaterial() {
            return material;
        }

        public BlockData getBlockData() {
            return blockData;
        }
    }

    public BlockChangingManager() {
        super(Main.getInstance(), ListenerPriority.NORMAL,
                PacketType.Play.Server.BLOCK_CHANGE,
                PacketType.Play.Server.MULTI_BLOCK_CHANGE);
        this.plugin = Main.getInstance();
        logHelper = plugin.getLogHelper();
        registerListener();
    }

    public void registerListener() {
        protocolManager.addPacketListener(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        logHelper.sendLogMessage("BlockChangingManager was registered!");
    }

    private String getBlockKey(Location location) {
        return location.getWorld().getName() + "," +
                location.getBlockX() + "," +
                location.getBlockY() + "," +
                location.getBlockZ();
    }

    private String getBlockKey(BlockPosition position, String worldName) {
        return worldName + "," +
                position.getX() + "," +
                position.getY() + "," +
                position.getZ();
    }

    public void registerGhostBlock(UUID playerUUID, Location location, Material material, BlockData blockData) {
        String blockKey = getBlockKey(location);
        GhostBlockData data = new GhostBlockData(location, material, blockData);

        ghostBlocks.computeIfAbsent(playerUUID, k -> new HashMap<>())
                .put(blockKey, data);

        logHelper.sendLogMessage("Ghost Block registered for " + playerUUID +
                " at " + blockKey + " as " + material.name() + " with data: " + blockData.getAsString());
    }

    public void removeGhostBlock(UUID playerUUID, Location location) {
        String blockKey = getBlockKey(location);
        if (ghostBlocks.containsKey(playerUUID)) {
            ghostBlocks.get(playerUUID).remove(blockKey);
        }
    }

    public void clearGhostBlocks(UUID playerUUID) {
        ghostBlocks.remove(playerUUID);
    }

    public void resendAllGhostBlocks(Player player) {
        UUID uuid = player.getUniqueId();
        if (!ghostBlocks.containsKey(uuid) || ghostBlocks.get(uuid).isEmpty()) {
            logHelper.sendLogMessage("No ghost blocks for player " + player.getName());
            return;
        }

        logHelper.sendLogMessage("Sending all ghost blocks for " + player.getName() +
                " (" + ghostBlocks.get(uuid).size() + " blocks)");

        if (protocolManager == null) {
            logHelper.sendLogMessage("ProtocolManager is null! Can't send packets.");
            return;
        }

        Map<String, GhostBlockData> blocks = ghostBlocks.get(uuid);

        for (Map.Entry<String, GhostBlockData> entry : blocks.entrySet()) {
            String key = entry.getKey();
            GhostBlockData data = entry.getValue();

            logHelper.sendLogMessage("Processing block: " + key);

            World world = Bukkit.getWorld(data.worldName);
            if (world == null) {
                logHelper.sendLogMessage("World " + data.worldName + " doesn't exist! Skipping block.");
                continue;
            }

            Location location = new Location(world, data.x, data.y, data.z);
            BlockData blockData = data.getBlockData();

            if (blockData == null) {
                logHelper.sendLogMessage("BlockData is null for block at " + location);
                continue;
            }

            if (!location.getChunk().isLoaded()) {
                logHelper.sendLogMessage("Chunk for block at " + location + " is not loaded. Skipping.");
                continue;
            }
            sending = true;
            try {
                PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);

                BlockPosition blockPosition = new BlockPosition(
                        location.getBlockX(), location.getBlockY(), location.getBlockZ());

                packet.getBlockPositionModifier().write(0, blockPosition);

                // Use the full BlockData instead of just Material
                WrappedBlockData wrappedData = WrappedBlockData.createData(blockData);
                packet.getBlockData().write(0, wrappedData);

                protocolManager.sendServerPacket(player, packet);
                sending = false;
            } catch (Exception e) {
                logHelper.sendLogMessage("Error sending ghost block at " + location + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        String worldName = event.getPlayer().getWorld().getName();

        if (!ghostBlocks.containsKey(playerUUID)) {
            return;
        }

        Map<String, GhostBlockData> playerGhostBlocks = ghostBlocks.get(playerUUID);

        if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
            PacketContainer packet = event.getPacket();
            BlockPosition position = packet.getBlockPositionModifier().read(0);
            String blockKey = getBlockKey(position, worldName);

            if (playerGhostBlocks.containsKey(blockKey) && !sending) {
                event.setCancelled(true);
                logHelper.sendLogMessage("Block-Update for Ghost-Block blocked: " + blockKey);
            }
        }
        else if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            //Check Block stuff
            event.setCancelled(true);
        }
    }

    /**
     * Creates a ghost block by copying all properties of the original block.
     *
     * @param location Location where the ghost block should appear
     * @param player Player who should see the ghost block
     * @param material Material of the block
     * @param blockData The complete BlockData including rotation, orientation, etc.
     */
    public void sendBlockChangePacket(Location location, Player player, Material material, BlockData blockData) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);

        BlockPosition blockPosition = new BlockPosition(
                location.getBlockX(), location.getBlockY(), location.getBlockZ());

        packet.getBlockPositionModifier().write(0, blockPosition);
        packet.getBlockData().write(0, WrappedBlockData.createData(blockData));

        try {
            protocolManager.sendServerPacket(player, packet);
            registerGhostBlock(player.getUniqueId(), location, material, blockData);
            logHelper.sendLogMessage("§aCreated Ghostblock " + material.name() + " with data: " + blockData.getAsString());
        } catch (Exception e) {
            logHelper.sendLogMessage("§cError while creating a Ghost Block!");
            e.printStackTrace();
        }
    }

    /**
     * Creates a ghost block by copying all properties from the placed block.
     * This method supports copying properties from ItemMeta if available.
     *
     * @param location Location where the ghost block should appear
     * @param player Player who should see the ghost block
     * @param block The block that was placed
     * @param itemMeta ItemMeta from the item used to place the block (important for skulls, etc.)
     */
    public void sendBlockChangePacket(Location location, Player player, Block block, ItemMeta itemMeta) {
        Material material = block.getType();
        BlockData blockData = block.getBlockData();

        /*
            TODO texture not working
         */

        if (itemMeta instanceof SkullMeta && material == Material.PLAYER_HEAD) {
            ItemStack item = new ItemStack(material);
            item.setItemMeta(itemMeta);
            String texture = Main.getTexture(ItemHelper.getSkullTexture(item));
            logHelper.sendLogMessage("Added texture value to head: "+texture);

            ItemStack head = new ItemBuilder(material).setSkullOwner(texture).build();
            BlockDataMeta bdMeta = (BlockDataMeta) head.getItemMeta();
            blockData = bdMeta.getBlockData(material);
            logHelper.sendLogMessage("Using SkullMeta from ItemMeta");
        }

        if (itemMeta instanceof BlockDataMeta) {
            BlockDataMeta blockDataMeta = (BlockDataMeta) itemMeta;
            if (blockDataMeta.hasBlockData()) {
                blockData = blockDataMeta.getBlockData(material);
                logHelper.sendLogMessage("Using BlockData from ItemMeta");
            }
        }

        sendBlockChangePacket(location, player, material, blockData);
    }

    // -------------------- EVENT LISTENERS --------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            resendAllGhostBlocks(event.getPlayer());
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            resendAllGhostBlocks(event.getPlayer());
        }, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            resendAllGhostBlocks(event.getPlayer());
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            resendAllGhostBlocks(event.getPlayer());
        }, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) {
            return;
        }

        for (Player player : event.getWorld().getPlayers()) {
            if (!ghostBlocks.containsKey(player.getUniqueId())) {
                continue;
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Map<String, GhostBlockData> playerBlocks = ghostBlocks.get(player.getUniqueId());

                for (GhostBlockData data : playerBlocks.values()) {
                    Location loc = data.getLocation();
                    if (loc.getChunk().equals(event.getChunk())) {
                        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
                        BlockPosition blockPosition = new BlockPosition(
                                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

                        packet.getBlockPositionModifier().write(0, blockPosition);
                        packet.getBlockData().write(0, WrappedBlockData.createData(data.getBlockData()));

                        try {
                            protocolManager.sendServerPacket(player, packet);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, 10L);
        }
    }
}