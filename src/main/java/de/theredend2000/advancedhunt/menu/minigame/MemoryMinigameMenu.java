package de.theredend2000.advancedHunt.menu.minigame;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedHunt.Main;
import de.theredend2000.advancedHunt.util.ItemBuilder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class MemoryMinigameMenu extends MinigameMenu {

    private final List<Integer> sequence;
    private final List<Integer> playerSequence;
    private final Random random;
    private int round;
    private boolean showingSequence;
    
    // Configurable
    private final int maxRounds;
    private final int displayTime;
    private final int[] GAME_SLOTS = {11, 13, 15, 29, 31, 33}; // 6 buttons
    private final XMaterial[] COLORS = {
            XMaterial.RED_WOOL, XMaterial.ORANGE_WOOL, XMaterial.YELLOW_WOOL,
            XMaterial.LIME_WOOL, XMaterial.BLUE_WOOL, XMaterial.PURPLE_WOOL
    };

    public MemoryMinigameMenu(Player player, Main plugin, Consumer<Boolean> onFinish) {
        super(player, plugin, onFinish);
        this.sequence = new ArrayList<>();
        this.playerSequence = new ArrayList<>();
        this.random = new Random();
        this.round = 0;
        this.maxRounds = plugin.getConfig().getInt("minigames.memory.rounds", 5);
        this.displayTime = plugin.getConfig().getInt("minigames.memory.display-time", 20);
    }

    @Override
    public String getMenuName() {
        return "Memory Game";
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public void setMenuItems() {
        fillBackground(new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE).setDisplayName(" ").build());
        
        // Set up game buttons
        for (int i = 0; i < GAME_SLOTS.length; i++) {
            updateSlot(GAME_SLOTS[i], new ItemBuilder(COLORS[i]).setDisplayName("§fButton " + (i + 1)).build());
        }

        startRound();
    }

    private void startRound() {
        round++;
        playerSequence.clear();
        showingSequence = true;
        
        // Add new step to sequence
        sequence.add(GAME_SLOTS[random.nextInt(GAME_SLOTS.length)]);
        
        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("minigame.memory.watch"));
        
        new BukkitRunnable() {
            int index = 0;
            
            @Override
            public void run() {
                if (finished) {
                    this.cancel();
                    return;
                }

                if (index >= sequence.size()) {
                    showingSequence = false;
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("minigame.memory.repeat"));
                    this.cancel();
                    return;
                }

                int slot = sequence.get(index);
                flashSlot(slot);
                index++;
            }
        }.runTaskTimer(plugin, displayTime, displayTime);
    }

    private void flashSlot(int slot) {
        // Find color index
        int colorIndex = -1;
        for(int i=0; i<GAME_SLOTS.length; i++) {
            if(GAME_SLOTS[i] == slot) {
                colorIndex = i;
                break;
            }
        }
        
        if(colorIndex == -1) return;

        // Highlight
        updateSlot(slot, new ItemBuilder(XMaterial.WHITE_WOOL).setDisplayName("§fButton " + (colorIndex + 1)).build());
        playerMenuUtility.playSound(playerMenuUtility.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1 + (colorIndex * 0.2f));

        // Reset after delay
        int finalColorIndex = colorIndex;
        new BukkitRunnable() {
            @Override
            public void run() {
                if(!finished) {
                    updateSlot(slot, new ItemBuilder(COLORS[finalColorIndex]).setDisplayName("§fButton " + (finalColorIndex + 1)).build());
                }
            }
        }.runTaskLater(plugin, 10);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (showingSequence || finished) return;

        int slot = event.getSlot();
        boolean isGameSlot = false;
        for (int s : GAME_SLOTS) {
            if (s == slot) {
                isGameSlot = true;
                break;
            }
        }

        if (!isGameSlot) return;

        flashSlot(slot);
        playerSequence.add(slot);

        // Check correctness
        int currentIndex = playerSequence.size() - 1;
        if (playerSequence.get(currentIndex).equals(sequence.get(currentIndex))) {
            // Correct so far
            if (playerSequence.size() == sequence.size()) {
                // Round complete
                if (round >= maxRounds) {
                    playerMenuUtility.playSound(playerMenuUtility.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                    finish(true);
                } else {
                    playerMenuUtility.playSound(playerMenuUtility.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            startRound();
                        }
                    }.runTaskLater(plugin, 30);
                }
            }
        } else {
            // Wrong
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("minigame.memory.wrong"));
            playerMenuUtility.playSound(playerMenuUtility.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            finish(false);
        }
    }
}
