package de.theredend2000.advancedhunt.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class RewardPreset {
    private final UUID id;
    private final RewardPresetType type;
    private String name;
    private List<Reward> rewards;

    public RewardPreset(UUID id, RewardPresetType type, String name, List<Reward> rewards) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.rewards = rewards != null ? new ArrayList<>(rewards) : new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public RewardPresetType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Reward> getRewards() {
        return rewards != null ? new ArrayList<>(rewards) : new ArrayList<>();
    }

    public void setRewards(List<Reward> rewards) {
        this.rewards = rewards != null ? new ArrayList<>(rewards) : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RewardPreset)) return false;
        RewardPreset that = (RewardPreset) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
