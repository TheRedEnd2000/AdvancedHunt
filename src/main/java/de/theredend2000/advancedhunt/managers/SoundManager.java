package de.theredend2000.advancedhunt.managers;

import com.cryptomorin.xseries.XSound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class SoundManager {

    private final JavaPlugin plugin;
    private ConfigurationSection soundConfig;

    public SoundManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        soundConfig = plugin.getConfig().getConfigurationSection("sounds");
    }

    public void playTreasureFound(Player player) {
        playSound(player, "treasure-found");
    }

    public void playCollectionCompleted(Player player) {
        playSound(player, "collection-completed");
    }

    private void playSound(Player player, String key) {
        if (soundConfig == null || !soundConfig.getBoolean("enabled", true)) {
            return;
        }

        ConfigurationSection section = soundConfig.getConfigurationSection(key);
        if (section == null || !section.getBoolean("enabled", true)) {
            return;
        }

        String soundName = section.getString("sound");
        if (soundName == null || soundName.isEmpty()) {
            return;
        }

        float volume = (float) section.getDouble("volume", 1.0);
        float pitch = (float) section.getDouble("pitch", 1.0);

        Optional<XSound> xSound = XSound.matchXSound(soundName);
        if (xSound.isPresent()) {
            xSound.get().play(player, volume, pitch);
        } else {
            plugin.getLogger().warning("Invalid sound name in config: " + soundName + " (Section: " + key + ")");
        }
    }
}
