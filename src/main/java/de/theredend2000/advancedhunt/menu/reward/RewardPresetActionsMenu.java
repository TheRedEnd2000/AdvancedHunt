package de.theredend2000.advancedhunt.menu.reward;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.menu.common.ConfirmationMenu;
import de.theredend2000.advancedhunt.model.*;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class RewardPresetActionsMenu extends Menu {

    private final RewardPreset preset;
    private final Collection collectionContext;

    public RewardPresetActionsMenu(Player playerMenuUtility, Main plugin, RewardPreset preset, Collection collectionContext) {
        super(playerMenuUtility, plugin);
        this.preset = preset;
        this.collectionContext = collectionContext;
    }

    @Override
    public String getMenuName() {
        return plugin.getMessageManager().getMessage("gui.presets.actions.title", false,
                "%name%", preset.getName());
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        // handled by buttons
    }

    @Override
    public void setMenuItems() {
        fillBorders(FILLER_GLASS);

        addButton(10, new ItemBuilder(Material.NAME_TAG)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.presets.actions.rename.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.presets.actions.rename.lore", false).toArray(new String[0]))
                .build(), e -> handleRename());

        addButton(11, buildCollectionActionItem(
                "gui.presets.actions.load_all",
                Material.ANVIL,
                isTreasurePreset() && hasCollectionContext()
        ), e -> handleLoadAll());

        addButton(12, buildCollectionActionItem(
                "gui.presets.actions.set_default",
                Material.WRITABLE_BOOK,
                isTreasurePreset() && hasCollectionContext()
        ), e -> handleSetDefault());

        addButton(14, new ItemBuilder(Material.WRITTEN_BOOK)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.presets.actions.edit_rewards.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.presets.actions.edit_rewards.lore", false).toArray(new String[0]))
                .build(), e -> {
            new RewardsMenu(playerMenuUtility, plugin, new PresetRewardHolder(plugin, preset), true)
                    .setTitleKey("gui.rewards.preset_title")
                    .setPreviousMenu(this)
                    .open();
        });

        addButton(16, new ItemBuilder(Material.TNT)
                .setDisplayName(plugin.getMessageManager().getMessage("gui.presets.actions.delete.name", false))
                .setLore(plugin.getMessageManager().getMessageList("gui.presets.actions.delete.lore", false).toArray(new String[0]))
                .build(), e -> handleDelete());
    }

    private ItemStack buildCollectionActionItem(String baseKey, Material material, boolean enabled) {
        if (enabled) {
            return new ItemBuilder(material)
                    .setDisplayName(plugin.getMessageManager().getMessage(baseKey + ".name", false))
                    .setLore(plugin.getMessageManager().getMessageList(baseKey + ".lore", false).toArray(new String[0]))
                    .build();
        }

        return new ItemBuilder(Material.BARRIER)
                .setDisplayName(plugin.getMessageManager().getMessage(baseKey + ".name", false))
                .setLore(plugin.getMessageManager().getMessageList(baseKey + ".lore_disabled", false).toArray(new String[0]))
                .build();
    }

    private void handleRename() {
        if (!playerMenuUtility.hasPermission("advancedhunt.admin")) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
            return;
        }

        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.prompt_rename", "%name%", preset.getName()));
        plugin.getChatInputListener().requestInput(playerMenuUtility, input -> {
            String trimmed = input == null ? null : input.trim();
            if (trimmed == null || trimmed.isEmpty()) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.preset.invalid_name"));
                Bukkit.getScheduler().runTask(plugin, this::open);
                return;
            }

            if (isNameTaken(trimmed)) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.preset.duplicate_name"));
                Bukkit.getScheduler().runTask(plugin, this::open);
                return;
            }

            preset.setName(trimmed);
            plugin.getRewardPresetManager().savePreset(preset).whenComplete((v, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (ex != null) {
                    playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.preset.save_failed_" + presetContextKey()));
                    open();
                    return;
                }
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.renamed",
                        "%name%", preset.getName()));
                openPreviousMenu();
            }));
        });
    }

    private void handleSetDefault() {
        if (!isTreasurePreset() || !hasCollectionContext()) {
            return;
        }
        if (!playerMenuUtility.hasPermission("advancedhunt.admin")) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
            return;
        }

        collectionContext.setDefaultTreasureRewardPresetId(preset.getId());
        plugin.getCollectionManager().saveCollection(collectionContext).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.set_default",
                    "%collection%", collectionContext.getName(),
                    "%name%", preset.getName()));
            openPreviousMenu();
        }));
    }

    private void handleLoadAll() {
        if (!isTreasurePreset() || !hasCollectionContext()) {
            return;
        }
        if (!playerMenuUtility.hasPermission("advancedhunt.admin")) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
            return;
        }

        new ConfirmationMenu(playerMenuUtility, plugin,
                plugin.getMessageManager().getMessage("gui.presets.actions.load_all.confirm_title", false,
                        "%collection%", collectionContext.getName(),
                        "%name%", preset.getName()),
                confirmEvent -> runBulkOverride(),
                cancelEvent -> open()
        ).setPreviousMenu(this).open();
    }

    private void runBulkOverride() {
        List<TreasureCore> cores = plugin.getTreasureManager().getTreasureCoresInCollection(collectionContext.getId());
        if (cores.isEmpty()) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.override.none"));
            open();
            return;
        }

        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.override.started",
                "%count%", String.valueOf(cores.size()),
                "%name%", preset.getName()));

        final int total = cores.size();
        final AtomicInteger completed = new AtomicInteger(0);
        final AtomicInteger nextPercentToReport = new AtomicInteger(10);

        final int batchSize = 50;
        final List<de.theredend2000.advancedhunt.model.Reward> rewardSnapshot =
                preset.getRewards() == null ? java.util.Collections.emptyList() : java.util.List.copyOf(preset.getRewards());

        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (int i = 0; i < cores.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, cores.size());
            chain = chain.thenCompose(v -> {
                List<CompletableFuture<Void>> batch = new ArrayList<>(end - start);
                for (int j = start; j < end; j++) {
                    batch.add(plugin.getTreasureManager().updateTreasureRewards(cores.get(j).getId(), rewardSnapshot));
                }
                return CompletableFuture.allOf(batch.toArray(new CompletableFuture[0])).thenRun(() -> {
                    int done = completed.addAndGet(end - start);
                    if (total <= 0) return;
                    int percent = (int) Math.floor((done * 100.0) / total);

                    int next = nextPercentToReport.get();
                    if (percent < next) return;

                    while (percent >= next && next < 100) {
                        if (!nextPercentToReport.compareAndSet(next, next + 10)) {
                            next = nextPercentToReport.get();
                            continue;
                        }
                        int percentToSend = next;
                        Bukkit.getScheduler().runTask(plugin, () -> playerMenuUtility.sendMessage(
                                plugin.getMessageManager().getMessage("feedback.preset.override.progress",
                                        "%percent%", String.valueOf(percentToSend),
                                        "%current%", String.valueOf(done),
                                        "%total%", String.valueOf(total))
                        ));
                        next += 10;
                    }
                });
            });
        }

        chain.whenComplete((v, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (ex != null) {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.override.failed"));
            } else {
                playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.override.done",
                        "%count%", String.valueOf(cores.size()),
                        "%name%", preset.getName()));
            }
            openPreviousMenu();
        }));
    }

    private void handleDelete() {
        if (!playerMenuUtility.hasPermission("advancedhunt.admin")) {
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
            return;
        }

        new ConfirmationMenu(playerMenuUtility, plugin,
                plugin.getMessageManager().getMessage("gui.presets.actions.delete.confirm_title", false,
                        "%name%", preset.getName()),
                confirmEvent -> plugin.getRewardPresetManager().deletePreset(preset.getType(), preset.getId()).thenRun(() ->
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.preset.deleted",
                                    "%name%", preset.getName()));
                            openPreviousMenu();
                        })
                ),
                cancelEvent -> open()
        ).setPreviousMenu(this).open();
    }

    private boolean hasCollectionContext() {
        return collectionContext != null;
    }

    private boolean isTreasurePreset() {
        return preset.getType() == RewardPresetType.TREASURE;
    }

    private String presetContextKey() {
        return preset.getType() == RewardPresetType.COLLECTION ? "collection" : "treasure";
    }

    private boolean isNameTaken(String name) {
        String normalized = normalizeName(name);
        UUID currentId = preset.getId();
        for (RewardPreset p : plugin.getRewardPresetManager().getPresets(preset.getType())) {
            if (p.getId().equals(currentId)) continue;
            if (normalizeName(p.getName()).equals(normalized)) return true;
        }
        return false;
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
