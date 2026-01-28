package de.theredend2000.advancedhunt.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Collection {
    private final UUID id;
    private String name;
    private boolean enabled;
    private List<ActRule> actRules; // ACT scheduling rules for availability
    private String progressResetCron; // Cron expression for progress reset (separate from availability)
    private boolean singlePlayerFind; // If true, only one player can find it (global treasure)
    private List<Reward> completionRewards; // Rewards for finding all treasures
    private UUID defaultTreasureRewardPresetId; // Applied to newly created treasures in this collection
    private boolean hideWhenNotAvailable; // If true, treasures are hidden when collection is not available

    public Collection(UUID id, String name, boolean enabled) {
        this.id = id;
        this.name = name;
        this.enabled = enabled;
        this.actRules = new ArrayList<>();
        this.progressResetCron = null;
        this.hideWhenNotAvailable = false;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<ActRule> getActRules() {
        return actRules;
    }

    public void setActRules(List<ActRule> actRules) {
        this.actRules = actRules != null ? actRules : new ArrayList<>();
    }

    public void addActRule(ActRule rule) {
        if (this.actRules == null) {
            this.actRules = new ArrayList<>();
        }
        this.actRules.add(rule);
    }

    public void removeActRule(UUID ruleId) {
        if (this.actRules != null) {
            this.actRules.removeIf(rule -> rule.getId().equals(ruleId));
        }
    }

    public List<ActRule> getEnabledActRules() {
        if (this.actRules == null) {
            return new ArrayList<>();
        }
        return this.actRules.stream()
                .filter(ActRule::isEnabled)
                .collect(Collectors.toList());
    }

    public String getProgressResetCron() {
        return progressResetCron;
    }

    public void setProgressResetCron(String progressResetCron) {
        this.progressResetCron = progressResetCron;
    }

    // Legacy getters/setters for backward compatibility during migration
    @Deprecated
    public String getResetCron() {
        return progressResetCron;
    }

    @Deprecated
    public void setResetCron(String resetCron) {
        this.progressResetCron = resetCron;
    }

    @Deprecated
    public String getActiveStart() {
        return null; // Legacy field, no longer used
    }

    @Deprecated
    public void setActiveStart(String activeStart) {
        // Legacy setter, ignore
    }

    @Deprecated
    public String getActiveEnd() {
        return null; // Legacy field, no longer used
    }

    @Deprecated
    public void setActiveEnd(String activeEnd) {
        // Legacy setter, ignore
    }

    public boolean isSinglePlayerFind() {
        return singlePlayerFind;
    }

    public void setSinglePlayerFind(boolean singlePlayerFind) {
        this.singlePlayerFind = singlePlayerFind;
    }

    public List<Reward> getCompletionRewards() {
        return completionRewards;
    }

    public void setCompletionRewards(List<Reward> completionRewards) {
        this.completionRewards = completionRewards;
    }

    public UUID getDefaultTreasureRewardPresetId() {
        return defaultTreasureRewardPresetId;
    }

    public void setDefaultTreasureRewardPresetId(UUID defaultTreasureRewardPresetId) {
        this.defaultTreasureRewardPresetId = defaultTreasureRewardPresetId;
    }

    public boolean isHideWhenNotAvailable() {
        return hideWhenNotAvailable;
    }

    public void setHideWhenNotAvailable(boolean hideWhenNotAvailable) {
        this.hideWhenNotAvailable = hideWhenNotAvailable;
    }
}
