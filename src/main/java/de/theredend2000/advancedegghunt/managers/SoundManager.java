package de.theredend2000.advancedegghunt.managers;

import de.theredend2000.advancedegghunt.Main;
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
    public int getSoundVolume(){
        return Main.getInstance().getPluginConfig().getSoundVolume();
    }
}
