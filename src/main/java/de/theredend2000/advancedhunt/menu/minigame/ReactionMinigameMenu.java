package de.theredend2000.advancedHunt.menu.minigame;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedHunt.Main;
import de.theredend2000.advancedHunt.util.ItemBuilder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;
import java.util.function.Consumer;

public class ReactionMinigameMenu extends MinigameMenu {

    private int currentSlot;
    private int currentCount;
    private boolean clickedCorrectSlot;
    private int lastClicked;
    private final Random random;
    private BukkitTask failTask;
    private boolean active;

    private final int hintCount;
    private final int updateTime;

    public ReactionMinigameMenu(Player player, Main plugin, Consumer<Boolean> onFinish) {
        super(player, plugin, onFinish);
        this.random = new Random();
        this.hintCount = plugin.getConfig().getInt("minigames.reaction.required-clicks", 5);
        this.updateTime = plugin.getConfig().getInt("minigames.reaction.update-time", 20);
    }

    @Override
    public String getMenuName() {
        return "Reaction Game";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void setMenuItems() {
        this.clickedCorrectSlot = true;
        this.currentCount = 0;
        this.lastClicked = -1;
        this.active = true;

        fillBackground(new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE).setDisplayName("§c").build());

        new BukkitRunnable() {
            @Override
            public void run() {
                updateFrame(false);
            }
        }.runTaskLater(plugin, 10);
    }

    private void restartFailedTask() {
        if (failTask != null) failTask.cancel();

        failTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateFrame(true);
            }
        }.runTaskLater(plugin, updateTime + 5);
    }

    private void updateFrame(boolean timeout) {
        if (!active || finished) return;

        if (timeout) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("minigame.reaction.timeout"));
            finish(false);
            return;
        }
        if (!clickedCorrectSlot) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("minigame.reaction.wrong_click"));
            finish(false);
            return;
        }

        // Reset old slot
        if (currentSlot >= 0 && currentSlot < getSlots()) {
            updateSlot(currentSlot, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE).setDisplayName("§c").build());
        }

        currentSlot = getRandomSlot();
        updateSlot(currentSlot, new ItemBuilder(XMaterial.LIME_STAINED_GLASS_PANE)
                .setDisplayName("§aClick Me!")
                .setLore("§6" + (currentCount + 1) + "§7/§6" + hintCount)
                .build());
        
        clickedCorrectSlot = false;
    }

    private int getRandomSlot() {
        int nextNum;
        do {
            nextNum = random.nextInt(getSlots());
        } while (nextNum == lastClicked);
        return nextNum;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (!active || finished) return;

        if (event.getSlot() == currentSlot) {
            currentCount++;
            playerMenuUtility.playSound(playerMenuUtility.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);
            
            if (currentCount >= hintCount) {
                playerMenuUtility.playSound(playerMenuUtility.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                finish(true);
                return;
            }
            
            clickedCorrectSlot = true;
            lastClicked = currentSlot;

            new BukkitRunnable() {
                @Override
                public void run() {
                    updateFrame(false);
                }
            }.runTaskLater(plugin, 5);

            restartFailedTask();
        } else {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("minigame.reaction.wrong_click"));
            finish(false);
        }
    }
    
    @Override
    protected void finish(boolean success) {
        active = false;
        if (failTask != null) failTask.cancel();
        super.finish(success);
    }
}
