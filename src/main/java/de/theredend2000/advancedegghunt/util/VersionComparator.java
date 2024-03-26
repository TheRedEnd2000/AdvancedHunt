package de.theredend2000.advancedegghunt.util;

public class VersionComparator {
    public static int compare(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int v1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (v1 < v2) {
                return -1;
            } else if (v1 > v2) {
                return 1;
            }
        }

        return 0;
    }

    public static boolean isGreaterThan(String version1, String version2) {
        return compare(version1, version2) > 0;
    }

    public static boolean isGreaterThanOrEqual(String version1, String version2) {
        return compare(version1, version2) >= 0;
    }

    public static boolean isLessThan(String version1, String version2) {
        return compare(version1, version2) < 0;
    }

    public static boolean isLessThanOrEqual(String version1, String version2) {
        return compare(version1, version2) <= 0;
    }

    public static boolean isEqual(String version1, String version2) {
        return compare(version1, version2) == 0;
    }
}