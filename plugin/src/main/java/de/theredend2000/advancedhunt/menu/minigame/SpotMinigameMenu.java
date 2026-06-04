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
import java.util.function.Consumer;

public class SpotMinigameMenu extends MinigameMenu {

    private static final XMaterial[] ITEM_TYPES = {
            XMaterial.RED_WOOL,    XMaterial.ORANGE_WOOL, XMaterial.YELLOW_WOOL,
            XMaterial.LIME_WOOL,   XMaterial.BLUE_WOOL,   XMaterial.PURPLE_WOOL,
            XMaterial.CYAN_WOOL,   XMaterial.PINK_WOOL,   XMaterial.MAGENTA_WOOL,
            XMaterial.WHITE_WOOL
    };

    private static final int TOTAL_SLOTS = 45;

    private final int itemCount;
    private final int displayTime;

    private final List<Integer> hiddenSlots = new ArrayList<>();
    private final List<Integer> foundSlots  = new ArrayList<>();

    private boolean memorisePhase = true;

    public SpotMinigameMenu(Player player, Main plugin, Consumer<Boolean> onFinish) {
        super(player, plugin, onFinish);
        this.itemCount   = plugin.getConfig().getInt("hint.minigames.spot.item-count",   5);
        this.displayTime = plugin.getConfig().getInt("hint.minigames.spot.display-time", 60);
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.minigame.spot.title", false);
    }

    @Override
    public int getSlots() {
        return TOTAL_SLOTS;
    }

    @Override
    public void setMenuItems() {
        fillBackground(new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE).hideTooltip(true).build());
        startMemorisePhase();
    }

    private void startMemorisePhase() {
        memorisePhase = true;
        hiddenSlots.clear();
        foundSlots.clear();

        List<Integer> allSlots = new ArrayList<>();
        for (int i = 0; i < TOTAL_SLOTS; i++) allSlots.add(i);
        Collections.shuffle(allSlots);

        List<XMaterial> pool = new ArrayList<>();
        for (XMaterial m : ITEM_TYPES) pool.add(m);
        Collections.shuffle(pool);

        int count = Math.min(itemCount, TOTAL_SLOTS);
        for (int i = 0; i < count; i++) {
            int slot = allSlots.get(i);
            hiddenSlots.add(slot);
            XMaterial mat = pool.get(i % pool.size());
            updateSlot(slot, new ItemBuilder(mat)
                    .setDisplayName(plugin.getMessageManager().getMessage(
                            "gui.minigame.spot.item.name", false))
                    .build());
        }

        playerMenuUtility.sendMessage(
                plugin.getMessageManager().getMessage("minigame.spot.memorise", false));

        scheduleTask(this::startGuessPhase, displayTime);
    }

    private void startGuessPhase() {
        memorisePhase = false;

        ItemStack glass = new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE)
                .hideTooltip(true).build();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            updateSlot(i, glass);
        }

        playerMenuUtility.sendMessage(
                plugin.getMessageManager().getMessage("minigame.spot.find", false));
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (memorisePhase || finished) return;

        int slot = event.getSlot();
        if (slot < 0 || slot >= TOTAL_SLOTS) return;

        if (foundSlots.contains(slot)) return;

        if (hiddenSlots.contains(slot)) {
            foundSlots.add(slot);
            int idx = hiddenSlots.indexOf(slot);
            XMaterial mat = ITEM_TYPES[idx % ITEM_TYPES.length];
            updateSlot(slot, new ItemBuilder(mat)
                    .setDisplayName(plugin.getMessageManager().getMessage(
                            "gui.minigame.spot.item.found", false))
                    .build());
            playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                    XSound.BLOCK_NOTE_BLOCK_PLING.get(), 1, 2f);

            if (foundSlots.size() >= hiddenSlots.size()) {
                playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                        XSound.ENTITY_PLAYER_LEVELUP.get(), 1, 1f);
                scheduleTask(() -> finish(true), 10);
            }
        } else {
            playerMenuUtility.sendMessage(
                    plugin.getMessageManager().getMessage("minigame.spot.wrong", false));
            playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                    XSound.ENTITY_VILLAGER_NO.get(), 1, 1f);
            finish(false);
        }
    }
}
