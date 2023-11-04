package de.theredend2000.advancedegghunt.versions;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ConsoleMessages;
import de.theredend2000.advancedegghunt.versions.managers.eggmanager.EggManager;
import de.theredend2000.advancedegghunt.versions.managers.eggmanager.*;
import de.theredend2000.advancedegghunt.versions.managers.extramanager.ExtraManager;
import de.theredend2000.advancedegghunt.versions.managers.extramanager.*;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.*;
import de.theredend2000.advancedegghunt.versions.managers.soundmanager.SoundManager;
import de.theredend2000.advancedegghunt.versions.managers.soundmanager.*;
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

        switch (sversion) {
            case "v1_20_R2":
                eggManager = new EggManager_1_20_R2();
                break;
            case "v1_20_R1":
                eggManager = new EggManager_1_20_R1();
                break;
            case "v1_19_R3":
                eggManager = new EggManager_1_19_R3();
                break;
            case "v1_19_R2":
                eggManager = new EggManager_1_19_R2();
                break;
            case "v1_19_R1":
                eggManager = new EggManager_1_19_R1();
                break;
            case "v1_18_R2":
                eggManager = new EggManager_1_18_R2();
                break;
            case "v1_18_R1":
                eggManager = new EggManager_1_18_R1();
                break;
            case "v1_17_R1":
                eggManager = new EggManager_1_17_R1();
                break;
            case "v1_16_R3":
                eggManager = new EggManager_1_16_R3();
                break;
            case "v1_16_R2":
                eggManager = new EggManager_1_16_R2();
                break;
            case "v1_16_R1":
                eggManager = new EggManager_1_16_R1();
                break;
            case "v1_15_R1":
                eggManager = new EggManager_1_15_R1();
                break;
            case "v1_14_R1":
                eggManager = new EggManager_1_14_R1();
                break;
            case "v1_13_R2":
                eggManager = new EggManager_1_13_R2();
                break;
            case "v1_13_R1":
                eggManager = new EggManager_1_13_R1();
                break;
            default:
                ConsoleMessages.sendNotCompatibleVersion();
                Bukkit.getPluginManager().disablePlugin(Main.getInstance());
                break;
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

        switch (sversion) {
            case "v1_20_R2":
                inventoryManager = new InventoryManager_1_20_R2();
                break;
            case "v1_20_R1":
                inventoryManager = new InventoryManager_1_20_R1();
                break;
            case "v1_19_R3":
                inventoryManager = new InventoryManager_1_19_R3();
                break;
            case "v1_19_R2":
                inventoryManager = new InventoryManager_1_19_R2();
                break;
            case "v1_19_R1":
                inventoryManager = new InventoryManager_1_19_R1();
                break;
            case "v1_18_R2":
                inventoryManager = new InventoryManager_1_18_R2();
                break;
            case "v1_18_R1":
                inventoryManager = new InventoryManager_1_18_R1();
                break;
            case "v1_17_R1":
                inventoryManager = new InventoryManager_1_17_R1();
                break;
            case "v1_16_R3":
                inventoryManager = new InventoryManager_1_16_R3();
                break;
            case "v1_16_R2":
                inventoryManager = new InventoryManager_1_16_R2();
                break;
            case "v1_16_R1":
                inventoryManager = new InventoryManager_1_16_R1();
                break;
            case "v1_15_R1":
                inventoryManager = new InventoryManager_1_15_R1();
                break;
            case "v1_14_R1":
                inventoryManager = new InventoryManager_1_14_R1();
                break;
            case "v1_13_R2":
                inventoryManager = new InventoryManager_1_13_R2();
                break;
            case "v1_13_R1":
                inventoryManager = new InventoryManager_1_13_R1();
                break;
            default:
                ConsoleMessages.sendNotCompatibleVersion();
                Bukkit.getPluginManager().disablePlugin(Main.getInstance());
                break;
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

        switch (sversion) {
            case "v1_20_R2":
                extraManager = new ExtraManager_1_20_R2();
                break;
            case "v1_20_R1":
                extraManager = new ExtraManager_1_20_R1();
                break;
            case "v1_19_R3":
                extraManager = new ExtraManager_1_19_R3();
                break;
            case "v1_19_R2":
                extraManager = new ExtraManager_1_19_R2();
                break;
            case "v1_19_R1":
                extraManager = new ExtraManager_1_19_R1();
                break;
            case "v1_18_R2":
                extraManager = new ExtraManager_1_18_R2();
                break;
            case "v1_18_R1":
                extraManager = new ExtraManager_1_18_R1();
                break;
            case "v1_17_R1":
                extraManager = new ExtraManager_1_17_R1();
                break;
            case "v1_16_R3":
                extraManager = new ExtraManager_1_16_R3();
                break;
            case "v1_16_R2":
                extraManager = new ExtraManager_1_16_R2();
                break;
            case "v1_16_R1":
                extraManager = new ExtraManager_1_16_R1();
                break;
            case "v1_15_R1":
                extraManager = new ExtraManager_1_15_R1();
                break;
            case "v1_14_R1":
                extraManager = new ExtraManager_1_14_R1();
                break;
            case "v1_13_R2":
                extraManager = new ExtraManager_1_13_R2();
                break;
            case "v1_13_R1":
                extraManager = new ExtraManager_1_13_R1();
                break;
            default:
                ConsoleMessages.sendNotCompatibleVersion();
                Bukkit.getPluginManager().disablePlugin(Main.getInstance());
                break;
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

        switch (sversion) {
            case "v1_20_R2":
                soundManager = new SoundManager_1_20_R2();
                break;
            case "v1_20_R1":
                soundManager = new SoundManager_1_20_R1();
                break;
            case "v1_19_R3":
                soundManager = new SoundManager_1_19_R3();
                break;
            case "v1_19_R2":
                soundManager = new SoundManager_1_19_R2();
                break;
            case "v1_19_R1":
                soundManager = new SoundManager_1_19_R1();
                break;
            case "v1_18_R2":
                soundManager = new SoundManager_1_18_R2();
                break;
            case "v1_18_R1":
                soundManager = new SoundManager_1_18_R1();
                break;
            case "v1_17_R1":
                soundManager = new SoundManager_1_17_R1();
                break;
            case "v1_16_R3":
                soundManager = new SoundManager_1_16_R3();
                break;
            case "v1_16_R2":
                soundManager = new SoundManager_1_16_R2();
                break;
            case "v1_16_R1":
                soundManager = new SoundManager_1_16_R1();
                break;
            case "v1_15_R1":
                soundManager = new SoundManager_1_15_R1();
                break;
            case "v1_14_R1":
                soundManager = new SoundManager_1_14_R1();
                break;
            case "v1_13_R2":
                soundManager = new SoundManager_1_13_R2();
                break;
            case "v1_13_R1":
                soundManager = new SoundManager_1_13_R1();
                break;
            default:
                ConsoleMessages.sendNotCompatibleVersion();
                break;
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
