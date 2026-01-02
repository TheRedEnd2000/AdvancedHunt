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
    // normalized group name -> display group name
    private final Map<String, String> groups = new ConcurrentHashMap<>();

    public PlacePresetManager(Main plugin, DataRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public CompletableFuture<Void> reloadPresets() {
        CompletableFuture<List<PlacePreset>> presetsFuture = repository.loadPlacePresets();
        CompletableFuture<Set<String>> groupsFuture = repository.loadPlacePresetGroups()
                .exceptionally(ex -> Set.of());

        return presetsFuture.thenCombine(groupsFuture, (list, persistedGroups) -> {
            presets.clear();
            groups.clear();

            if (persistedGroups != null) {
                for (String group : persistedGroups) {
                    addGroup(group);
                }
            }

            if (list != null) {
                for (PlacePreset preset : list) {
                    if (preset != null) {
                        presets.put(preset.getId(), preset);
                        addGroup(preset.getGroup());
                    }
                }
            }

            return null;
        }).thenAccept(v -> {
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
        for (String group : this.groups.values()) {
            if (group != null && !group.isBlank()) {
                groups.add(group);
            }
        }
        return new ArrayList<>(groups);
    }

    public boolean hasGroup(String group) {
        if (group == null || group.isBlank()) return false;
        return groups.containsKey(normalize(group));
    }

    public CompletableFuture<Boolean> createGroup(String group) {
        if (group == null || group.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }
        String trimmed = group.trim();
        if (hasGroup(trimmed)) {
            return CompletableFuture.completedFuture(false);
        }

        return repository.createPlacePresetGroup(trimmed)
                .thenCompose(v -> reloadPresets())
                .thenApply(v -> true)
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to create place preset group: " + ex.getMessage());
                    return false;
                });
    }

    public CompletableFuture<Boolean> renameGroup(String oldGroup, String newGroup) {
        if (oldGroup == null || oldGroup.isBlank() || newGroup == null || newGroup.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }

        String oldTrimmed = oldGroup.trim();
        String newTrimmed = newGroup.trim();

        if (!hasGroup(oldTrimmed)) {
            return CompletableFuture.completedFuture(false);
        }
        if (hasGroup(newTrimmed)) {
            return CompletableFuture.completedFuture(false);
        }
        if (normalize(oldTrimmed).equals(normalize(newTrimmed))) {
            return CompletableFuture.completedFuture(false);
        }

        // Update group persistence first (covers empty groups)
        return repository.renamePlacePresetGroup(oldTrimmed, newTrimmed)
                .thenCompose(v -> {
                    List<PlacePreset> toMove = getPresetsInGroup(oldTrimmed);
                    if (toMove.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    CompletableFuture<?>[] futures = new CompletableFuture[toMove.size()];
                    for (int i = 0; i < toMove.size(); i++) {
                        PlacePreset preset = toMove.get(i);
                        preset.setGroup(newTrimmed);
                        futures[i] = repository.savePlacePreset(preset);
                    }
                    return CompletableFuture.allOf(futures);
                })
                .thenCompose(v -> reloadPresets())
                .thenApply(v -> true)
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to rename place preset group: " + ex.getMessage());
                    return false;
                });
    }

    public CompletableFuture<Boolean> deleteGroup(String group) {
        if (group == null || group.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }

        String trimmed = group.trim();
        if (!hasGroup(trimmed)) {
            return CompletableFuture.completedFuture(false);
        }

        List<PlacePreset> inGroup = getPresetsInGroup(trimmed);
        CompletableFuture<?>[] deleteFutures = new CompletableFuture[inGroup.size()];
        for (int i = 0; i < inGroup.size(); i++) {
            deleteFutures[i] = repository.deletePlacePreset(inGroup.get(i).getId());
        }

        return CompletableFuture.allOf(deleteFutures)
                .thenCompose(v -> repository.deletePlacePresetGroup(trimmed))
                .thenCompose(v -> reloadPresets())
                .thenApply(v -> true)
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to delete place preset group: " + ex.getMessage());
                    return false;
                });
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

    private void addGroup(String group) {
        if (group == null || group.isBlank()) {
            return;
        }
        String trimmed = group.trim();
        groups.putIfAbsent(normalize(trimmed), trimmed);
    }
}
