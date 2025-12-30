package de.theredend2000.advancedhunt.model;

import de.theredend2000.advancedhunt.Main;

import java.util.ArrayList;
import java.util.List;

public class CollectionRewardHolder implements RewardHolder {
    private final Main plugin;
    private final Collection collection;

    public CollectionRewardHolder(Main plugin, Collection collection) {
        this.plugin = plugin;
        this.collection = collection;
    }

    @Override
    public List<Reward> getRewards() {
        return collection.getCompletionRewards() != null ? new ArrayList<>(collection.getCompletionRewards()) : new ArrayList<>();
    }

    @Override
    public void saveRewards(List<Reward> rewards) {
        collection.setCompletionRewards(new ArrayList<>(rewards));
        plugin.getCollectionManager().saveCollection(collection);
    }

    @Override
    public String getDisplayName() {
        return collection.getName();
    }

    public Collection getCollection() {
        return collection;
    }
}
