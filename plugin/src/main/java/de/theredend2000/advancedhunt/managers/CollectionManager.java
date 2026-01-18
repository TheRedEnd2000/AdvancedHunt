package de.theredend2000.advancedhunt.managers;

import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.data.DataRepository;
import de.theredend2000.advancedhunt.model.ActRule;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.model.PlayerData;
import de.theredend2000.advancedhunt.model.TreasureCore;
import de.theredend2000.advancedhunt.util.CronUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CollectionManager {

    private final JavaPlugin plugin;
    private final DataRepository repository;
    private final TreasureManager treasureManager;
    private final PlayerManager playerManager;
    private final RewardManager rewardManager;
    private final ActRuleEvaluator actRuleEvaluator;
    private List<Collection> cachedCollections;
    private BukkitTask resetTask;

    public CollectionManager(JavaPlugin plugin, DataRepository repository, TreasureManager treasureManager, PlayerManager playerManager, RewardManager rewardManager) {
        this.plugin = plugin;
        this.repository = repository;
        this.treasureManager = treasureManager;
        this.playerManager = playerManager;
        this.rewardManager = rewardManager;
        this.cachedCollections = new ArrayList<>();
        this.actRuleEvaluator = new ActRuleEvaluator(plugin);

        reloadCollections();

        // Check for resets and availability every minute
        resetTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkResets, 20L * 60, 20L * 60);
    }

    public void stop() {
        if (resetTask != null) {
            resetTask.cancel();
            resetTask = null;
        }
    }

    public CompletableFuture<Void> reloadCollections() {
        return repository.loadCollections().thenAccept(collections -> {
            this.cachedCollections = collections;
            actRuleEvaluator.clearAllAvailabilityCache();
        });
    }

    /**
     * Normalizes a collection name for consistent comparison by replacing spaces with underscores
     * and converting to lowercase.
     * 
     * @param name the collection name to normalize
     * @return the normalized name
     */
    public static String normalizeCollectionName(String name) {
        if (name == null) return "";
        return name.replace(" ", "_").toLowerCase();
    }

    public Optional<Collection> getCollectionByName(String name) {
        String normalizedInput = normalizeCollectionName(name);
        return cachedCollections.stream()
                .filter(c -> normalizeCollectionName(c.getName()).equals(normalizedInput))
                .findFirst();
    }

    public Optional<Collection> getCollectionById(UUID id) {
        return cachedCollections.stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    public List<String> getAllCollectionNames() {
        return cachedCollections.stream().map(Collection::getName).collect(Collectors.toList());
    }
    
    public List<Collection> getAllCollections() {
        return new ArrayList<>(cachedCollections);
    }

    public void checkCompletion(Player player, Collection collection) {
        CompletableFuture.runAsync(() -> {
            PlayerData data = playerManager.getPlayerData(player.getUniqueId());

            List<TreasureCore> allCores = treasureManager.getTreasureCoresInCollection(collection.getId());
            if (allCores.isEmpty()) return;

            boolean allFound = true;
            for (TreasureCore core : allCores) {
                if (!data.hasFound(core.getId())) {
                    allFound = false;
                    break;
                }
            }

            if (allFound) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(((Main) plugin).getMessageManager().getMessage("collection.completed", "%collection%", collection.getName()));
                    ((Main) plugin).getSoundManager().playCollectionCompleted(player);
                    rewardManager.giveRewards(player, collection.getCompletionRewards(),collection);
                });
            }else
                ((Main) plugin).getSoundManager().playTreasureFound(player);
        });
    }

    public CompletableFuture<Boolean> renameCollection(String oldName, String newName) {
        // Normalize the new name (replace spaces with underscores)
        String normalizedNewName = newName.replace(" ", "_");
        
        // Check if normalized new name already exists (and is not the same collection)
        Optional<Collection> existing = getCollectionByName(normalizedNewName);
        if (existing.isPresent() && !normalizeCollectionName(existing.get().getName()).equals(normalizeCollectionName(oldName))) {
            return CompletableFuture.completedFuture(false);
        }
        
        Optional<Collection> c = getCollectionByName(oldName);
        return c.map(collection -> repository.renameCollection(collection.getId(), normalizedNewName).thenCompose(success -> {
            if (success) {
                return reloadCollections().thenApply(v -> true);
            }
            return CompletableFuture.completedFuture(false);
        })).orElseGet(() -> CompletableFuture.completedFuture(false));

    }

    public UUID generateUniqueCollectionId() {
        UUID id;
        do {
            id = UUID.randomUUID();
        } while (getCollectionById(id).isPresent());
        return id;
    }

    public UUID generateUniqueActRuleId() {
        UUID id;
        do {
            id = UUID.randomUUID();
        } while (doesActRuleExist(id));
        return id;
    }

    private boolean doesActRuleExist(UUID id) {
        for (Collection collection : cachedCollections) {
            if (collection.getActRules() != null) {
                for (ActRule rule : collection.getActRules()) {
                    if (rule.getId().equals(id)) return true;
                }
            }
        }
        return false;
    }

    public CompletableFuture<Boolean> createCollection(String name) {
        // Normalize the name (replace spaces with underscores)
        String normalizedName = name.replace(" ", "_");
        
        // Check for duplicate (using normalized comparison)
        if (getCollectionByName(normalizedName).isPresent()) {
            return CompletableFuture.completedFuture(false);
        }

        Collection collection = new Collection(generateUniqueCollectionId(), normalizedName, true);
        return repository.saveCollection(collection).thenCompose(v -> 
            reloadCollections().thenApply(x -> true)
        );
    }

    public CompletableFuture<Void> saveCollection(Collection collection) {
        // Clear cache for this collection before saving
        actRuleEvaluator.clearAvailabilityCache(collection.getId());
        return repository.saveCollection(collection).thenCompose(v -> reloadCollections());
    }

    public CompletableFuture<Void> deleteCollection(UUID collectionId) {
        // Delete the collection and its treasures from the repository
        return repository.deleteCollection(collectionId).thenCompose(v -> {
            // Clear treasures from memory cache
            treasureManager.removeCollection(collectionId);
            // Reload collections to update cache
            return reloadCollections();
        });
    }

    private void checkResets() {
        ZonedDateTime now = ZonedDateTime.now();
        for (Collection c : cachedCollections) {
            // Check progress reset cron
            if (c.getProgressResetCron() != null && !c.getProgressResetCron().isEmpty()) {
                try {
                    Cron cron = CronUtils.getParser().parse(c.getProgressResetCron());
                    ExecutionTime executionTime = ExecutionTime.forCron(cron);
                    Optional<ZonedDateTime> lastExecution = executionTime.lastExecution(now);

                    if (lastExecution.isPresent()) {
                        ZonedDateTime lastRun = lastExecution.get();
                        if (lastRun.isAfter(now.minusMinutes(1)) && lastRun.isBefore(now.plusSeconds(1))) {
                            plugin.getLogger().info("Resetting collection progress: " + c.getName());
                            repository.resetCollectionProgress(c.getId());
                            
                            // Clear the particle manager cache for this collection
                            ((Main) plugin).getParticleManager().clearGlobalCache(c.getId());
                            
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                playerManager.invalidate(p.getUniqueId());
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid progress reset cron for collection " + c.getName() + ": " + c.getProgressResetCron());
                }
            }
        }
    }

    /**
     * Checks if a collection is currently available based on its ACT rules.
     * Delegates to ActRuleEvaluator which handles caching and state transitions.
     * @param collection the collection to check
     * @return true if available, false otherwise
     */
    public boolean isCollectionAvailable(Collection collection) {
        return actRuleEvaluator.isCollectionAvailable(collection);
    }

    /**
     * Gets the next activation time for a collection.
     * Delegates to ActRuleEvaluator.
     * @param collection the collection
     * @return Optional containing the next activation time across all rules
     */
    public Optional<ZonedDateTime> getNextActivation(Collection collection) {
        return actRuleEvaluator.getNextActivation(collection);
    }
}
