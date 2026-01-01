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

import java.time.ZonedDateTime;
import java.util.*;
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
    private final Map<UUID, Map<UUID, ZonedDateTime>> ruleActivationTimes; // Collection ID -> Rule ID -> Last Activation Time

    public CollectionManager(JavaPlugin plugin, DataRepository repository, TreasureManager treasureManager, PlayerManager playerManager, RewardManager rewardManager) {
        this.plugin = plugin;
        this.repository = repository;
        this.treasureManager = treasureManager;
        this.playerManager = playerManager;
        this.rewardManager = rewardManager;
        this.cachedCollections = new ArrayList<>();
        this.ruleActivationTimes = new HashMap<>();
        this.actRuleEvaluator = new ActRuleEvaluator(plugin, ruleActivationTimes);

        reloadCollections();

        // Check for resets and availability every minute
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkResets, 20L * 60, 20L * 60);
    }

    public CompletableFuture<Void> reloadCollections() {
        return repository.loadCollections().thenAccept(collections -> {
            this.cachedCollections = collections;
            actRuleEvaluator.clearAllAvailabilityCache();
        });
    }

    public Optional<Collection> getCollectionByName(String name) {
        return cachedCollections.stream().filter(c -> c.getName().equalsIgnoreCase(name)).findFirst();
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
        Optional<Collection> c = getCollectionByName(oldName);
        if (c.isEmpty()) return CompletableFuture.completedFuture(false);
        
        return repository.renameCollection(c.get().getId(), newName).thenCompose(success -> {
            if (success) {
                return reloadCollections().thenApply(v -> true);
            }
            return CompletableFuture.completedFuture(false);
        });
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
        if (getCollectionByName(name).isPresent()) {
            return CompletableFuture.completedFuture(false);
        }

        Collection collection = new Collection(generateUniqueCollectionId(), name, true);
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
     * Manually activates a collection rule (for MANUAL cron expressions).
     * Delegates to ActRuleEvaluator.
     * @param collectionId the collection ID
     * @param ruleId the rule ID to activate
     */
    public void activateRule(UUID collectionId, UUID ruleId) {
        actRuleEvaluator.activateRule(collectionId, ruleId);
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
