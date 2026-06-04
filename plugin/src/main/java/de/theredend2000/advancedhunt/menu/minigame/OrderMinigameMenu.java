package de.theredend2000.advancedhunt.menu.minigame;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class OrderMinigameMenu extends MinigameMenu {

    private static final XMaterial[] ITEM_TYPES = {
            XMaterial.RED_WOOL,    XMaterial.ORANGE_WOOL, XMaterial.YELLOW_WOOL,
            XMaterial.LIME_WOOL,   XMaterial.BLUE_WOOL,   XMaterial.PURPLE_WOOL,
            XMaterial.CYAN_WOOL,   XMaterial.PINK_WOOL
    };

    private static final int TOTAL_SLOTS    = 54;

    private final int maxRounds;
    private final int sequenceLength;
    private final int displayTime;

    private int round = 0;

    private final List<Integer> correctSequence = new ArrayList<>();
    private final List<Integer> playerAnswer    = new ArrayList<>();

    private final List<Integer> answerSlots  = new ArrayList<>();
    private final List<Integer> paletteSlots = new ArrayList<>();
    private final List<Integer> paletteTypes = new ArrayList<>();

    private boolean memorisePhase = true;
    private boolean inputPhase    = false;

    public OrderMinigameMenu(Player player, Main plugin, Consumer<Boolean> onFinish) {
        super(player, plugin, onFinish);
        this.maxRounds      = plugin.getConfig().getInt("hint.minigames.order.rounds",          3);
        this.sequenceLength = plugin.getConfig().getInt("hint.minigames.order.sequence-length", 4);
        this.displayTime    = plugin.getConfig().getInt("hint.minigames.order.display-time",    60);
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.minigame.order.title", false);
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
        memorisePhase = true;
        inputPhase    = false;

        correctSequence.clear();
        playerAnswer.clear();
        answerSlots.clear();
        paletteSlots.clear();
        paletteTypes.clear();

        fillBackground(new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE).hideTooltip(true).build());

        int len = Math.min(sequenceLength, ITEM_TYPES.length);
        int startCol = (9 - len) / 2;
        for (int i = 0; i < len; i++) {
            answerSlots.add(9 + startCol + i);
        }

        List<Integer> typePool = new ArrayList<>();
        for (int i = 0; i < ITEM_TYPES.length; i++) typePool.add(i);
        Collections.shuffle(typePool);
        List<Integer> usedTypes = typePool.subList(0, Math.min(len, typePool.size()));

        for (int i = 0; i < len; i++) {
            correctSequence.add(usedTypes.get((int)(Math.random() * usedTypes.size())));
        }

        for (int i = 0; i < len; i++) {
            int typeIdx = correctSequence.get(i);
            updateSlot(answerSlots.get(i), new ItemBuilder(ITEM_TYPES[typeIdx])
                    .setDisplayName(plugin.getMessageManager().getMessage(
                            "gui.minigame.order.sequence.name", false,
                            "%pos%", String.valueOf(i + 1)))
                    .build());
        }

        List<Integer> palettePool = new ArrayList<>(correctSequence);
        Collections.shuffle(palettePool);
        int paletteStartCol = (9 - palettePool.size()) / 2;
        for (int i = 0; i < palettePool.size(); i++) {
            int slot    = 36 + paletteStartCol + i;
            int typeIdx = palettePool.get(i);
            paletteSlots.add(slot);
            paletteTypes.add(typeIdx);
            updateSlot(slot, new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE).hideTooltip(true).build());
        }

        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                "minigame.order.memorise", false,
                "%round%", String.valueOf(round),
                "%max%",   String.valueOf(maxRounds)));

        scheduleTask(this::startInputPhase, displayTime);
    }

    private void startInputPhase() {
        if (finished) return;
        memorisePhase = false;
        inputPhase    = true;

        for (int slot : answerSlots) {
            updateSlot(slot, new ItemBuilder(XMaterial.WHITE_STAINED_GLASS_PANE)
                    .setDisplayName(plugin.getMessageManager().getMessage(
                            "gui.minigame.order.empty.name", false))
                    .build());
        }

        for (int i = 0; i < paletteSlots.size(); i++) {
            int typeIdx = paletteTypes.get(i);
            updateSlot(paletteSlots.get(i), new ItemBuilder(ITEM_TYPES[typeIdx])
                    .setDisplayName(plugin.getMessageManager().getMessage(
                            "gui.minigame.order.palette.name", false))
                    .build());
        }

        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                "minigame.order.recreate", false));
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (!inputPhase || finished) return;

        int slot = event.getSlot();

        int paletteIndex = paletteSlots.indexOf(slot);
        if (paletteIndex < 0) return;

        int typeIdx = paletteTypes.get(paletteIndex);
        int pos = playerAnswer.size();

        if (pos >= correctSequence.size()) return;

        updateSlot(answerSlots.get(pos), new ItemBuilder(ITEM_TYPES[typeIdx])
                .setDisplayName(plugin.getMessageManager().getMessage(
                        "gui.minigame.order.placed.name", false,
                        "%pos%", String.valueOf(pos + 1)))
                .build());

        playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                XSound.BLOCK_NOTE_BLOCK_PLING.get(), 1, 1 + pos * 0.1f);

        playerAnswer.add(typeIdx);
        updateSlot(paletteSlots.get(paletteIndex),
                new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE).hideTooltip(true).build());

        if (typeIdx != correctSequence.get(pos)) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                    "minigame.order.wrong", false));
            playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                    XSound.ENTITY_VILLAGER_NO.get(), 1, 1f);
            finish(false);
            return;
        }

        if (playerAnswer.size() >= correctSequence.size()) {
            playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                    XSound.ENTITY_EXPERIENCE_ORB_PICKUP.get(), 1, 1.5f);

            if (round >= maxRounds) {
                playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                        XSound.ENTITY_PLAYER_LEVELUP.get(), 1, 1f);
                scheduleTask(() -> finish(true), 20);
            } else {
                scheduleTask(this::startRound, 40);
            }
        }
    }
}
