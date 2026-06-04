package de.theredend2000.advancedhunt.menu.minigame;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SliderPuzzleMinigameMenu extends MinigameMenu {

    private static final int[] GRID_INV_SLOTS = {12, 13, 14, 21, 22, 23, 30, 31, 32};

    private static final int GRID_SIZE = 3;

    private static final int[] SOLVED = {1, 2, 3, 4, 5, 6, 7, 8, 0};

    private static final XMaterial[] TILE_MATERIALS = {
            XMaterial.RED_WOOL,    XMaterial.ORANGE_WOOL, XMaterial.YELLOW_WOOL,
            XMaterial.LIME_WOOL,   XMaterial.BLUE_WOOL,   XMaterial.PURPLE_WOOL,
            XMaterial.CYAN_WOOL,   XMaterial.PINK_WOOL
    };

    private final int shuffles;
    private final int[] board = new int[9];
    private int emptyPos;

    public SliderPuzzleMinigameMenu(Player player, Main plugin, Consumer<Boolean> onFinish) {
        super(player, plugin, onFinish);
        this.shuffles = plugin.getConfig().getInt("hint.minigames.sliderpuzzle.shuffles", 50);
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.minigame.sliderpuzzle.title", false);
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public void setMenuItems() {
        fillBackground(new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE).hideTooltip(true).build());

        for (int i = 0; i < 9; i++) board[i] = SOLVED[i];
        emptyPos = 8;

        shuffleBoard();
        renderBoard();

        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage(
                "minigame.sliderpuzzle.start", false));
    }

    private void shuffleBoard() {
        java.util.Random rng = new java.util.Random();
        int lastMoved = -1;
        for (int s = 0; s < shuffles; s++) {
            List<Integer> neighbours = getNeighbours(emptyPos);
            if (neighbours.size() > 1) {
                final int prevEmpty = lastMoved;
                neighbours.removeIf(n -> n == prevEmpty);
            }
            int pick = neighbours.get(rng.nextInt(neighbours.size()));
            lastMoved = emptyPos;
            swap(emptyPos, pick);
            emptyPos = pick;
        }
    }

    private void renderBoard() {
        for (int pos = 0; pos < 9; pos++) {
            int invSlot = GRID_INV_SLOTS[pos];
            int tile    = board[pos];
            if (tile == 0) {
                updateSlot(invSlot, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE)
                        .setDisplayName(plugin.getMessageManager().getMessage(
                                "gui.minigame.sliderpuzzle.empty.name", false))
                        .build());
            } else {
                updateSlot(invSlot, new ItemBuilder(TILE_MATERIALS[tile - 1])
                        .setDisplayName(plugin.getMessageManager().getMessage(
                                "gui.minigame.sliderpuzzle.tile.name", false,
                                "%number%", String.valueOf(tile)))
                        .build());
            }
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (finished) return;

        int invSlot = event.getSlot();
        int gridPos = invSlotToGridPos(invSlot);
        if (gridPos < 0) return;

        if (!getNeighbours(emptyPos).contains(gridPos)) {
            playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                    XSound.BLOCK_STONE_PLACE.get(), 0.5f, 0.5f);
            return;
        }

        swap(emptyPos, gridPos);
        int prevEmpty = emptyPos;
        emptyPos = gridPos;

        renderTile(prevEmpty);
        renderTile(emptyPos);

        playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                XSound.BLOCK_NOTE_BLOCK_HAT.get(), 1f, 1.5f);

        if (isSolved()) {
            playerMenuUtility.playSound(playerMenuUtility.getLocation(),
                    XSound.ENTITY_PLAYER_LEVELUP.get(), 1f, 1f);
            scheduleTask(() -> finish(true), 20);
        }
    }

    private void renderTile(int pos) {
        int invSlot = GRID_INV_SLOTS[pos];
        int tile    = board[pos];
        if (tile == 0) {
            updateSlot(invSlot, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE)
                    .setDisplayName(plugin.getMessageManager().getMessage(
                            "gui.minigame.sliderpuzzle.empty.name", false))
                    .build());
        } else {
            updateSlot(invSlot, new ItemBuilder(TILE_MATERIALS[tile - 1])
                    .setDisplayName(plugin.getMessageManager().getMessage(
                            "gui.minigame.sliderpuzzle.tile.name", false,
                            "%number%", String.valueOf(tile)))
                    .build());
        }
    }

    private void swap(int a, int b) {
        int tmp = board[a];
        board[a] = board[b];
        board[b] = tmp;
    }

    private boolean isSolved() {
        for (int i = 0; i < 9; i++) {
            if (board[i] != SOLVED[i]) return false;
        }
        return true;
    }

    private List<Integer> getNeighbours(int pos) {
        List<Integer> result = new ArrayList<>();
        int row = pos / GRID_SIZE;
        int col = pos % GRID_SIZE;
        if (row > 0) result.add(pos - GRID_SIZE); // up
        if (row < GRID_SIZE - 1) result.add(pos + GRID_SIZE); // down
        if (col > 0) result.add(pos - 1); // left
        if (col < GRID_SIZE - 1) result.add(pos + 1); // right
        return result;
    }

    private int invSlotToGridPos(int invSlot) {
        for (int i = 0; i < GRID_INV_SLOTS.length; i++) {
            if (GRID_INV_SLOTS[i] == invSlot) return i;
        }
        return -1;
    }
}
