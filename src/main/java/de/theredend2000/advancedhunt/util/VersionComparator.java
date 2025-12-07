package de.theredend2000.advancedhunt.util;

public class VersionComparator {
    private static final String SNAPSHOT_TOKEN = "SNAPSHOT";

    public static int compare(String version1, String version2) {
        if (version1 == null || version2 == null) {
            throw new IllegalArgumentException("Version values must not be null");
        }

        // Split versions into parts using both dash and dot as separators
        String[] firstVersionParts = version1.split("[-.]");
        String[] secondVersionParts = version2.split("[-.]");

        int maxLength = Math.max(firstVersionParts.length, secondVersionParts.length);

        for (int partIndex = 0; partIndex < maxLength; partIndex++) {
            String firstPart = getVersionPart(firstVersionParts, partIndex);
            String secondPart = getVersionPart(secondVersionParts, partIndex);

            int comparisonResult = compareVersionParts(firstPart, secondPart);
            if (comparisonResult != 0) {
                return comparisonResult;
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

    /**
     * Gets the version part at the specified index, returning "0" if the index is out of bounds.
     */
    private static String getVersionPart(String[] versionParts, int index) {
        if (index >= versionParts.length) {
            return "0";
        }
        String part = versionParts[index].trim();
        return part.isEmpty() ? "0" : part;
    }

    /**
     * Compares two version parts, handling SNAPSHOT tokens and numeric/string comparisons.
     * SNAPSHOT versions are considered less than release versions.
     */
    private static int compareVersionParts(String firstPart, String secondPart) {
        boolean firstIsSnapshot = isSnapshotToken(firstPart);
        boolean secondIsSnapshot = isSnapshotToken(secondPart);

        // Handle SNAPSHOT tokens
        if (firstIsSnapshot || secondIsSnapshot) {
            if (firstIsSnapshot && secondIsSnapshot) {
                return 0; // Both are snapshots, considered equal
            }
            return firstIsSnapshot ? -1 : 1; // SNAPSHOT < release
        }

        // Try to compare as integers
        Integer firstNumber = tryParseInteger(firstPart);
        Integer secondNumber = tryParseInteger(secondPart);

        if (firstNumber != null && secondNumber != null) {
            return Integer.compare(firstNumber, secondNumber);
        }

        // If one is numeric and the other is not, numeric is considered greater
        if (firstNumber != null) {
            return 1;
        }
        if (secondNumber != null) {
            return -1;
        }

        // Both are non-numeric strings, compare lexicographically
        return firstPart.compareToIgnoreCase(secondPart);
    }

    /**
     * Attempts to parse a string as an integer, returning null if parsing fails.
     */
    private static Integer tryParseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Checks if the given part represents a SNAPSHOT token.
     */
    private static boolean isSnapshotToken(String part) {
        return SNAPSHOT_TOKEN.equalsIgnoreCase(part);
    }
}