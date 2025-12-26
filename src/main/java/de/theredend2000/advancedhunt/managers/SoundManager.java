package de.theredend2000.advancedhunt.managers;

import com.cryptomorin.xseries.XSound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class SoundManager {

    private final JavaPlugin plugin;
    private ConfigurationSection soundConfig;
    private static ConfigurationSection staticSoundConfig;

    public SoundManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        soundConfig = plugin.getConfig().getConfigurationSection("sounds");
        staticSoundConfig = plugin.getConfig().getConfigurationSection("sounds");
    }

    public boolean isEnabled(){
        return soundConfig != null && soundConfig.getBoolean("enabled", true);
    }

    public static boolean isEnabledStatic(){
        return staticSoundConfig != null && staticSoundConfig.getBoolean("enabled", true);
    }

    public boolean isSoundEnabled(String key){
        ConfigurationSection section = soundConfig.getConfigurationSection(key);
        return section != null && section.getBoolean("enabled", true);
    }

    public void playTreasureFound(Player player) {
        playSound(player, "treasure-found");
    }

    public void playCollectionCompleted(Player player) {
        playSound(player, "collection-completed");
    }

    public void playTreasurePlaced(Player player) {
        playSound(player, "treasure-placed");
    }

    public void playTreasureBroken(Player player) {
        playSound(player, "treasure-broken");
    }

    public void playTreasureAlreadyFound(Player player) {
        playSound(player, "treasure-already-found");
    }

    public void playTreasureClaimedByOther(Player player) {
        playSound(player, "treasure-claimed-by-other");
    }

    public void playBlockProtected(Player player) {
        playSound(player, "block-protected");
    }
    public void playPlaceModeBreakDeny(Player player) {
        playSound(player, "place-mode-break-denied");
    }

    private void playSound(Player player, String key) {
        if (!isEnabled()) return;

        if (!isSoundEnabled(key)) return;

        ConfigurationSection section = soundConfig.getConfigurationSection(key);
        if (section == null) return;

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