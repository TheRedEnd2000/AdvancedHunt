package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInputListener implements Listener {

    private final Main plugin;
    private final Map<UUID, InputRequest> awaitingInput = new ConcurrentHashMap<>();

    public ChatInputListener(Main plugin) {
        this.plugin = plugin;
    }

    public void requestInput(Player player, Consumer<String> callback) {
        UUID uuid = player.getUniqueId();
        cancelRequest(uuid);

        player.closeInventory();
        player.sendMessage(plugin.getMessageManager().getMessage("chat_input.request"));

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            InputRequest request = awaitingInput.remove(uuid);
            if (request != null) {
                player.sendMessage(plugin.getMessageManager().getMessage("chat_input.timeout"));
            }
        }, 1200L); // 60 seconds timeout

        awaitingInput.put(uuid, new InputRequest(callback, task));
    }

    private void cancelRequestDueToAction(Player player) {
        UUID uuid = player.getUniqueId();
        if (!awaitingInput.containsKey(uuid)) {
            return;
        }

        cancelRequest(uuid);
        player.sendMessage(plugin.getMessageManager().getMessage("chat_input.canceled"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        String message = event.getMessage();

        InputRequest request = awaitingInput.remove(playerId);
        if (request == null) {
            return;
        }

        event.setCancelled(true);

        Bukkit.getScheduler().runTask(plugin, () -> {
            request.task().cancel();

            Player syncPlayer = Bukkit.getPlayer(playerId);
            if (syncPlayer == null || !syncPlayer.isOnline()) {
                return;
            }

            if (message.equalsIgnoreCase("cancel")) {
                syncPlayer.sendMessage(plugin.getMessageManager().getMessage("chat_input.canceled"));
                return;
            }

            request.callback().accept(message);
        });
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }

        // Allow vertical-only movement (jump/crouch); cancel only on horizontal block movement.
        // Also ignores pure head rotation.
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        cancelRequestDueToAction(event.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        cancelRequestDueToAction(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        cancelRequestDueToAction(event.getPlayer());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        cancelRequestDueToAction(event.getPlayer());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        cancelRequestDueToAction(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelRequest(event.getPlayer().getUniqueId());
    }

    private void cancelRequest(UUID uuid) {
        InputRequest request = awaitingInput.remove(uuid);
        if (request != null) {
            request.task().cancel();
        }
    }

    private static final class InputRequest {
        private final Consumer<String> callback;
        private final BukkitTask task;

        private InputRequest(Consumer<String> callback, BukkitTask task) {
            this.callback = callback;
            this.task = task;
        }

        private Consumer<String> callback() {
            return callback;
        }

        private BukkitTask task() {
            return task;
        }
    }
}
