package de.theredend2000.advancedhunt.util;

public class VersionComparator {
    public static int compare(String version1, String version2) {
        // Split versions into parts
        String[] parts1 = version1.split("[-.]");
        String[] parts2 = version2.split("[-.]");

        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            String v1 = i < parts1.length ? parts1[i] : "0";
            String v2 = i < parts2.length ? parts2[i] : "0";

            // Try to parse as integers
            try {
                int i1 = Integer.parseInt(v1);
                int i2 = Integer.parseInt(v2);
                if (i1 < i2) return -1;
                if (i1 > i2) return 1;
            } catch (NumberFormatException e) {
                // If parsing fails, compare as strings
                int result = v1.compareTo(v2);
                if (result != 0) return result;
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