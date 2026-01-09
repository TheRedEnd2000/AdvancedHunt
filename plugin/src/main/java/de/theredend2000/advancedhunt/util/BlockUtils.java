package de.theredend2000.advancedhunt.util;

import de.theredend2000.advancedhunt.platform.PlatformAccess;
import org.bukkit.block.Block;

public class BlockUtils {
    public static String getBlockStateString(Block block) {
        return PlatformAccess.get().getBlockStateString(block);
    }
}
