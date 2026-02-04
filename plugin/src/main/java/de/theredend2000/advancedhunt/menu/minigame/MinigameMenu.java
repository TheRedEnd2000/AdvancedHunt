package de.theredend2000.advancedhunt.menu.minigame;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class MinigameMenu extends Menu {

    protected final List<BukkitTask> activeTasks = new ArrayList<>();

    protected final Consumer<Boolean> onFinish;
    protected boolean finished = false;

    public MinigameMenu(Player player, Main plugin, Consumer<Boolean> onFinish) {
        super(player, plugin);
        this.onFinish = onFinish;
    }

    protected void finish(boolean success) {
        if (finished) return;
        finished = true;
        cancelAllTasks();
        playerMenuUtility.closeInventory();
        if (onFinish != null) {
            onFinish.accept(success);
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        cancelAllTasks();
        if (!finished) {
            // If closed manually/abruptly, consider it a failure or just cancel
            // Depending on game design, we might want to fail.
            // For now, let's assume closing without finishing is a fail/cancel.
            if (onFinish != null) {
                onFinish.accept(false);
            }
        }
    }

    /**
     * Schedule a task to run after a delay and track it for cleanup.
     * @param action The action to run
     * @param delay The delay in ticks
     * @return The scheduled task
     */
    protected BukkitTask scheduleTask(Runnable action, long delay) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                action.run();
            }
        }.runTaskLater(plugin, delay);
        activeTasks.add(task);
        return task;
    }

    /**
     * Schedule a repeating task and track it for cleanup.
     * @param action The action to run
     * @param delay The initial delay in ticks
     * @param period The period between executions in ticks
     * @return The scheduled task
     */
    protected BukkitTask scheduleTaskTimer(Runnable action, long delay, long period) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                action.run();
            }
        }.runTaskTimer(plugin, delay, period);
        activeTasks.add(task);
        return task;
    }

    /**
     * Cancel all active tasks to prevent memory leaks and scheduler buildup.
     */
    protected void cancelAllTasks() {
        for (BukkitTask task : activeTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        activeTasks.clear();
    }
    
    @Override
    public void handleMenu(InventoryClickEvent e) {
        // Default implementation, can be overridden
    }

    @Override
    public void addCloseOrBack() {
    }
}
