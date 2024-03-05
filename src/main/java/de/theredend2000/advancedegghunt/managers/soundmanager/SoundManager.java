package de.theredend2000.advancedegghunt.managers.soundmanager;

import com.cryptomorin.xseries.XSound;
import de.theredend2000.advancedegghunt.Main;
import org.bukkit.Sound;

public class SoundManager {

    public Sound playEggAlreadyFoundSound() {
        return XSound.valueOf(Main.getInstance().getPluginConfig().getString("Sounds.EggAlreadyFoundSound")).parseSound();
    }
    public Sound playEggFoundSound() {
        return XSound.valueOf(Main.getInstance().getPluginConfig().getString("Sounds.PlayerFindEggSound")).parseSound();
    }
    public Sound playAllEggsFound() {
        return XSound.valueOf(Main.getInstance().getPluginConfig().getString("Sounds.AllEggsFoundSound")).parseSound();
    }
    public Sound playEggBreakSound() {
        return XSound.valueOf(Main.getInstance().getPluginConfig().getString("Sounds.EggBreakSound")).parseSound();
    }
    public Sound playEggPlaceSound() {
        return XSound.valueOf(Main.getInstance().getPluginConfig().getString("Sounds.EggPlaceSound")).parseSound();
    }
    public Sound playErrorSound() {
        return XSound.valueOf(Main.getInstance().getPluginConfig().getString("Sounds.ErrorSound")).parseSound();
    }
    public Sound playInventorySuccessSound() {
        return XSound.valueOf(Main.getInstance().getPluginConfig().getString("Sounds.InventoryClickSuccess")).parseSound();
    }
    public Sound playInventoryFailedSound() {
        return XSound.valueOf(Main.getInstance().getPluginConfig().getString("Sounds.InventoryClickFailed")).parseSound();
    }
    public int getSoundVolume(){
        return Main.getInstance().getPluginConfig().getSoundVolume();
    }
}
