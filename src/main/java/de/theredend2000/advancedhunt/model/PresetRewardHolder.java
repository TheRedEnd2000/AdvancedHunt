package de.theredend2000.advancedhunt.model;

import de.theredend2000.advancedhunt.Main;

import java.util.ArrayList;
import java.util.List;

/**
 * RewardHolder implementation that edits a RewardPreset.
 */
public class PresetRewardHolder implements RewardHolder {

    private final Main plugin;
    private final RewardPreset preset;

    public PresetRewardHolder(Main plugin, RewardPreset preset) {
        this.plugin = plugin;
        this.preset = preset;
    }

    public RewardPreset getPreset() {
        return preset;
    }

    @Override
    public List<Reward> getRewards() {
        return preset.getRewards() != null ? new ArrayList<>(preset.getRewards()) : new ArrayList<>();
    }

    @Override
    public void saveRewards(List<Reward> rewards) {
        preset.setRewards(rewards);
        plugin.getRewardPresetManager().savePreset(preset);
    }

    @Override
    public String getRewardsTitleKey() {
        return "gui.rewards.preset_title";
    }
}
