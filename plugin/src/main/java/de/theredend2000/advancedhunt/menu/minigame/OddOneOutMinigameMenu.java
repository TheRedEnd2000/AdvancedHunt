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
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class OddOneOutMinigameMenu extends MinigameMenu {

    private static final XMaterial[][][] DIFFICULTY_PAIRS = {
            {
                    {XMaterial.CYAN_CONCRETE_POWDER, XMaterial.DIAMOND_BLOCK},
                    {XMaterial.MAGENTA_WOOL,         XMaterial.PINK_CONCRETE_POWDER},
                    {XMaterial.ACACIA_PLANKS,        XMaterial.NETHER_WART_BLOCK},
                    {XMaterial.BIRCH_PLANKS,         XMaterial.LIME_WOOL},
                    {XMaterial.GLOWSTONE,            XMaterial.YELLOW_CONCRETE_POWDER},
                    {XMaterial.CUT_RED_SANDSTONE,    XMaterial.RED_TERRACOTTA},
                    {XMaterial.COAL_BLOCK,           XMaterial.GRAY_CONCRETE_POWDER},
                    {XMaterial.HAY_BLOCK,            XMaterial.LIME_TERRACOTTA}
            },
            {
                    {XMaterial.BLUE_TERRACOTTA,   XMaterial.LIGHT_BLUE_TERRACOTTA},
                    {XMaterial.BROWN_WOOL,        XMaterial.SMOOTH_RED_SANDSTONE},
                    {XMaterial.MAGENTA_CONCRETE,  XMaterial.MAGENTA_TERRACOTTA},
                    {XMaterial.ORANGE_TERRACOTTA, XMaterial.PUMPKIN},
                    {XMaterial.CYAN_TERRACOTTA,   XMaterial.SOUL_SAND},
                    {XMaterial.GREEN_WOOL,        XMaterial.MELON},
                    {XMaterial.GRAY_TERRACOTTA,   XMaterial.NETHERRACK},
                    {XMaterial.BLACK_TERRACOTTA,  XMaterial.NETHERITE_BLOCK}
            },
            {
                    {XMaterial.DIRT,                    XMaterial.RED_SAND},
                    {XMaterial.MAGENTA_CONCRETE_POWDER, XMaterial.PINK_CONCRETE},
                    {XMaterial.GREEN_CONCRETE_POWDER,   XMaterial.GREEN_TERRACOTTA},
                    {XMaterial.CLAY,                    XMaterial.WHITE_CONCRETE},
                    {XMaterial.SPONGE,                  XMaterial.YELLOW_WOOL},
                    {XMaterial.POLISHED_DIORITE,        XMaterial.SNOW_BLOCK},
                    {XMaterial.ORANGE_CONCRETE_POWDER,  XMaterial.RED_SANDSTONE},
                    {XMaterial.CRACKED_STONE_BRICKS,    XMaterial.LIGHT_GRAY_TERRACOTTA}
            },
            {
                    {XMaterial.GOLD_BLOCK,            XMaterial.YELLOW_CONCRETE},
                    {XMaterial.BLUE_CONCRETE,         XMaterial.PURPLE_CONCRETE},
                    {XMaterial.DIORITE,               XMaterial.QUARTZ_BLOCK},
                    {XMaterial.BLACK_CONCRETE_POWDER, XMaterial.GRAY_CONCRETE},
                    {XMaterial.GRAY_WOOL,             XMaterial.POLISHED_BLACKSTONE},
                    {XMaterial.BROWN_CONCRETE,        XMaterial.BROWN_CONCRETE_POWDER},
                    {XMaterial.BONE_BLOCK,            XMaterial.IRON_BLOCK},
                    {XMaterial.MOSSY_STONE_BRICKS,    XMaterial.POLISHED_ANDESITE}
            },
            {
                    {XMaterial.GRANITE,         XMaterial.JUNGLE_PLANKS},
                    {XMaterial.END_STONE,       XMaterial.SAND},
                    {XMaterial.DARK_OAK_PLANKS, XMaterial.SOUL_SOIL},
                    {XMaterial.PINK_TERRACOTTA, XMaterial.RED_CONCRETE_POWDER},
                    {XMaterial.BLACKSTONE,      XMaterial.BLACK_WOOL},
                    {XMaterial.BLUE_CONCRETE,   XMaterial.BLUE_CONCRETE_POWDER},
                    {XMaterial.CYAN_WOOL,       XMaterial.WARPED_WART_BLOCK},
                    {XMaterial.GRAVEL,          XMaterial.LIGHT_GRAY_WOOL}
            },
            {
                    {XMaterial.ORANGE_CONCRETE,     XMaterial.ORANGE_WOOL},
                    {XMaterial.CRIMSON_PLANKS,      XMaterial.PURPLE_TERRACOTTA},
                    {XMaterial.DEEPSLATE_TILES,     XMaterial.POLISHED_DEEPSLATE},
                    {XMaterial.LIGHT_GRAY_CONCRETE, XMaterial.MOSSY_COBBLESTONE},
                    {XMaterial.BRICKS,              XMaterial.TERRACOTTA},
                    {XMaterial.OXIDIZED_COPPER,     XMaterial.PRISMARINE},
                    {XMaterial.MUD,                 XMaterial.POLISHED_BLACKSTONE_BRICKS},
                    {XMaterial.ANDESITE,            XMaterial.STONE_BRICKS}
            },
            {
                    {XMaterial.SMOOTH_QUARTZ,        XMaterial.WHITE_WOOL},
                    {XMaterial.BLUE_CONCRETE_POWDER, XMaterial.BLUE_WOOL},
                    {XMaterial.COARSE_DIRT,          XMaterial.SPRUCE_PLANKS},
                    {XMaterial.POLISHED_GRANITE,     XMaterial.ROOTED_DIRT},
                    {XMaterial.QUARTZ_BRICKS,        XMaterial.WHITE_CONCRETE_POWDER},
                    {XMaterial.RED_CONCRETE,         XMaterial.RED_WOOL},
                    {XMaterial.BLUE_ICE,             XMaterial.PACKED_ICE},
                    {XMaterial.DEEPSLATE,            XMaterial.DEEPSLATE_BRICKS}
            },
            {
                    {XMaterial.PINK_CONCRETE_POWDER,       XMaterial.PINK_WOOL},
                    {XMaterial.POLISHED_GRANITE,           XMaterial.RAW_COPPER_BLOCK},
                    {XMaterial.PURPLE_CONCRETE_POWDER,     XMaterial.PURPLE_WOOL},
                    {XMaterial.DIRT,                       XMaterial.PACKED_MUD},
                    {XMaterial.PRISMARINE,                 XMaterial.PRISMARINE_BRICKS},
                    {XMaterial.COBBLESTONE,                XMaterial.STONE_BRICKS},
                    {XMaterial.BLACK_CONCRETE,             XMaterial.BLACK_WOOL},
                    {XMaterial.LIGHT_BLUE_CONCRETE_POWDER, XMaterial.LIGHT_BLUE_WOOL}
            },
            {
                    {XMaterial.GREEN_CONCRETE_POWDER, XMaterial.LIME_TERRACOTTA},
                    {XMaterial.CALCITE,               XMaterial.IRON_BLOCK},
                    {XMaterial.COBBLED_DEEPSLATE,     XMaterial.GRAY_CONCRETE_POWDER},
                    {XMaterial.BLACKSTONE,            XMaterial.POLISHED_BLACKSTONE_BRICKS},
                    {XMaterial.BROWN_CONCRETE_POWDER, XMaterial.COARSE_DIRT},
                    {XMaterial.COBBLESTONE,           XMaterial.STONE},
                    {XMaterial.WHITE_CONCRETE_POWDER, XMaterial.WHITE_WOOL},
                    {XMaterial.SOUL_SAND,             XMaterial.SOUL_SOIL}
            },
            {
                    {XMaterial.CUT_SANDSTONE,    XMaterial.SMOOTH_SANDSTONE},
                    {XMaterial.ANDESITE,         XMaterial.POLISHED_ANDESITE},
                    {XMaterial.RED_SAND,         XMaterial.RED_SANDSTONE},
                    {XMaterial.DIORITE,          XMaterial.POLISHED_DIORITE},
                    {XMaterial.MUD_BRICKS,       XMaterial.PACKED_MUD},
                    {XMaterial.QUARTZ_BRICKS,    XMaterial.SMOOTH_QUARTZ},
                    {XMaterial.SAND,             XMaterial.SANDSTONE},
                    {XMaterial.DEEPSLATE_BRICKS, XMaterial.POLISHED_DEEPSLATE}
            }
    };

    private static final int MAX_DIFFICULTY = DIFFICULTY_PAIRS.length;
    private static final int TOTAL_SLOTS    = 54;

    private final int    maxRounds;
    private final int    baseTime;
    private final int    startDifficulty;

    private final Random random = new Random();

    private int     round           = 0;
    private int     currentDifficulty;
    private int     roundsOnDifficulty = 0;
    private int     oddSlot         = -1;
    private boolean waitingForClick = false;

    private XMaterial currentMain = null;
    private XMaterial currentOdd  = null;
    private XMaterial[] lastPair  = null;

    private BukkitTask countdownTask = null;

    public OddOneOutMinigameMenu(Player player, Main plugin, Consumer<Boolean> onFinish) {
        super(player, plugin, onFinish);
        this.maxRounds      = plugin.getConfig().getInt("hint.minigames.oddoneout.rounds",     10);
        this.baseTime       = plugin.getConfig().getInt("hint.minigames.oddoneout.base-time",  200);
        int raw             = plugin.getConfig().getInt("hint.minigames.oddoneout.difficulty",   4);
        this.startDifficulty    = Math.max(1, Math.min(MAX_DIFFICULTY, raw));
        this.currentDifficulty  = this.startDifficulty;
    }

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

    private void startRound() {
        if (finished) return;
        round++;

        if (round > 1) updateDifficulty();

        waitingForClick = false;

        XMaterial[] pair = pickPair();
        boolean swap = random.nextBoolean();
        currentMain = swap ? pair[1] : pair[0];
        currentOdd  = swap ? pair[0] : pair[1];

        oddSlot = random.nextInt(TOTAL_SLOTS);

        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                "minigame.oddoneout.find", true,
                "%round%",      String.valueOf(round),
                "%max%",        String.valueOf(maxRounds),
                "%difficulty%", String.valueOf(currentDifficulty)));

        waitingForClick = true;
        startCountdown(baseTime);
    }

    private XMaterial[] pickPair() {
        for (int level = currentDifficulty; level >= 1; level--) {
            List<XMaterial[]> usable = new ArrayList<>();
            for (XMaterial[] pair : DIFFICULTY_PAIRS[level - 1]) {
                if (pair[0].isSupported() && pair[1].isSupported()) usable.add(pair);
            }
            if (usable.isEmpty()) continue;
            if (usable.size() > 1 && lastPair != null) usable.remove(lastPair);
            XMaterial[] chosen = usable.get(random.nextInt(usable.size()));
            lastPair = chosen;
            return chosen;
        }
        return new XMaterial[]{XMaterial.RED_WOOL, XMaterial.BLUE_WOOL};
    }

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