package de.theredend2000.advancedhunt.platform;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public interface PlatformAdapter {

    boolean isAir(Material material);

    void spawnParticle(Location location, String particleName, int count,
                       double offsetX, double offsetY, double offsetZ, double speed);

    void spawnParticleForPlayer(Player player, Location location, String particleName, int count,
                                double offsetX, double offsetY, double offsetZ, double speed);

    void applyHideTooltip(ItemMeta meta, boolean hide);

    void applyUnbreakable(ItemMeta meta, boolean unbreakable);

    void applyCustomModelData(ItemMeta meta, Integer customModelData);

    void applySkullOwner(ItemMeta meta, UUID ownerUuid);

    void applySkullOwner(ItemMeta meta, String ownerName);

    /**
     * Ensure the given item stack represents a player head in the current server version.
     * Implementations may replace the stack type/durability if needed.
     */
    ItemStack ensurePlayerHeadItem(ItemStack item);

    /**
     * 1.9+ fires interact events for both hands; 1.8 only has the main hand.
     */

    boolean isMainHandInteract(PlayerInteractEvent event);

    /**
     * Version-safe block state representation.
     */

    String getBlockStateString(Block block);

    /**
     * 1.8 does not support silent entities.
     */

    void setFireworkSilent(Firework firework, boolean silent);

    /**
     * Spawns a client-side (packet-only) armor stand hologram for a single player.
     * Implementations may require PacketEvents to be installed.
     *
     * Expected properties: CustomNameVisible, Invisible, Small, NoBasePlate, NoGravity, Marker.
     *
     * @return true if the packets were sent successfully
     */
    boolean spawnHologramArmorStandForPlayer(Player player, int entityId, UUID entityUuid, Location location, String customName);

    /**
     * Destroys one or more client-side entities for a single player.
     * Implementations may require PacketEvents to be installed.
     */
    boolean destroyEntitiesForPlayer(Player player, int... entityIds);

    /**
     * Spawns a client-side (packet-only) glowing marker for a block for a single player.
     * <p>
     * Intended usage is a "block outline" effect without placing fake blocks (to avoid z-fighting).
     * Implementations may require PacketEvents to be installed.
     * <p>
     * Notes:
     * - 1.9+ can use an invisible + glowing entity (typically a shulker) to get a cube-like outline.
     * - 1.8.x has no built-in glowing outline; implementations may return false.
     *
     * @return true if packets were sent successfully
     */
    boolean spawnGlowingBlockMarkerForPlayer(Player player, int entityId, UUID entityUuid, Location blockLocation);

    /**
     * Sends a clickable text message to the player. On 1.15+, the text will have a click event
     * to copy the value to clipboard. On older versions, returns a plain formatted message.
     *
     * @param player the player to send the message to
     * @param displayText the text to display in chat
     * @param copyText the text to copy when clicked (1.15+ only)
     * @param hoverText the tooltip text when hovering (can be null)
     */
    void sendClickableCopyText(Player player, String displayText, String copyText, String hoverText);
}
