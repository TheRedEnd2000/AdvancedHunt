package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.model.Reward;
import de.theredend2000.advancedhunt.model.RewardPreset;
import de.theredend2000.advancedhunt.model.RewardPresetType;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RewardPresetManager {

    private final Main plugin;
    private final DataRepository repository;

    private final Map<UUID, RewardPreset> treasurePresets = new ConcurrentHashMap<>();
    private final Map<UUID, RewardPreset> collectionPresets = new ConcurrentHashMap<>();

    public RewardPresetManager(Main plugin, DataRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public CompletableFuture<Void> reloadPresets() {
        CompletableFuture<List<RewardPreset>> treasureFuture = repository.loadRewardPresets(RewardPresetType.TREASURE);
        CompletableFuture<List<RewardPreset>> collectionFuture = repository.loadRewardPresets(RewardPresetType.COLLECTION);

        return treasureFuture.thenCombine(collectionFuture, (treasure, collection) -> {
            treasurePresets.clear();
            collectionPresets.clear();

            if (treasure != null) {
                for (RewardPreset preset : treasure) {
                    treasurePresets.put(preset.getId(), preset);
                }
            }
            if (collection != null) {
                for (RewardPreset preset : collection) {
                    collectionPresets.put(preset.getId(), preset);
                }
            }
            return null;
        });
    }

    public List<RewardPreset> getPresets(RewardPresetType type) {
        return new ArrayList<>(getMap(type).values());
    }

    public Optional<RewardPreset> getPreset(RewardPresetType type, UUID id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(getMap(type).get(id));
    }

    public boolean hasPresetName(RewardPresetType type, String name) {
        if (name == null) return false;
        String normalized = normalizeName(name);
        for (RewardPreset preset : getMap(type).values()) {
            if (normalizeName(preset.getName()).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public CompletableFuture<Boolean> createPreset(RewardPresetType type, String name, List<Reward> rewards) {
        if (name == null || name.trim().isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        if (hasPresetName(type, name)) {
            return CompletableFuture.completedFuture(false);
        }

        RewardPreset preset = new RewardPreset(UUID.randomUUID(), type, name.trim(), rewards);
        return repository.saveRewardPreset(preset).thenCompose(v -> reloadPresets()).thenApply(v -> true).exceptionally(ex -> {
            plugin.getLogger().warning("Failed to create preset: " + ex.getMessage());
            return false;
        });
    }

    public CompletableFuture<Void> savePreset(RewardPreset preset) {
        if (preset == null) {
            return CompletableFuture.completedFuture(null);
        }
        return repository.saveRewardPreset(preset).thenCompose(v -> reloadPresets());
    }

    public CompletableFuture<Void> deletePreset(RewardPresetType type, UUID presetId) {
        if (presetId == null) {
            return CompletableFuture.completedFuture(null);
        }
        return repository.deleteRewardPreset(type, presetId).thenCompose(v -> reloadPresets());
    }

    private Map<UUID, RewardPreset> getMap(RewardPresetType type) {
        return type == RewardPresetType.COLLECTION ? collectionPresets : treasurePresets;
    }

    private static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
