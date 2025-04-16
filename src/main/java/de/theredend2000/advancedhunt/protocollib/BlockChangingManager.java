package de.theredend2000.advancedhunt.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import de.theredend2000.advancedhunt.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockChangingManager extends PacketAdapter implements Listener {

    private Main plugin;
    private final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

    // Speichert Ghost-Block-Informationen für jeden Spieler
    private final Map<UUID, Map<String, GhostBlockData>> ghostBlocks = new HashMap<>();

    // Statische Klasse für Ghost-Block-Daten
    public static class GhostBlockData {
        public final String worldName;
        public final int x, y, z;
        public final Material material;

        public GhostBlockData(Location location, Material material) {
            this.worldName = location.getWorld().getName();
            this.x = location.getBlockX();
            this.y = location.getBlockY();
            this.z = location.getBlockZ();
            this.material = material;
        }

        public Location getLocation() {
            return new Location(Bukkit.getWorld(worldName), x, y, z);
        }

        public Material getMaterial() {
            return material;
        }
    }

    public BlockChangingManager() {
        super(Main.getInstance(), ListenerPriority.NORMAL,
                PacketType.Play.Server.BLOCK_CHANGE,
                PacketType.Play.Server.MULTI_BLOCK_CHANGE);
        this.plugin = Main.getInstance();
    }

    public void registerListener() {
        // ProtocolLib Listener registrieren
        protocolManager.addPacketListener(this);

        // Bukkit Event Listener registrieren
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Debug-Nachricht
        plugin.getLogger().info("BlockChangingManager wurde registriert!");
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

    public void registerGhostBlock(UUID playerUUID, Location location, Material material) {
        String blockKey = getBlockKey(location);
        GhostBlockData data = new GhostBlockData(location, material);

        ghostBlocks.computeIfAbsent(playerUUID, k -> new HashMap<>())
                .put(blockKey, data);

        plugin.getLogger().info("Ghost Block registriert für " + playerUUID +
                " bei " + blockKey + " als " + material.name());
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

    // Sendet alle Ghost-Blöcke für einen Spieler
    public void resendAllGhostBlocks(Player player) {
        UUID uuid = player.getUniqueId();
        if (!ghostBlocks.containsKey(uuid) || ghostBlocks.get(uuid).isEmpty()) {
            return;
        }

        plugin.getLogger().info("Sende alle Ghost-Blöcke für " + player.getName() +
                " (" + ghostBlocks.get(uuid).size() + " Blöcke)");

        Map<String, GhostBlockData> blocks = ghostBlocks.get(uuid);

        for (Map.Entry<String, GhostBlockData> entry : blocks.entrySet()) {
            GhostBlockData data = entry.getValue();

            // Prüfe, ob die Welt existiert
            if (Bukkit.getWorld(data.worldName) == null) {
                plugin.getLogger().warning("Welt " + data.worldName + " existiert nicht! Überspringe Block.");
                continue;
            }

            // Erstelle die Location neu ohne .getBlock() aufzurufen
            Location location = new Location(Bukkit.getWorld(data.worldName), data.x, data.y, data.z);
            Material material = data.material;
            if(material == null){
                plugin.getLogger().info("Material is null");
                continue;
            }
            plugin.getLogger().info("Material is "+material.name());

            // Prüfe, ob der Chunk geladen ist
            if (!location.getChunk().isLoaded()) {
                plugin.getLogger().info("Chunk für Block bei " + location + " ist nicht geladen. Überspringe.");
                continue;
            }

            // Paket erstellen und senden
            // DOES NOT SEND!
            try {
                PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);

                BlockPosition blockPosition = new BlockPosition(
                        location.getBlockX(), location.getBlockY(), location.getBlockZ());

                packet.getBlockPositionModifier().write(0, blockPosition);
                packet.getBlockData().write(0, WrappedBlockData.createData(material));
            } catch (Exception e) {
                plugin.getLogger().warning("Fehler beim Senden des Ghost-Blocks bei " + location);
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

            if (playerGhostBlocks.containsKey(blockKey)) {
                event.setCancelled(true);
                plugin.getLogger().info("Block-Update für Ghost Block blockiert: " + blockKey);
            }
        }
        else if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            // Multi-Block-Changes komplexer (versionsspezifisch)
            // Für jetzt blockieren wir einfach alle solchen Pakete für Spieler mit Ghost-Blocks
            // Dies könnte zu Problemen führen, wenn viele Blöcke geändert werden
            event.setCancelled(true);
        }
    }

    public void sendBlockChangePacket(Location location, Player player, Material material) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);

        BlockPosition blockPosition = new BlockPosition(
                location.getBlockX(), location.getBlockY(), location.getBlockZ());

        packet.getBlockPositionModifier().write(0, blockPosition);
        packet.getBlockData().write(0, WrappedBlockData.createData(material));

        try {
            protocolManager.sendServerPacket(player, packet);
            registerGhostBlock(player.getUniqueId(), location, material);
            player.sendMessage("§aGhost-Block als " + material.name() + " erstellt!");
        } catch (Exception e) {
            player.sendMessage("§cFehler beim Erstellen des Ghost-Blocks!");
            e.printStackTrace();
        }
    }

    // -------------------- EVENT LISTENERS --------------------

    // Wenn ein Spieler sich teleportiert
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Warte einen Tick, damit die Teleportation abgeschlossen ist
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            resendAllGhostBlocks(event.getPlayer());
        }, 1L);
    }

    // Wenn ein Spieler respawnt
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Warte einen Tick, damit der Respawn abgeschlossen ist
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            resendAllGhostBlocks(event.getPlayer());
        }, 20L);
    }

    // Wenn ein Spieler die Welt wechselt
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        // Warte einen Tick, damit der Weltwechsel abgeschlossen ist
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            resendAllGhostBlocks(event.getPlayer());
        }, 1L);
    }

    // Wenn ein Spieler den Server erneut betritt
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Warte einige Ticks, damit alle Chunks geladen sind
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            resendAllGhostBlocks(event.getPlayer());
        }, 10L);
    }

    // Wenn ein Chunk geladen wird
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        // Nur wenn Spieler in der Nähe sind
        if (event.isNewChunk()) {
            return;
        }

        // Für jeden Spieler in der Nähe
        for (Player player : event.getWorld().getPlayers()) {
            if (!ghostBlocks.containsKey(player.getUniqueId())) {
                continue;
            }

            // Warte einen Tick, damit der Chunk vollständig geladen ist
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Nur Ghost-Blöcke in diesem Chunk neu senden
                Map<String, GhostBlockData> playerBlocks = ghostBlocks.get(player.getUniqueId());

                for (GhostBlockData data : playerBlocks.values()) {
                    Location loc = data.getLocation();
                    if (loc.getChunk().equals(event.getChunk())) {
                        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
                        BlockPosition blockPosition = new BlockPosition(
                                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

                        packet.getBlockPositionModifier().write(0, blockPosition);
                        packet.getBlockData().write(0, WrappedBlockData.createData(data.getMaterial()));

                        try {
                            protocolManager.sendServerPacket(player, packet);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, 1L);
        }
    }
}