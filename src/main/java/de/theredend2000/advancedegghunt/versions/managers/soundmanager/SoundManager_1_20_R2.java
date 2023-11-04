package de.theredend2000.advancedegghunt.versions.managers.soundmanager;

import de.theredend2000.advancedegghunt.Main;
import org.bukkit.Sound;

public class SoundManager_1_20_R2 implements SoundManager{


    public Sound playEggAlreadyFoundSound() {
        return Sound.valueOf(Main.getInstance().getConfig().getString("Sounds.EggAlreadyFoundSound"));
    }
    public Sound playEggFoundSound() {
        return Sound.valueOf(Main.getInstance().getConfig().getString("Sounds.PlayerFindEggSound"));
    }
    public Sound playAllEggsFound() {
        return Sound.valueOf(Main.getInstance().getConfig().getString("Sounds.AllEggsFoundSound"));
    }
    public Sound playEggBreakSound() {
        return Sound.valueOf(Main.getInstance().getConfig().getString("Sounds.EggBreakSound"));
    }
    public Sound playEggPlaceSound() {
        return Sound.valueOf(Main.getInstance().getConfig().getString("Sounds.EggPlaceSound"));
    }
    public Sound playErrorSound() {
        return Sound.valueOf(Main.getInstance().getConfig().getString("Sounds.ErrorSound"));
    }
    public Sound playInventorySuccessSound() {
        return Sound.valueOf(Main.getInstance().getConfig().getString("Sounds.InventoryClickSuccess"));
    }
    public Sound playInventoryFailedSound() {
        return Sound.valueOf(Main.getInstance().getConfig().getString("Sounds.InventoryClickFailed"));
    }
    public int getSoundVolume(){
        return Main.getInstance().getConfig().getInt("Settings.SoundVolume");
    }
}
