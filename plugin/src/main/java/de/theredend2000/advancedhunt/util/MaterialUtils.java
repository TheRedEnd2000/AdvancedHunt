package de.theredend2000.advancedhunt.util;

import de.theredend2000.advancedhunt.platform.PlatformAccess;
import org.bukkit.Material;

public final class MaterialUtils {

    private MaterialUtils() {
    }

    public static boolean isAir(Material material) {
        return PlatformAccess.get().isAir(material);
    }
}
