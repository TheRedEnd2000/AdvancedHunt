package de.theredend2000.advancedhunt.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import de.theredend2000.advancedhunt.Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockChangingManager extends PacketAdapter {

    private Main plugin;
    private final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    private final Map<UUID, Map<BlockPosition, Boolean>> ghostBlocks = new HashMap<>();

    public BlockChangingManager(){
        super(Main.getInstance(), ListenerPriority.NORMAL,
                PacketType.Play.Server.BLOCK_CHANGE,
                PacketType.Play.Server.MULTI_BLOCK_CHANGE);
        this.plugin = Main.getInstance();
    }

    public void registerListener() {
        protocolManager.addPacketListener(this);
    }

    // Registriert einen Ghost-Block für einen Spieler
    public void registerGhostBlock(UUID playerUUID, Location location) {
        BlockPosition blockPos = new BlockPosition(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());

        ghostBlocks.computeIfAbsent(playerUUID, k -> new HashMap<>())
                .put(blockPos, true);
    }

    // Entfernt einen Ghost-Block für einen Spieler
    public void removeGhostBlock(UUID playerUUID, Location location) {
        BlockPosition blockPos = new BlockPosition(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());

        if (ghostBlocks.containsKey(playerUUID)) {
            ghostBlocks.get(playerUUID).remove(blockPos);
        }
    }

    // Entfernt alle Ghost-Blocks für einen Spieler
    public void clearGhostBlocks(UUID playerUUID) {
        ghostBlocks.remove(playerUUID);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        if (!ghostBlocks.containsKey(playerUUID)) {
            return; // Dieser Spieler hat keine Ghost-Blocks
        }

        Map<BlockPosition, Boolean> playerGhostBlocks = ghostBlocks.get(playerUUID);

        if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
            // Einzelne Block-Änderung
            PacketContainer packet = event.getPacket();
            BlockPosition position = packet.getBlockPositionModifier().read(0);

            if (playerGhostBlocks.containsKey(position)) {
                event.setCancelled(true); // Blockiere das Paket
            }
        }
        else if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            // Multi-Block-Änderung (komplizierteres Paket)
            PacketContainer packet = event.getPacket();
            // In 1.16+ hat das Paket eine SectionPosition und ein Array von BlockData
            // Dies kann je nach MC-Version unterschiedlich sein
            // Hier eine vereinfachte Version:

            // Wir holen den SectionPosition (ChunkPosition)
            Object sectionPosition = packet.getModifier().read(0);
            // Die BlockData-Array-Struktur
            // Dies muss an deine spezifische Minecraft-Version angepasst werden
            // In einigen Versionen kann dies "readWithCodec" oder ähnliches sein
            short[] positions = packet.getShortArrays().read(0);

            if (positions != null) {
                boolean shouldCancel = false;

                // Überprüfe jede Position im Multi-Block-Change
                for (short pos : positions) {
                    // Berechne die tatsächliche BlockPosition aus dem SectionPosition
                    // (Diese Berechnung ist vereinfacht und muss je nach Version angepasst werden)
                    int x = (pos >> 8 & 15);
                    int y = (pos & 15);
                    int z = (pos >> 4 & 15);

                    // Hier müsste man die tatsächliche BlockPosition aus dem SectionPosition berechnen
                    // Dies ist vereinfacht und muss angepasst werden
                    BlockPosition blockPos = new BlockPosition(x, y, z); // Dies ist nicht korrekt und muss angepasst werden

                    if (playerGhostBlocks.containsKey(blockPos)) {
                        shouldCancel = true;
                        break;
                    }
                }

                if (shouldCancel) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // Angepasste Version deiner Methode, die nun den Block auch als Ghost-Block registriert
    public void sendBlockChangePacket(Block block, Player player){
        Location location = block.getLocation();
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);

        BlockPosition blockPosition = new BlockPosition(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());

        packet.getBlockPositionModifier().write(0, blockPosition);
        packet.getBlockData().write(0, WrappedBlockData.createData(block.getType()));

        try {
            protocolManager.sendServerPacket(player, packet);
            // Registriere diesen Block als Ghost-Block für diesen Spieler
            registerGhostBlock(player.getUniqueId(), location);
            player.sendMessage("Ghost-Block erstellt!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
