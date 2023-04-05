package de.theredend2000.advancedegghunt.versions;

import de.theredend2000.advancedegghunt.util.ConsoleMessages;
import de.theredend2000.advancedegghunt.versions.managers.eggmanager.EggManager;
import de.theredend2000.advancedegghunt.versions.managers.eggmanager.EggManager_1_15_R1;
import de.theredend2000.advancedegghunt.versions.managers.eggmanager.EggManager_1_16_R3;
import de.theredend2000.advancedegghunt.versions.managers.eggmanager.EggManager_1_19_R1;
import de.theredend2000.advancedegghunt.versions.managers.extramanager.ExtraManager;
import de.theredend2000.advancedegghunt.versions.managers.extramanager.ExtraManager_1_19_R1;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.InventoryManager;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.InventoryManager_1_19_R1;
import de.theredend2000.advancedegghunt.versions.managers.soundmanager.SoundManager;
import de.theredend2000.advancedegghunt.versions.managers.soundmanager.SoundManager_1_19_R1;
import org.bukkit.Bukkit;

public class VersionManager {

    private static String sversion;
    private static EggManager eggManager;
    private static InventoryManager inventoryManager;
    private static ExtraManager extraManager;
    private static SoundManager soundManager;


    public static void registerAllManagers(){
        setupEggManager();
        setupInventoryManager();
        setupExtraManager();
        setupSoundManager();
    }

    private static boolean setupEggManager(){

        sversion = "N/A";

        try {
            sversion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        } catch (ArrayIndexOutOfBoundsException e){
            return false;
        }

        if(sversion.equals("v1_15_R1")){
            eggManager = new EggManager_1_15_R1();
        }else if(sversion.equals("v1_16_R3")){
            eggManager = new EggManager_1_16_R3();
        }else if(sversion.equals("v1_19_R1")){
            eggManager = new EggManager_1_19_R1();
        }else{
            ConsoleMessages.sendNotCompatibleVersion();
        }

        return eggManager != null;
    }

    private static boolean setupInventoryManager(){

        sversion = "N/A";

        try {
            sversion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        } catch (ArrayIndexOutOfBoundsException e){
            return false;
        }

        if(sversion.equals("v1_19_R1")){
            inventoryManager = new InventoryManager_1_19_R1();
        }else{
            ConsoleMessages.sendNotCompatibleVersion();
        }

        return inventoryManager != null;
    }

    private static boolean setupExtraManager(){

        sversion = "N/A";

        try {
            sversion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        } catch (ArrayIndexOutOfBoundsException e){
            return false;
        }

        if(sversion.equals("v1_19_R1")){
            extraManager = new ExtraManager_1_19_R1();
        }else{
            ConsoleMessages.sendNotCompatibleVersion();
        }

        return extraManager != null;
    }

    private static boolean setupSoundManager(){

        sversion = "N/A";

        try {
            sversion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        } catch (ArrayIndexOutOfBoundsException e){
            return false;
        }

        if(sversion.equals("v1_19_R1")){
            soundManager = new SoundManager_1_19_R1();
        }else{
            ConsoleMessages.sendNotCompatibleVersion();
        }

        return soundManager != null;
    }

    public static EggManager getEggManager() {
        return eggManager;
    }

    public static InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public static ExtraManager getExtraManager() {
        return extraManager;
    }

    public static SoundManager getSoundManager() {
        return soundManager;
    }
}
