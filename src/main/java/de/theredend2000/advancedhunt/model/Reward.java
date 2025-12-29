package de.theredend2000.advancedhunt.model;

public class Reward {
    private final RewardType type;
    private final double chance;
    private final String message;
    private final String broadcast;
    private final String value; // Command string or Base64 item string

    public Reward(RewardType type, double chance, String message, String broadcast, String value) {
        this.type = type;
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
}
