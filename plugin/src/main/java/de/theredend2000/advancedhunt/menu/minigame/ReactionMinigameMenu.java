package de.theredend2000.advancedhunt.menu.minigame;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.ItemBuilder;
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
        return plugin.getMessageManager().getMessage("gui.minigame.reaction.title", false);
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

        fillBackground(new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE).hideTooltip(true).build());

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
            updateSlot(currentSlot, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE).hideTooltip(true).build());
        }

        currentSlot = getRandomSlot();
        updateSlot(currentSlot, new ItemBuilder(XMaterial.LIME_STAINED_GLASS_PANE)
            .setDisplayName(plugin.getMessageManager().getMessage("gui.minigame.reaction.click.name", false))
            .setLore(plugin.getMessageManager().getMessageList("gui.minigame.reaction.click.lore", false,
                "%current%", String.valueOf(currentCount + 1),
                "%required%", String.valueOf(hintCount)))
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
            playerMenuUtility.playSound(playerMenuUtility.getLocation(), XSound.BLOCK_NOTE_BLOCK_PLING.get(), 1, 2);
            
            if (currentCount >= hintCount) {
                playerMenuUtility.playSound(playerMenuUtility.getLocation(), XSound.ENTITY_PLAYER_LEVELUP.get(), 1, 1);
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
