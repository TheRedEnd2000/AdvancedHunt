package de.theredend2000.advancedhunt.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtList;
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

import java.lang.reflect.Field;
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

        // Process BlockDataMeta for any block that supports it
        if (itemMeta instanceof BlockDataMeta) {
            BlockDataMeta blockDataMeta = (BlockDataMeta) itemMeta;
            if (blockDataMeta.hasBlockData()) {
                blockData = blockDataMeta.getBlockData(material);
                logHelper.sendLogMessage("Using BlockData from ItemMeta");
            }
        }

        // First send the normal block packet to set up the block with its orientation
        PacketContainer blockPacket = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
        BlockPosition blockPosition = new BlockPosition(
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
        blockPacket.getBlockPositionModifier().write(0, blockPosition);
        blockPacket.getBlockData().write(0, WrappedBlockData.createData(blockData));

        try {
            protocolManager.sendServerPacket(player, blockPacket);

            // Handle player head textures separately with a tile entity packet
            if (itemMeta instanceof SkullMeta && material == Material.PLAYER_HEAD) {
                ItemStack item = new ItemStack(material);
                item.setItemMeta(itemMeta);
                String texture = Main.getTexture(ItemHelper.getSkullTexture(item));
                logHelper.sendLogMessage("Added texture value to head: " + texture);

                // Create and send the tile entity packet to set the texture
                PacketContainer tileEntityPacket = protocolManager.createPacket(PacketType.Play.Server.TILE_ENTITY_DATA);

                // Set the block position
                tileEntityPacket.getBlockPositionModifier().write(0, blockPosition);

                // Check if the packet uses TileEntityType enum in newer versions
                try {
                    // For newer versions - use reflection to get the proper enum
                    Class<?> tileEntityTypeClass = Class.forName("net.minecraft.world.level.block.entity.TileEntityTypes");
                    // The actual class path might vary by MC version - adjust if needed

                    Object skullType = null;
                    for (Field field : tileEntityTypeClass.getDeclaredFields()) {
                        if (field.getName().equals("SKULL") || field.getName().equals("HEAD")) {
                            field.setAccessible(true);
                            skullType = field.get(null);
                            break;
                        }
                    }

                    if (skullType != null) {
                        tileEntityPacket.getModifier().write(1, skullType);
                    } else {
                        // Fallback - try to use the action integer but check array length first
                        if (tileEntityPacket.getIntegers().size() > 0) {
                            tileEntityPacket.getIntegers().write(0, 5); // Updated skull type value
                        } else {
                            logHelper.sendLogMessage("§cWarning: Could not set skull type in packet");
                        }
                    }
                } catch (Exception e) {
                    // Alternative approach - get the correct indexes by inspecting the packet
                    logHelper.sendLogMessage("§cError setting tile entity type: " + e.getMessage());

                    // Debug the packet structure to find correct fields
                    logHelper.sendLogMessage("§ePacket structure: " + tileEntityPacket.getStructures());

                    // Try using a different modifier if integers array is empty
                    try {
                        // Some versions use byte for the action type instead of integer
                        tileEntityPacket.getBytes().write(0, (byte)5);
                    } catch (Exception e2) {
                        logHelper.sendLogMessage("§cCould not set tile entity type: " + e2.getMessage());
                    }
                }

                // Create NBT data for the skull texture
                NbtCompound nbt = NbtFactory.ofCompound("");
                nbt.put("id", "minecraft:skull"); // Add the id field which might be required

                // Set the skull type to player
                nbt.put("SkullType", (byte) 3);

                NbtCompound skullOwner = NbtFactory.ofCompound("SkullOwner");
                String uuidString = UUID.randomUUID().toString();
                skullOwner.put("Id", uuidString);

                // Create texture properties
                NbtCompound properties = NbtFactory.ofCompound("Properties");
                NbtList textures = NbtFactory.ofList("textures");
                NbtCompound textureValue = NbtFactory.ofCompound("");
                textureValue.put("Value", texture);
                textures.add(textureValue);
                properties.put("textures", textures);
                skullOwner.put("Properties", properties);

                // Add SkullOwner to main compound
                nbt.put("SkullOwner", skullOwner);

                // Write NBT data to packet
                tileEntityPacket.getNbtModifier().write(0, nbt);

                // Send the tile entity packet
                protocolManager.sendServerPacket(player, tileEntityPacket);
                logHelper.sendLogMessage("§aApplied skull texture to ghost block");
            }

            // Register the ghost block with its blockdata
            registerGhostBlock(player.getUniqueId(), location, material, blockData);
            logHelper.sendLogMessage("§aCreated Ghostblock " + material.name() + " with data: " + blockData.getAsString());
        } catch (Exception e) {
            logHelper.sendLogMessage("§cError while creating a Ghost Block!");
            e.printStackTrace();
        }
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