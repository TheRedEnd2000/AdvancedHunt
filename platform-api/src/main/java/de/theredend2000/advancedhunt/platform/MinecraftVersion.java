package de.theredend2000.advancedhunt.platform;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MinecraftVersion {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?.*$");

    private final int major;
    private final int minor;
    private final int patch;

    private MinecraftVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    static MinecraftVersion detect() {
        String raw = Bukkit.getBukkitVersion();
        String base = raw == null ? "" : raw.split("-")[0];

        Matcher m = VERSION_PATTERN.matcher(base);
        if (!m.matches()) {
            // Default to modern if parsing fails.
            return new MinecraftVersion(1, 21, 0);
        }

        int major = parseInt(m.group(1), 1);
        int minor = parseInt(m.group(2), 21);
        int patch = parseInt(m.group(3), 0);
        return new MinecraftVersion(major, minor, patch);
    }

    boolean isAtMostMinor(int maxMinor) {
        return major == 1 && minor <= maxMinor;
    }

    boolean isLessThan(int otherMajor, int otherMinor, int otherPatch) {
        if (major != otherMajor) {
            return major < otherMajor;
        }
        if (minor != otherMinor) {
            return minor < otherMinor;
        }
        return patch < otherPatch;
    }

    private static int parseInt(String s, int fallback) {
        if (s == null) return fallback;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
