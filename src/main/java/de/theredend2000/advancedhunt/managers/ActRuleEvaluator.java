package de.theredend2000.advancedhunt.managers;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.theredend2000.advancedhunt.model.ActRule;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.util.ActFormatParser;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Evaluates ACT (Availability-Cycle-Timing) rules for collections.
 * Handles rule activation checks, next activation/deactivation calculations,
 * and state transition predictions for caching purposes.
 */
public class ActRuleEvaluator {

    private final JavaPlugin plugin;
    private final CronParser cronParser;
    
    // Availability cache with pre-calculated state transitions
    private final Cache<UUID, AvailabilityCacheEntry> availabilityCache;
    
    /**
     * Cache entry holding availability state and expiry time
     */
    private static class AvailabilityCacheEntry {
        final boolean available;
        final ZonedDateTime expiresAt;
        
        AvailabilityCacheEntry(boolean available, ZonedDateTime expiresAt) {
            this.available = available;
            this.expiresAt = expiresAt;
        }
        
        boolean isExpired(ZonedDateTime now) {
            return expiresAt != null && now.isAfter(expiresAt);
        }
    }

    public ActRuleEvaluator(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
        
        // Initialize availability cache - expiration handled per-entry based on state transitions
        this.availabilityCache = Caffeine.newBuilder()
            .maximumSize(1000) // Safety limit (typically < 100 collections)
            .build();
    }

    /**
     * Checks if a collection is currently available based on its ACT rules.
     * Uses caching with pre-calculated state transitions for optimal performance.
     * A collection is available if it's enabled AND any of its enabled rules are active.
     * @param collection the collection to check
     * @return true if available, false otherwise
     */
    public boolean isCollectionAvailable(Collection collection) {
        ZonedDateTime now = ZonedDateTime.now();
        
        // Check cache first
        AvailabilityCacheEntry cached = availabilityCache.getIfPresent(collection.getId());
        if (cached != null && !cached.isExpired(now)) {
            return cached.available;
        }
        
        // Cache miss or expired - recalculate
        boolean available = calculateAvailability(collection, now);
        
        // Calculate next state transition and cache result
        Optional<ZonedDateTime> nextTransition = getNextStateTransition(collection, now, available);
        ZonedDateTime expiresAt = nextTransition.orElse(now.plusMinutes(1)); // Fallback: 1 min TTL
        
        availabilityCache.put(collection.getId(), new AvailabilityCacheEntry(available, expiresAt));
        
        return available;
    }
    
    /**
     * Calculates current availability without caching.
     * Checks if collection is enabled and if any enabled rules are active.
     */
    private boolean calculateAvailability(Collection collection, ZonedDateTime now) {
        if (!collection.isEnabled()) {
            return false;
        }

        List<ActRule> enabledRules = collection.getEnabledActRules();
        if (enabledRules.isEmpty()) {
            // No rules = always available (backward compatibility)
            return true;
        }
        
        // Check each enabled rule - if ANY is active, collection is available
        for (ActRule rule : enabledRules) {
            if (isRuleActive(collection.getId(), rule, now)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Checks if a specific ACT rule is currently active
     * @param collectionId the collection ID
     * @param rule the rule to check
     * @param now current time
     * @return true if the rule is currently active
     */
    public boolean isRuleActive(UUID collectionId, ActRule rule, ZonedDateTime now) {
        try {
            String actFormat = rule.getActFormat();
            Optional<ActFormatParser.ActSchedule> scheduleOpt = ActFormatParser.parse(actFormat);
            
            if (!scheduleOpt.isPresent()) {
                plugin.getLogger().warning("Invalid ACT format for rule " + rule.getName() + ": " + actFormat);
                return false;
            }
            
            ActFormatParser.ActSchedule schedule = scheduleOpt.get();

            return schedule.isAvailable(now, null);
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking ACT rule " + rule.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the next activation time for a collection
     * @param collection the collection
     * @return Optional containing the next activation time across all rules
     */
    public Optional<ZonedDateTime> getNextActivation(Collection collection) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime earliest = null;
        
        for (ActRule rule : collection.getEnabledActRules()) {
            try {
                String actFormat = rule.getActFormat();
                Optional<ActFormatParser.ActSchedule> scheduleOpt = ActFormatParser.parse(actFormat);
                
                if (scheduleOpt.isPresent()) {
                    Optional<ZonedDateTime> nextTrigger = scheduleOpt.get().getNextTrigger(now);
                    if (nextTrigger.isPresent()) {
                        if (earliest == null || nextTrigger.get().isBefore(earliest)) {
                            earliest = nextTrigger.get();
                        }
                    }
                }
            } catch (Exception e) {
                // Skip invalid rules
            }
        }
        
        return Optional.ofNullable(earliest);
    }
    
    /**
     * Gets the next deactivation time for a currently available collection
     * @param collection the collection
     * @return Optional containing the next deactivation time
     */
    public Optional<ZonedDateTime> getNextDeactivation(Collection collection) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime earliest = null;
        
        for (ActRule rule : collection.getEnabledActRules()) {
            // Only check currently active rules
            if (!isRuleActive(collection.getId(), rule, now)) {
                continue;
            }
            
            try {
                String actFormat = rule.getActFormat();
                Optional<ActFormatParser.ActSchedule> scheduleOpt = ActFormatParser.parse(actFormat);
                
                if (scheduleOpt.isPresent()) {
                    ActFormatParser.ActSchedule schedule = scheduleOpt.get();

                    // NONE has no deactivation time (it is date-range based).
                    if (schedule.isNone() || schedule.getActiveDuration() == null) {
                        continue;
                    }

                    // Cron-based rules
                    ExecutionTime executionTime = ExecutionTime.forCron(
                        cronParser.parse(schedule.getCron())
                    );
                    Optional<ZonedDateTime> lastExecution = executionTime.lastExecution(now);

                    if (lastExecution.isPresent()) {
                        ZonedDateTime deactivation = lastExecution.get().plus(schedule.getActiveDuration());
                        if (earliest == null || deactivation.isBefore(earliest)) {
                            earliest = deactivation;
                        }
                    }
                }
            } catch (Exception e) {
                // Skip invalid rules
            }
        }
        
        return Optional.ofNullable(earliest);
    }
    
    /**
     * Gets the next state transition (activation or deactivation) for a collection.
     * Used for cache expiry calculations.
     * @param collection the collection
     * @param now current time
     * @param currentlyAvailable whether the collection is currently available
     * @return Optional containing the next transition time
     */
    public Optional<ZonedDateTime> getNextStateTransition(Collection collection, ZonedDateTime now, boolean currentlyAvailable) {
        if (currentlyAvailable) {
            // Currently available - find when it will become unavailable
            return getNextDeactivation(collection);
        } else {
            // Currently unavailable - find when it will become available
            return getNextActivation(collection);
        }
    }
    
    /**
     * Clears the availability cache for a specific collection.
     * Called when collection configuration changes.
     */
    public void clearAvailabilityCache(UUID collectionId) {
        availabilityCache.invalidate(collectionId);
    }
    
    /**
     * Clears the entire availability cache.
     * Called during reload or when rules are manually activated.
     */
    public void clearAllAvailabilityCache() {
        availabilityCache.invalidateAll();
    }
}
