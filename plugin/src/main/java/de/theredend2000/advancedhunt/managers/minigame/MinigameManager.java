package de.theredend2000.advancedhunt.managers.minigame;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.minigame.MemoryMinigameMenu;
import de.theredend2000.advancedhunt.menu.minigame.ReactionMinigameMenu;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

public class MinigameManager {

    private final Main plugin;
    private final HashMap<UUID, MinigameType> activeSessions;

    public MinigameManager(Main plugin) {
        this.plugin = plugin;
        this.activeSessions = new HashMap<>();
    }

    public void startMinigame(Player player, MinigameType type, Consumer<Boolean> onFinish) {
        if (!plugin.getConfig().getBoolean("minigames.enabled", true)) {
            onFinish.accept(true);
            return;
        }

        if (!isMinigameEnabled(type)) {
            onFinish.accept(true);
            return;
        }

        if (activeSessions.containsKey(player.getUniqueId())) {
            return;
        }

        activeSessions.put(player.getUniqueId(), type);

        switch (type) {
            case REACTION:
                new ReactionMinigameMenu(player, plugin, (success) -> {
                    endSession(player);
                    onFinish.accept(success);
                }).open();
                break;
            case MEMORY:
                new MemoryMinigameMenu(player, plugin, (success) -> {
                    endSession(player);
                    onFinish.accept(success);
                }).open();
                break;
        }
    }

    public boolean isMinigameEnabled(MinigameType type) {
        return plugin.getConfig().getBoolean("minigames." + type.name().toLowerCase() + ".enabled", true);
    }

    public void endSession(Player player) {
        activeSessions.remove(player.getUniqueId());
    }

    public boolean isPlaying(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }
}
