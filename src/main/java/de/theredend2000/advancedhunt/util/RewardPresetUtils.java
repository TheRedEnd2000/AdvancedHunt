package de.theredend2000.advancedhunt.util;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.model.Reward;
import de.theredend2000.advancedhunt.model.RewardPresetType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RewardPresetUtils {

    private RewardPresetUtils() {
    }

    public static List<Reward> getDefaultTreasureRewards(Main plugin, UUID collectionId) {
        if (plugin == null || collectionId == null) return List.of();

        List<Reward> rewards = new ArrayList<>();
        plugin.getCollectionManager().getCollectionById(collectionId)
                .map(Collection::getDefaultTreasureRewardPresetId)
                .flatMap(defaultPresetId -> plugin.getRewardPresetManager().getPreset(RewardPresetType.TREASURE, defaultPresetId))
                .ifPresent(preset -> rewards.addAll(preset.getRewards()));

        return rewards;
    }
}
