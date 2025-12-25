package de.theredend2000.advancedHunt.util;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;

import java.lang.reflect.Method;

public class BlockUtils {

    private static final boolean IS_LEGACY;
    private static Method GET_DATA_METHOD;
    private static Method GET_BLOCK_DATA_METHOD;
    private static Method GET_AS_STRING_METHOD;

    static {
        boolean legacy = false;
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            int minorVersion = Integer.parseInt(version.split("_")[1]);
            legacy = minorVersion < 13;
        } catch (Exception e) {
            // Fallback or assume modern if parsing fails, but usually this works for CraftBukkit/Spigot
            // If it fails, we might be in a test environment or very weird server.
            // Defaulting to false (modern) is safer for new servers.
        }
        IS_LEGACY = legacy;

        try {
            if (IS_LEGACY) {
                GET_DATA_METHOD = Block.class.getMethod("getData");
            } else {
                GET_BLOCK_DATA_METHOD = Block.class.getMethod("getBlockData");
                Class<?> blockDataClass = Class.forName("org.bukkit.block.data.BlockData");
                GET_AS_STRING_METHOD = blockDataClass.getMethod("getAsString");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getBlockStateString(Block block) {
        try {
            if (IS_LEGACY) {
                if (GET_DATA_METHOD != null) {
                    Object data = GET_DATA_METHOD.invoke(block);
                    return String.valueOf(data);
                }
            } else {
                if (GET_BLOCK_DATA_METHOD != null && GET_AS_STRING_METHOD != null) {
                    Object blockData = GET_BLOCK_DATA_METHOD.invoke(block);
                    return (String) GET_AS_STRING_METHOD.invoke(blockData);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return IS_LEGACY ? "0" : "";
    }
    
    public static boolean isLegacy() {
        return IS_LEGACY;
    }
}
