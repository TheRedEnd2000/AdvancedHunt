package de.theredend2000.advancedegghunt.versions.managers.soundmanager;

import org.bukkit.Sound;

public interface SoundManager {

    public Sound playEggAlreadyFoundSound();
    public Sound playEggFoundSound();
    public Sound playAllEggsFound();
    public int getSoundVolume();
    public Sound playEggPlaceSound();
    public Sound playEggBreakSound();
    public Sound playErrorSound();
}
