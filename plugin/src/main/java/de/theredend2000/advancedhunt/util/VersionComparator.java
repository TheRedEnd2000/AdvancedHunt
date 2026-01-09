package de.theredend2000.advancedhunt.util;

import java.util.Comparator;

public class VersionComparator implements Comparator<String> {

    @Override
    public int compare(String v1, String v2) {
        // Identity check for max speed on same objects
        if (v1 == v2) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;

        int i1 = 0, i2 = 0;
        int len1 = v1.length(), len2 = v2.length();

        while (i1 < len1 || i2 < len2) {
            // Skip separators (dots, dashes, underscores, etc.)
            i1 = skipSeparators(v1, i1, len1);
            i2 = skipSeparators(v2, i2, len2);

            // Check end of string
            if (i1 >= len1 && i2 >= len2) return 0;

            // Handle one string ending before the other
            // If v2 continues with a number (1.0 vs 1.0.1), v1 is older (-1).
            // If v2 continues with a qualifier (1.0 vs 1.0-alpha), v1 is newer (1).
            if (i1 >= len1) return isNextTokenNumeric(v2, i2) ? -1 : 1;
            if (i2 >= len2) return isNextTokenNumeric(v1, i1) ? 1 : -1;

            boolean isNum1 = isAsciiDigit(v1.charAt(i1));
            boolean isNum2 = isAsciiDigit(v2.charAt(i2));

            // Compare types: Number > Qualifier (e.g. 1.0.1 > 1.0-alpha)
            if (isNum1 != isNum2) {
                return isNum1 ? 1 : -1;
            }

            if (isNum1) {
                int end1 = findDigitEnd(v1, i1, len1);
                int end2 = findDigitEnd(v2, i2, len2);

                int cmp = compareNumeric(v1, i1, end1, v2, i2, end2);
                if (cmp != 0) return cmp;

                i1 = end1;
                i2 = end2;
            } else {
                int end1 = findLetterEnd(v1, i1, len1);
                int end2 = findLetterEnd(v2, i2, len2);

                int cmp = compareQualifiers(v1, i1, end1, v2, i2, end2);
                if (cmp != 0) return cmp;

                i1 = end1;
                i2 = end2;
            }
        }
        return 0;
    }

    public boolean isEqual(String v1, String v2) {
        return compare(v1, v2) == 0;
    }

    public boolean isLessThan(String v1, String v2) {
        return compare(v1, v2) < 0;
    }

    public boolean isGreaterThan(String v1, String v2) {
        return compare(v1, v2) > 0;
    }

    public boolean isGreaterThanOrEqual(String v1, String v2) {
        return compare(v1, v2) >= 0;
    }

    public boolean isLessThanOrEqual(String v1, String v2) {
        return compare(v1, v2) <= 0;
    }

    private static boolean isAsciiDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private int skipSeparators(String s, int i, int len) {
        while (i < len) {
            char c = s.charAt(i);
            if (isAsciiDigit(c) || isAsciiLetter(c)) break;
            i++;
        }
        return i;
    }

    private int findDigitEnd(String s, int i, int len) {
        while (i < len && isAsciiDigit(s.charAt(i))) i++;
        return i;
    }

    private int findLetterEnd(String s, int i, int len) {
        while (i < len && isAsciiLetter(s.charAt(i))) i++;
        return i;
    }

    private boolean isNextTokenNumeric(String s, int index) {
        return index < s.length() && isAsciiDigit(s.charAt(index));
    }

    private int compareNumeric(String v1, int start1, int end1, String v2, int start2, int end2) {
        long val1 = parseLongSafe(v1, start1, end1);
        long val2 = parseLongSafe(v2, start2, end2);
        return Long.compare(val1, val2);
    }

    private long parseLongSafe(String s, int start, int end) {
        int len = end - start;
        if (len > 19) return Long.MAX_VALUE; // Simple length check first
        
        long val = 0;
        // Fast path for standard lengths (no overflow check needed)
        if (len < 19) {
            for (int i = start; i < end; i++) {
                val = val * 10 + (s.charAt(i) - '0');
            }
        } else {
            // Length is 19, might overflow
            for (int i = start; i < end; i++) {
                int digit = s.charAt(i) - '0';
                if (val > (Long.MAX_VALUE - digit) / 10) {
                    return Long.MAX_VALUE;
                }
                val = val * 10 + digit;
            }
        }
        return val;
    }

    private int compareQualifiers(String v1, int start1, int end1, String v2, int start2, int end2) {
        int w1 = getQualifierWeight(v1, start1, end1);
        int w2 = getQualifierWeight(v2, start2, end2);

        int cmp = Integer.compare(w1, w2);
        if (cmp != 0) return cmp;

        // Fallback: Lexical comparison (case-insensitive)
        int len1 = end1 - start1;
        int len2 = end2 - start2;
        int lim = Math.min(len1, len2);

        for (int k = 0; k < lim; k++) {
            char c1 = v1.charAt(start1 + k);
            char c2 = v2.charAt(start2 + k);
            if (c1 != c2) {
                // Fast ASCII lowercase
                int lc1 = (c1 >= 'A' && c1 <= 'Z') ? (c1 + 32) : c1;
                int lc2 = (c2 >= 'A' && c2 <= 'Z') ? (c2 + 32) : c2;
                if (lc1 != lc2) return lc1 - lc2;
            }
        }
        // If one is a prefix of the other, the longer one is "newer" (e.g. alpha1 > alpha)
        return len1 - len2;
    }

    private int getQualifierWeight(String s, int start, int end) {
        int len = end - start;
        if (len <= 0) return 10;

        char first = s.charAt(start);
        // Case insensitive check for first char
        int lowerFirst = (first >= 'A' && first <= 'Z') ? (first + 32) : first;

        switch (lowerFirst) {
            case 'a': // alpha
                if (len == 5 && s.regionMatches(true, start, "alpha", 0, 5)) return 1;
                break;
            case 'b': // beta
                if (len == 4 && s.regionMatches(true, start, "beta", 0, 4)) return 2;
                break;
            case 's': // snapshot / snap
                if (len == 8 && s.regionMatches(true, start, "snapshot", 0, 8)) return 3;
                if (len == 4 && s.regionMatches(true, start, "snap", 0, 4)) return 3;
                break;
            case 'r': // rc
                if (len == 2 && s.regionMatches(true, start, "rc", 0, 2)) return 4;
                break;
        }
        return 10; // Unknown/Release
    }
}
