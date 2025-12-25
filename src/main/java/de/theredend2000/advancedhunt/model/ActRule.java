package de.theredend2000.advancedhunt.model;

import java.util.UUID;

/**
 * Represents an Availability-Cycle-Timing (ACT) rule for collection scheduling.
 * Format: [DATE_RANGE] [DURATION] [CRON_EXPRESSION]
 * 
 * Example: [2025-12-24:2025-12-31] [2h] [0 14 * * *]
 * Meaning: During Dec 24-31, collection is available for 2 hours starting at 2pm daily
 */
public class ActRule {
    private final UUID id;
    private UUID collectionId;
    private String name;
    private String dateRange;      // [START:END] or [*] for always
    private String duration;        // [2h], [30m], [*] for permanent
    private String cronExpression;  // Quartz cron or [MANUAL] or [NONE]
    private boolean enabled;
    private int priority;           // For UI ordering

    /**
     * Creates a new ACT rule with generated UUID
     */
    public ActRule(UUID collectionId, String name) {
        this(UUID.randomUUID(), collectionId, name);
    }

    /**
     * Creates a new ACT rule with specified UUID (for loading from storage)
     */
    public ActRule(UUID id, UUID collectionId, String name) {
        this.id = id;
        this.collectionId = collectionId;
        this.name = name;
        this.dateRange = "*";
        this.duration = "*";
        this.cronExpression = "MANUAL";
        this.enabled = true;
        this.priority = 0;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getCollectionId() {
        return collectionId;
    }

    public String getName() {
        return name;
    }

    public String getDateRange() {
        return dateRange;
    }

    public String getDuration() {
        return duration;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getPriority() {
        return priority;
    }

    // Setters
    public void setCollectionId(UUID collectionId) {
        this.collectionId = collectionId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDateRange(String dateRange) {
        this.dateRange = dateRange;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Gets the full ACT format string
     * @return formatted string: [DATE_RANGE] [DURATION] [CRON]
     */
    public String getActFormat() {
        return String.format("[%s] [%s] [%s]", dateRange, duration, cronExpression);
    }

    @Override
    public String toString() {
        return "ActRule{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", actFormat=" + getActFormat() +
                ", enabled=" + enabled +
                '}';
    }
}
