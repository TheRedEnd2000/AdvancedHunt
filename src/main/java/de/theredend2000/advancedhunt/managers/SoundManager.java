package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.Main;
import org.bukkit.Sound;

public class SoundManager {

    public Sound playEggAlreadyFoundSound() {
        return Main.getInstance().getPluginConfig().getEggAlreadyFoundSound();
    }
    public Sound playEggFoundSound() {
        return Main.getInstance().getPluginConfig().getPlayerFindEggSound();
    }
    public Sound playAllEggsFound() {
        return Main.getInstance().getPluginConfig().getAllEggsFoundSound();
    }
    public Sound playEggBreakSound() {
        return Main.getInstance().getPluginConfig().getEggBreakSound();
    }
    public Sound playEggPlaceSound() {
        return Main.getInstance().getPluginConfig().getEggPlaceSound();
    }
    public Sound playErrorSound() {
        return Main.getInstance().getPluginConfig().getErrorSound();
    }
    public Sound playInventorySuccessSound() {
        return Main.getInstance().getPluginConfig().getInventoryClickSuccess();
    }
    public Sound playInventoryFailedSound() {
        return Main.getInstance().getPluginConfig().getInventoryClickFailed();
    }
    public float getSoundVolume(){
        return (Main.getInstance().getPluginConfig().getSoundVolume() / 100f);
    }
}
