package de.theredend2000.advancedhunt.model;

import java.util.Objects;

public class Reward {
    private final RewardType type;
    /**
     * The probability of this reward being granted, expressed as a percentage
     * between 0.0 (never) and 100.0 (always). For example, 50.0 represents a 50% chance.
     */
    private final double chance;
    private final String message;
    private final String broadcast;
    private final String value; // Command string or Base64 item string

    public Reward(RewardType type, double chance, String message, String broadcast, String value) {
        this.type = Objects.requireNonNull(type, "Reward type cannot be null");
        if (chance < 0.0 || chance > 100.0) {
            throw new IllegalArgumentException("Chance must be between 0.0 and 100.0, got: " + chance);
        }
        this.chance = chance;
        this.message = message;
        this.broadcast = broadcast;
        this.value = value;
    }

    public RewardType getType() {
        return type;
    }

    public double getChance() {
        return chance;
    }

    public String getMessage() {
        return message;
    }

    public String getBroadcast() {
        return broadcast;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        String valuePreview = value == null ? "null" : 
            (value.length() > 30 ? value.substring(0, 30) + "..." : value);
        return "Reward{type=" + type + ", chance=" + chance + ", value='" + valuePreview + "'}";
    }
}
