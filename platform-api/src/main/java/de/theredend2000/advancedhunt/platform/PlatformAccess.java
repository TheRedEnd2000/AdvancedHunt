package de.theredend2000.advancedhunt.platform;

import org.bukkit.Bukkit;

public final class PlatformAccess {

    private static volatile PlatformAdapter adapter;

    private PlatformAccess() {
    }

    public static PlatformAdapter get() {
        PlatformAdapter local = adapter;
        if (local != null) {
            return local;
        }
        synchronized (PlatformAccess.class) {
            if (adapter != null) {
                return adapter;
            }
            adapter = loadAdapter();
            return adapter;
        }
    }

    private static PlatformAdapter loadAdapter() {
        MinecraftVersion version = MinecraftVersion.detect();

        // Select the highest applicable adapter tier.
        // 1.8.x -> legacy effect-based particles.
        // 1.9+  -> Particle API.
        // 1.13+ -> modern head material + UUID skull owner.
        // 1.14+ -> custom model data + unbreakable.
        // 1.15+ -> Material#isAir().
        // 1.20.5+ -> hide tooltip (and other component-era meta changes).
        String implClass;
        if (version.isLessThan(1, 9, 0)) {
            implClass = "de.theredend2000.advancedhunt.platform.impl.Spigot18PlatformAdapter";
        } else if (version.isLessThan(1, 13, 0)) {
            implClass = "de.theredend2000.advancedhunt.platform.impl.Spigot19PlatformAdapter";
        } else if (version.isLessThan(1, 14, 0)) {
            implClass = "de.theredend2000.advancedhunt.platform.impl.Spigot113PlatformAdapter";
        } else if (version.isLessThan(1, 15, 0)) {
            implClass = "de.theredend2000.advancedhunt.platform.impl.Spigot114PlatformAdapter";
        } else if (version.isLessThan(1, 20, 5)) {
            implClass = "de.theredend2000.advancedhunt.platform.impl.Spigot115PlatformAdapter";
        } else {
            implClass = "de.theredend2000.advancedhunt.platform.impl.SpigotModernPlatformAdapter";
        }

        try {
            Class<?> clazz = Class.forName(implClass, true, PlatformAccess.class.getClassLoader());
            Object instance = clazz.getDeclaredConstructor().newInstance();
            if (!(instance instanceof PlatformAdapter)) {
                throw new IllegalStateException("Platform adapter does not implement PlatformAdapter: " + implClass);
            }
            Bukkit.getLogger().info("[AdvancedHunt] Using platform adapter: " + implClass + " (MC " + version + ")");
            return (PlatformAdapter) instance;
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to load platform adapter: " + implClass + " (MC " + version + ")", t);
        }
    }
}
