package de.theredend2000.advancedhunt.menu.minigame;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class OddOneOutMinigameMenu extends MinigameMenu {

    private static final XMaterial[] MATERIALS = {
            XMaterial.RED_WOOL,     XMaterial.ORANGE_WOOL,  XMaterial.YELLOW_WOOL,
            XMaterial.LIME_WOOL,    XMaterial.BLUE_WOOL,    XMaterial.PURPLE_WOOL,
            XMaterial.CYAN_WOOL,    XMaterial.PINK_WOOL,    XMaterial.MAGENTA_WOOL,
            XMaterial.WHITE_WOOL,   XMaterial.BLACK_WOOL,   XMaterial.BROWN_WOOL,
            XMaterial.LIGHT_BLUE_WOOL, XMaterial.GREEN_WOOL
    };

    private static final int TOTAL_SLOTS = 54;

    private final int maxRounds;
    private final int baseTime;
    private final int timeDecrease;
    private final Random random = new Random();

    private int round = 0;
    private int oddSlot = -1;
    private boolean waitingForClick = false;

    public OddOneOutMinigameMenu(Player player, Main plugin, Consumer<Boolean> onFinish) {
        super(player, plugin, onFinish);
        this.maxRounds    = plugin.getConfig().getInt("hint.minigames.oddoneout.rounds",        4);
        this.baseTime     = plugin.getConfig().getInt("hint.minigames.oddoneout.base-time",     100);
        this.timeDecrease = plugin.getConfig().getInt("hint.minigames.oddoneout.time-decrease", 15);
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

    private void startRound() {
        if (finished) return;
        round++;
        waitingForClick = false;

        List<XMaterial> pool = new ArrayList<>();
        for (XMaterial m : MATERIALS) pool.add(m);
        Collections.shuffle(pool);
        XMaterial main = pool.get(0);
        XMaterial odd  = pool.get(1);

        ItemStack mainItem = new ItemBuilder(main).hideTooltip(true).build();
        for (int i = 0; i < TOTAL_SLOTS; i++) updateSlot(i, mainItem);

        oddSlot = random.nextInt(TOTAL_SLOTS);
        updateSlot(oddSlot, new ItemBuilder(odd)
                .setDisplayName(plugin.getMessageManager().getMessage(
                        "gui.minigame.oddoneout.odd.name", false))
                .hideTooltip(false)
                .build());

        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                "minigame.oddoneout.find", false,
                "%round%", String.valueOf(round),
                "%max%",   String.valueOf(maxRounds)));

        waitingForClick = true;

        long timeLimit = Math.max(20, baseTime - (long) (round - 1) * timeDecrease);
        scheduleTask(() -> {
            if (!waitingForClick || finished) return;
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                    "minigame.oddoneout.timeout", false));
            playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                    XSound.ENTITY_VILLAGER_NO.get(), 1, 1f);
            finish(false);
        }, timeLimit);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (!waitingForClick || finished) return;

        int slot = event.getSlot();

        if (slot == oddSlot) {
            waitingForClick = false;
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
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                    "minigame.oddoneout.wrong", false));
            playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                    XSound.ENTITY_VILLAGER_NO.get(), 1, 1f);
            finish(false);
        }
    }
}
