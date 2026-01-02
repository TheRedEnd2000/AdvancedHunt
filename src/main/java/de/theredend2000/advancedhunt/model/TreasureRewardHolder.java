package de.theredend2000.advancedhunt.model;

import de.theredend2000.advancedhunt.Main;

import java.util.ArrayList;
import java.util.List;

public class TreasureRewardHolder implements RewardHolder {
    private final Main plugin;
    private Treasure treasure;

    public TreasureRewardHolder(Main plugin, Treasure treasure) {
        this.plugin = plugin;
        this.treasure = treasure;
    }

    @Override
    public List<Reward> getRewards() {
        return treasure.getRewards() != null ? new ArrayList<>(treasure.getRewards()) : new ArrayList<>();
    }

    @Override
    public void saveRewards(List<Reward> rewards) {
        Treasure newTreasure = new Treasure(
            treasure.getId(),
            treasure.getCollectionId(),
            treasure.getLocation(),
            new ArrayList<>(rewards),
            treasure.getNbtData(),
            treasure.getMaterial(),
            treasure.getBlockState()
        );
        plugin.getTreasureManager().updateTreasure(treasure, newTreasure);
        this.treasure = newTreasure;
    }

    @Override
    public String getRewardsTitleKey() {
        return "gui.rewards.title";
    }
}
