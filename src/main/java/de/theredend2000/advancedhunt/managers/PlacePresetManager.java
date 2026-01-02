package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.model.PlacePreset;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlacePresetManager {

    private final Main plugin;
    private final DataRepository repository;

    private final Map<UUID, PlacePreset> presets = new ConcurrentHashMap<>();

    public PlacePresetManager(Main plugin, DataRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public CompletableFuture<Void> reloadPresets() {
        return repository.loadPlacePresets().thenAccept(list -> {
            presets.clear();
            if (list == null) {
                return;
            }
            for (PlacePreset preset : list) {
                if (preset != null) {
                    presets.put(preset.getId(), preset);
                }
            }
        });
    }

    public Optional<PlacePreset> getPreset(UUID id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(presets.get(id));
    }

    public List<PlacePreset> getAllPresets() {
        return new ArrayList<>(presets.values());
    }

    public List<String> getGroups() {
        TreeSet<String> groups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (PlacePreset preset : presets.values()) {
            if (preset.getGroup() != null && !preset.getGroup().isBlank()) {
                groups.add(preset.getGroup());
            }
        }
        return new ArrayList<>(groups);
    }

    public List<PlacePreset> getPresetsInGroup(String group) {
        if (group == null) {
            return List.of();
        }
        List<PlacePreset> list = new ArrayList<>();
        for (PlacePreset preset : presets.values()) {
            if (preset.getGroup() != null && preset.getGroup().equalsIgnoreCase(group)) {
                list.add(preset);
            }
        }
        list.sort(Comparator.comparing(PlacePreset::getName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public boolean hasPresetNameInGroup(String group, String name) {
        if (group == null || name == null) return false;
        String g = normalize(group);
        String n = normalize(name);
        for (PlacePreset preset : presets.values()) {
            if (preset.getGroup() == null || preset.getName() == null) continue;
            if (normalize(preset.getGroup()).equals(g) && normalize(preset.getName()).equals(n)) {
                return true;
            }
        }
        return false;
    }

    public CompletableFuture<Boolean> createPreset(String group, String name, String itemData) {
        if (group == null || group.isBlank() || name == null || name.isBlank() || itemData == null || itemData.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }
        if (hasPresetNameInGroup(group, name)) {
            return CompletableFuture.completedFuture(false);
        }

        PlacePreset preset = new PlacePreset(UUID.randomUUID(), group.trim(), name.trim(), itemData);
        return repository.savePlacePreset(preset)
                .thenCompose(v -> reloadPresets())
                .thenApply(v -> true)
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to create place preset: " + ex.getMessage());
                    return false;
                });
    }

    public CompletableFuture<Void> savePreset(PlacePreset preset) {
        if (preset == null) {
            return CompletableFuture.completedFuture(null);
        }
        return repository.savePlacePreset(preset).thenCompose(v -> reloadPresets());
    }

    public CompletableFuture<Void> deletePreset(UUID presetId) {
        if (presetId == null) {
            return CompletableFuture.completedFuture(null);
        }
        return repository.deletePlacePreset(presetId).thenCompose(v -> reloadPresets());
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
