package de.theredend2000.advancedhunt.model;

import java.util.List;

/**
 * Interface for objects that can hold and manage a list of rewards.
 * Used to abstract reward management between Treasures and Collections.
 */
public interface RewardHolder {
    /**
     * Gets the list of rewards.
     */
    List<Reward> getRewards();

    /**
     * Saves the updated list of rewards.
     * @param rewards The new list of rewards to save.
     */
    void saveRewards(List<Reward> rewards);

    /**
     * Gets a display name for the holder (e.g., "Treasure" or "Collection Name").
     */
    String getDisplayName();
}
