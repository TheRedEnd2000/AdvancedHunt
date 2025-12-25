package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatInputListener implements Listener {

    private final Main plugin;
    private final Map<UUID, InputRequest> awaitingInput = new HashMap<>();

    public ChatInputListener(Main plugin) {
        this.plugin = plugin;
    }

    public void requestInput(Player player, Consumer<String> callback) {
        UUID uuid = player.getUniqueId();
        cancelRequest(uuid);

        player.closeInventory();
        player.sendMessage(plugin.getMessageManager().getMessage("chat_input.request"));

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (awaitingInput.containsKey(uuid)) {
                awaitingInput.remove(uuid);
                player.sendMessage(plugin.getMessageManager().getMessage("chat_input.timeout"));
            }
        }, 1200L); // 60 seconds timeout

        awaitingInput.put(uuid, new InputRequest(callback, task));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (awaitingInput.containsKey(uuid)) {
            event.setCancelled(true);

            InputRequest request = awaitingInput.remove(uuid);
            request.task().cancel();

            String message = event.getMessage();
            
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(plugin.getMessageManager().getMessage("chat_input.canceled"));
                return;
            }

            // Run on the main thread as chat is async
            Bukkit.getScheduler().runTask(plugin, () -> request.callback().accept(message));
        }
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

    private record InputRequest(Consumer<String> callback, BukkitTask task) {}
}
