package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.model.PlaceItem;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlaceItemManager {

    private final Main plugin;
    private final DataRepository repository;

    private final Map<UUID, PlaceItem> items = new ConcurrentHashMap<>();
    // normalized group name -> display group name
    private final Map<String, String> groups = new ConcurrentHashMap<>();

    public PlaceItemManager(Main plugin, DataRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public CompletableFuture<Void> reloadItems() {
        CompletableFuture<List<PlaceItem>> itemsFuture = repository.loadPlaceItems();
        CompletableFuture<Set<String>> groupsFuture = repository.loadPlaceItemGroups()
            .exceptionally(ex -> Collections.emptySet());

        return itemsFuture.thenCombine(groupsFuture, (list, persistedGroups) -> {
            items.clear();
            groups.clear();

            if (persistedGroups != null) {
                for (String group : persistedGroups) {
                    addGroup(group);
                }
            }

            if (list != null) {
                for (PlaceItem preset : list) {
                    if (preset != null) {
                        items.put(preset.getId(), preset);
                        addGroup(preset.getGroup());
                    }
                }
            }

            return null;
        }).thenAccept(v -> {
        });
    }

    public Optional<PlaceItem> getItem(UUID id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(items.get(id));
    }

    public List<PlaceItem> getAllItems() {
        return new ArrayList<>(items.values());
    }

    public List<String> getGroups() {
        TreeSet<String> groups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String group : this.groups.values()) {
            if (group != null && !group.trim().isEmpty()) {
                groups.add(group);
            }
        }
        return new ArrayList<>(groups);
    }

    public boolean hasGroup(String group) {
        if (group == null || group.trim().isEmpty()) return false;
        return groups.containsKey(normalize(group));
    }

    public CompletableFuture<Boolean> createGroup(String group) {
        if (group == null || group.trim().isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        String trimmed = group.trim();
        if (hasGroup(trimmed)) {
            return CompletableFuture.completedFuture(false);
        }

        return repository.createPlaceItemGroup(trimmed)
                .thenCompose(v -> reloadItems())
                .thenApply(v -> true)
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to create place preset group: " + ex.getMessage());
                    return false;
                });
    }

    public CompletableFuture<Boolean> renameGroup(String oldGroup, String newGroup) {
        if (oldGroup == null || oldGroup.trim().isEmpty() || newGroup == null || newGroup.trim().isEmpty()) {
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
        return repository.renamePlaceItemGroup(oldTrimmed, newTrimmed)
                .thenCompose(v -> {
                    List<PlaceItem> toMove = getItemsInGroup(oldTrimmed);
                    if (toMove.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    CompletableFuture<?>[] futures = new CompletableFuture[toMove.size()];
                    for (int i = 0; i < toMove.size(); i++) {
                        PlaceItem preset = toMove.get(i);
                        preset.setGroup(newTrimmed);
                        futures[i] = repository.savePlaceItem(preset);
                    }
                    return CompletableFuture.allOf(futures);
                })
                .thenCompose(v -> reloadItems())
                .thenApply(v -> true)
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to rename place preset group: " + ex.getMessage());
                    return false;
                });
    }

    public CompletableFuture<Boolean> deleteGroup(String group) {
        if (group == null || group.trim().isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        String trimmed = group.trim();
        if (!hasGroup(trimmed)) {
            return CompletableFuture.completedFuture(false);
        }

        List<PlaceItem> inGroup = getItemsInGroup(trimmed);
        CompletableFuture<?>[] deleteFutures = new CompletableFuture[inGroup.size()];
        for (int i = 0; i < inGroup.size(); i++) {
            deleteFutures[i] = repository.deletePlaceItem(inGroup.get(i).getId());
        }

        return CompletableFuture.allOf(deleteFutures)
                .thenCompose(v -> repository.deletePlaceItemGroup(trimmed))
                .thenCompose(v -> reloadItems())
                .thenApply(v -> true)
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to delete place preset group: " + ex.getMessage());
                    return false;
                });
    }

    public List<PlaceItem> getItemsInGroup(String group) {
        if (group == null) {
            return Collections.emptyList();
        }
        List<PlaceItem> list = new ArrayList<>();
        for (PlaceItem preset : items.values()) {
            if (preset.getGroup() != null && preset.getGroup().equalsIgnoreCase(group)) {
                list.add(preset);
            }
        }
        list.sort(Comparator.comparing(PlaceItem::getName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public boolean hasPresetNameInGroup(String group, String name) {
        if (group == null || name == null) return false;
        String g = normalize(group);
        String n = normalize(name);
        for (PlaceItem preset : items.values()) {
            if (preset.getGroup() == null || preset.getName() == null) continue;
            if (normalize(preset.getGroup()).equals(g) && normalize(preset.getName()).equals(n)) {
                return true;
            }
        }
        return false;
    }

    public CompletableFuture<Boolean> createItem(String group, String name, String itemData) {
        if (group == null || group.trim().isEmpty() || name == null || name.trim().isEmpty() || itemData == null || itemData.trim().isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        if (hasPresetNameInGroup(group, name)) {
            return CompletableFuture.completedFuture(false);
        }

        PlaceItem preset = new PlaceItem(UUID.randomUUID(), group.trim(), name.trim(), itemData);
        return repository.savePlaceItem(preset)
                .thenCompose(v -> reloadItems())
                .thenApply(v -> true)
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to create place preset: " + ex.getMessage());
                    return false;
                });
    }

    public CompletableFuture<Void> saveItem(PlaceItem preset) {
        if (preset == null) {
            return CompletableFuture.completedFuture(null);
        }
        return repository.savePlaceItem(preset).thenCompose(v -> reloadItems());
    }

    public CompletableFuture<Void> deleteItem(UUID presetId) {
        if (presetId == null) {
            return CompletableFuture.completedFuture(null);
        }
        return repository.deletePlaceItem(presetId).thenCompose(v -> reloadItems());
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void addGroup(String group) {
        if (group == null || group.trim().isEmpty()) {
            return;
        }
        String trimmed = group.trim();
        groups.putIfAbsent(normalize(trimmed), trimmed);
    }
}
