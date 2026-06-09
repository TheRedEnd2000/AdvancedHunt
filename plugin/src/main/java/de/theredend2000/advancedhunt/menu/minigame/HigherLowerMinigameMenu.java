package de.theredend2000.advancedhunt.menu.minigame;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Random;
import java.util.function.Consumer;

public class HigherLowerMinigameMenu extends MinigameMenu {

    private static final int TOTAL_SLOTS     = 54;
    private static final int BUTTON_START    = 9;
    private static final int BUTTON_END      = 35;
    private static final int HINT_BAR_START  = 36;
    private static final int FEEDBACK_START  = 45;

    private final int maxNumber;
    private final int maxGuesses;
    private final Random random = new Random();

    private int secretNumber;
    private int guessesLeft;

    // Inclusive bounds still in play (for visual feedback)
    private int lowerBound;
    private int upperBound;

    public HigherLowerMinigameMenu(Player player, Main plugin, Consumer<Boolean> onFinish) {
        super(player, plugin, onFinish);
        this.maxNumber  = Math.min(plugin.getConfig().getInt("hint.minigames.higherlower.max-number",  15), 27);
        this.maxGuesses = plugin.getConfig().getInt("hint.minigames.higherlower.max-guesses", 5);
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.minigame.higherlower.title", false);
    }

    @Override
    public int getSlots() {
        return TOTAL_SLOTS;
    }

    @Override
    public void setMenuItems() {
        secretNumber = random.nextInt(maxNumber) + 1;
        guessesLeft  = maxGuesses;
        lowerBound   = 1;
        upperBound   = maxNumber;

        fillBackground(new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE).hideTooltip(true).build());
        renderNumberButtons();
        renderHintBar();
        renderFeedback(null);

        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                "minigame.higherlower.start", true,
                "%max%",     String.valueOf(maxNumber),
                "%guesses%", String.valueOf(maxGuesses)));
    }

    private void renderNumberButtons() {
        for (int n = 1; n <= maxNumber; n++) {
            int slot = BUTTON_START + (n - 1);
            boolean eliminated = n < lowerBound || n > upperBound;

            if (eliminated) {
                updateSlot(slot, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE)
                        .setDisplayName(plugin.getMessageManager().getMessage(
                                "gui.minigame.higherlower.number.eliminated", false,
                                "%n%", String.valueOf(n)))
                        .build());
            } else {
                updateSlot(slot, new ItemBuilder(XMaterial.LIME_STAINED_GLASS_PANE)
                        .setDisplayName(plugin.getMessageManager().getMessage(
                                "gui.minigame.higherlower.number.active", false,
                                "%n%", String.valueOf(n)))
                        .build());
            }
        }
        for (int slot = BUTTON_START + maxNumber; slot <= BUTTON_END; slot++) {
            updateSlot(slot, new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE).hideTooltip(true).build());
        }
    }

    private void renderHintBar() {
        for (int i = 0; i < 9; i++) {
            int slot = HINT_BAR_START + i;
            if (i < guessesLeft) {
                updateSlot(slot, new ItemBuilder(XMaterial.YELLOW_STAINED_GLASS_PANE)
                        .setDisplayName(plugin.getMessageManager().getMessage(
                                "gui.minigame.higherlower.guesses.remaining", false,
                                "%left%", String.valueOf(guessesLeft)))
                        .build());
            } else {
                updateSlot(slot, new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE).hideTooltip(true).build());
            }
        }
    }

    private void renderFeedback(String direction) {
        for (int i = 0; i < 9; i++) {
            updateSlot(FEEDBACK_START + i, new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE)
                    .hideTooltip(true).build());
        }

        if (direction == null) {
            updateSlot(FEEDBACK_START + 4, new ItemBuilder(XMaterial.PAPER)
                    .setDisplayName(plugin.getMessageManager().getMessage(
                            "gui.minigame.higherlower.feedback.neutral", false))
                    .build());
        } else if ("higher".equals(direction)) {
            for (int i = 2; i <= 6; i++) {
                updateSlot(FEEDBACK_START + i, new ItemBuilder(XMaterial.LIME_STAINED_GLASS_PANE)
                        .setDisplayName(plugin.getMessageManager().getMessage(
                                "gui.minigame.higherlower.feedback.higher", false))
                        .build());
            }
        } else if ("lower".equals(direction)) {
            for (int i = 2; i <= 6; i++) {
                updateSlot(FEEDBACK_START + i, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE)
                        .setDisplayName(plugin.getMessageManager().getMessage(
                                "gui.minigame.higherlower.feedback.lower", false))
                        .build());
            }
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (finished) return;

        int slot = event.getSlot();

        if (slot < BUTTON_START || slot > BUTTON_START + maxNumber - 1) return;

        int guess = (slot - BUTTON_START) + 1;

        if (guess < lowerBound || guess > upperBound) {
            playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                    XSound.BLOCK_STONE_PLACE.get(), 0.5f, 0.5f);
            return;
        }

        if (guess == secretNumber) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                    "minigame.higherlower.correct", true,
                    "%n%", String.valueOf(secretNumber)));
            playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                    XSound.ENTITY_PLAYER_LEVELUP.get(), 1f, 1f);
            scheduleTask(() -> finish(true), 20);
            return;
        }

        guessesLeft--;
        playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                XSound.ENTITY_VILLAGER_NO.get(), 1f, 1f);

        String direction;
        if (guess < secretNumber) {
            lowerBound = guess + 1;
            direction  = "higher";
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                    "minigame.higherlower.higher", true,
                    "%left%", String.valueOf(guessesLeft)));
        } else {
            upperBound = guess - 1;
            direction  = "lower";
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                    "minigame.higherlower.lower", true,
                    "%left%", String.valueOf(guessesLeft)));
        }

        renderNumberButtons();
        renderFeedback(direction);
        renderHintBar();

        if (guessesLeft <= 0) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                    "minigame.higherlower.lose", true,
                    "%n%", String.valueOf(secretNumber)));
            playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                    XSound.ENTITY_VILLAGER_NO.get(), 1f, 0.5f);
            scheduleTask(() -> finish(false), 40);
        }
    }
}
