package de.theredend2000.advancedhunt.menu.minigame;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class OddOneOutMinigameMenu extends MinigameMenu {

    /**
     * 10 difficulty groups, ordered easiest → hardest.
     * Each group has at least 4 materials; two are picked randomly each round.
     *
     * Rule of thumb per tier:
     *  1-2  → clearly different colours/shapes   (warm-up)
     *  3-4  → same material family, visible diff  (easy)
     *  5-6  → similar tones, same texture family  (medium)
     *  7-8  → almost identical, subtle difference (hard)
     *  9-10 → near-identical palette/texture      (very hard)
     */
    private static final XMaterial[][] DIFFICULTY_GROUPS = {

            // ── Tier 1 – bold primary wools ──────────────────────────────
            {
                    XMaterial.RED_WOOL,     XMaterial.BLUE_WOOL,
                    XMaterial.YELLOW_WOOL,  XMaterial.GREEN_WOOL,
                    XMaterial.BLACK_WOOL,   XMaterial.ORANGE_WOOL,
                    XMaterial.PURPLE_WOOL,  XMaterial.WHITE_WOOL
            },

            // ── Tier 2 – different block categories (still very obvious) ─
            {
                    XMaterial.RED_CONCRETE,    XMaterial.BLUE_CONCRETE,
                    XMaterial.YELLOW_CONCRETE, XMaterial.GREEN_CONCRETE,
                    XMaterial.BLACK_CONCRETE,  XMaterial.ORANGE_CONCRETE,
                    XMaterial.PURPLE_CONCRETE, XMaterial.WHITE_CONCRETE
            },

            // ── Tier 3 – mid-tone wools (warm vs cool) ───────────────────
            {
                    XMaterial.CYAN_WOOL,       XMaterial.LIGHT_BLUE_WOOL,
                    XMaterial.LIME_WOOL,       XMaterial.GREEN_WOOL,
                    XMaterial.MAGENTA_WOOL,    XMaterial.PINK_WOOL,
                    XMaterial.PURPLE_WOOL,     XMaterial.BLUE_WOOL
            },

            // ── Tier 4 – mid-tone concretes (same palette as tier 3) ─────
            {
                    XMaterial.CYAN_CONCRETE,       XMaterial.LIGHT_BLUE_CONCRETE,
                    XMaterial.LIME_CONCRETE,       XMaterial.GREEN_CONCRETE,
                    XMaterial.MAGENTA_CONCRETE,    XMaterial.PINK_CONCRETE,
                    XMaterial.PURPLE_CONCRETE,     XMaterial.BLUE_CONCRETE
            },

            // ── Tier 5 – stone family (grey/brown, similar brightness) ───
            {
                    XMaterial.STONE,       XMaterial.COBBLESTONE,
                    XMaterial.ANDESITE,    XMaterial.DIORITE,
                    XMaterial.GRANITE,     XMaterial.DEEPSLATE,
                    XMaterial.TUFF,        XMaterial.CALCITE
            },

            // ── Tier 6 – wood planks (organic grain, different hues) ─────
            {
                    XMaterial.OAK_PLANKS,      XMaterial.BIRCH_PLANKS,
                    XMaterial.SPRUCE_PLANKS,   XMaterial.JUNGLE_PLANKS,
                    XMaterial.ACACIA_PLANKS,   XMaterial.DARK_OAK_PLANKS,
                    XMaterial.MANGROVE_PLANKS, XMaterial.CHERRY_PLANKS
            },

            // ── Tier 7 – terracotta (muted, earthy – hard to tell apart) ─
            {
                    XMaterial.TERRACOTTA,              XMaterial.WHITE_TERRACOTTA,
                    XMaterial.LIGHT_GRAY_TERRACOTTA,   XMaterial.GRAY_TERRACOTTA,
                    XMaterial.BROWN_TERRACOTTA,        XMaterial.ORANGE_TERRACOTTA,
                    XMaterial.YELLOW_TERRACOTTA,       XMaterial.RED_TERRACOTTA
            },

            // ── Tier 8 – glazed terracotta (complex pattern, similar hues)
            {
                    XMaterial.WHITE_GLAZED_TERRACOTTA,      XMaterial.LIGHT_GRAY_GLAZED_TERRACOTTA,
                    XMaterial.GRAY_GLAZED_TERRACOTTA,       XMaterial.BROWN_GLAZED_TERRACOTTA,
                    XMaterial.ORANGE_GLAZED_TERRACOTTA,     XMaterial.YELLOW_GLAZED_TERRACOTTA,
                    XMaterial.GREEN_GLAZED_TERRACOTTA,      XMaterial.CYAN_GLAZED_TERRACOTTA
            },

            // ── Tier 9 – pale/neutral wools (very similar brightness) ────
            {
                    XMaterial.WHITE_WOOL,        XMaterial.LIGHT_GRAY_WOOL,
                    XMaterial.GRAY_WOOL,         XMaterial.WHITE_CONCRETE,
                    XMaterial.LIGHT_GRAY_CONCRETE, XMaterial.GRAY_CONCRETE,
                    XMaterial.QUARTZ_BLOCK,      XMaterial.SNOW_BLOCK
            },

            // ── Tier 10 – polished stones (near-identical grey palette) ──
            {
                    XMaterial.POLISHED_ANDESITE,   XMaterial.POLISHED_DIORITE,
                    XMaterial.POLISHED_GRANITE,    XMaterial.POLISHED_DEEPSLATE,
                    XMaterial.SMOOTH_STONE,        XMaterial.SMOOTH_QUARTZ,
                    XMaterial.CHISELED_STONE_BRICKS, XMaterial.STONE_BRICKS
            }
    };

    private static final int MAX_DIFFICULTY = DIFFICULTY_GROUPS.length; // 10
    private static final int TOTAL_SLOTS    = 54;

    private final int    maxRounds;
    private final int    baseTime;
    private final int    startDifficulty;

    private final Random random = new Random();

    private int     round           = 0;
    private int     currentDifficulty;   // 1-based, updated each round
    private int     roundsOnDifficulty = 0; // rounds spent without going up
    private int     oddSlot         = -1;
    private boolean waitingForClick = false;

    private XMaterial currentMain = null;
    private XMaterial currentOdd  = null;

    private BukkitTask countdownTask = null;

    public OddOneOutMinigameMenu(Player player, Main plugin, Consumer<Boolean> onFinish) {
        super(player, plugin, onFinish);
        this.maxRounds      = plugin.getConfig().getInt("hint.minigames.oddoneout.rounds",     10);
        this.baseTime       = plugin.getConfig().getInt("hint.minigames.oddoneout.base-time",  200);
        int raw             = plugin.getConfig().getInt("hint.minigames.oddoneout.difficulty",   4);
        this.startDifficulty    = Math.max(1, Math.min(MAX_DIFFICULTY, raw));
        this.currentDifficulty  = this.startDifficulty;
    }

    // ------------------------------------------------------------------ //
    //  Menu basics                                                         //
    // ------------------------------------------------------------------ //

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.minigame.oddoneout.title", false);
    }

    @Override
    public int getSlots() {
        return TOTAL_SLOTS;
    }

    @Override
    public void setMenuItems() {
        fillBackground(new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE).hideTooltip(true).build());
        scheduleTask(this::startRound, 10);
    }

    // ------------------------------------------------------------------ //
    //  Difficulty progression                                              //
    // ------------------------------------------------------------------ //

    /**
     * After each round, decide whether to increase the difficulty.
     *
     * Already at max → always stay.
     * Otherwise the probability of going up grows the longer we have stayed
     * on the current tier:
     *
     *   P(increase) = roundsOnDifficulty / (roundsOnDifficulty + 2)
     *
     *   rounds stayed │  0   1    2    3    4    5 …
     *   P(increase)   │  0%  33%  50%  60%  67%  71% …
     *
     * This means on the very first round of a new tier there is NO increase,
     * giving the player at least one "safe" round to adjust. After that the
     * pressure builds until an increase is virtually guaranteed.
     */
    private void updateDifficulty() {
        if (currentDifficulty >= MAX_DIFFICULTY) {
            roundsOnDifficulty++;
            return;
        }

        double chance = (double) roundsOnDifficulty / (roundsOnDifficulty + 2.0);
        if (random.nextDouble() < chance) {
            currentDifficulty++;
            roundsOnDifficulty = 0;
        } else {
            roundsOnDifficulty++;
        }
    }

    // ------------------------------------------------------------------ //
    //  Round logic                                                         //
    // ------------------------------------------------------------------ //

    private void startRound() {
        if (finished) return;
        round++;

        // Advance difficulty (skip on very first round)
        if (round > 1) updateDifficulty();

        waitingForClick = false;

        XMaterial[] group = DIFFICULTY_GROUPS[currentDifficulty - 1];
        List<XMaterial> pool = new ArrayList<>();
        for (XMaterial m : group) pool.add(m);
        Collections.shuffle(pool, random);
        currentMain = pool.get(0);
        currentOdd  = pool.get(1);

        oddSlot = random.nextInt(TOTAL_SLOTS);

        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                "minigame.oddoneout.find", true,
                "%round%",      String.valueOf(round),
                "%max%",        String.valueOf(maxRounds),
                "%difficulty%", String.valueOf(currentDifficulty)));

        waitingForClick = true;
        startCountdown(baseTime);
    }

    // ------------------------------------------------------------------ //
    //  Countdown                                                           //
    // ------------------------------------------------------------------ //

    private void startCountdown(long totalTicks) {
        cancelCountdown();
        final int[] secondsLeft = {(int) Math.ceil(totalTicks / 20.0)};
        renderAllItems(secondsLeft[0]);

        countdownTask = scheduleTaskTimer(() -> {
            if (!waitingForClick || finished) {
                cancelCountdown();
                return;
            }
            secondsLeft[0]--;
            if (secondsLeft[0] <= 0) {
                cancelCountdown();
                if (!waitingForClick || finished) return;
                waitingForClick = false;
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                        "minigame.oddoneout.timeout", true));
                playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                        XSound.ENTITY_VILLAGER_NO.get(), 1, 1f);
                finish(false);
            } else {
                renderAllItems(secondsLeft[0]);
            }
        }, 20L, 20L);
    }

    private void cancelCountdown() {
        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
        }
        countdownTask = null;
    }

    private void renderAllItems(int seconds) {
        String timeName = plugin.getMessageManager().getMessage(
                "gui.minigame.oddoneout.timer", false,
                "%seconds%", String.valueOf(seconds));

        ItemStack mainItem = new ItemBuilder(currentMain)
                .setDisplayName(timeName)
                .build();
        ItemStack oddItem = new ItemBuilder(currentOdd)
                .setDisplayName(timeName)
                .build();

        for (int i = 0; i < TOTAL_SLOTS; i++) {
            updateSlot(i, i == oddSlot ? oddItem : mainItem);
        }
    }

    // ------------------------------------------------------------------ //
    //  Click handling                                                      //
    // ------------------------------------------------------------------ //

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (!waitingForClick || finished) return;

        int slot = event.getSlot();

        if (slot == oddSlot) {
            waitingForClick = false;
            cancelCountdown();
            playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                    XSound.BLOCK_NOTE_BLOCK_PLING.get(), 1, 2f);

            if (round >= maxRounds) {
                playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                        XSound.ENTITY_PLAYER_LEVELUP.get(), 1, 1f);
                scheduleTask(() -> finish(true), 10);
            } else {
                playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                        XSound.ENTITY_EXPERIENCE_ORB_PICKUP.get(), 1, 1f);
                scheduleTask(this::startRound, 20);
            }
        } else {
            waitingForClick = false;
            cancelCountdown();
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                    "minigame.oddoneout.wrong", true));
            playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                    XSound.ENTITY_VILLAGER_NO.get(), 1, 1f);
            finish(false);
        }
    }
}