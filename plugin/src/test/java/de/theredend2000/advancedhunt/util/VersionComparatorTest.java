package de.theredend2000.advancedhunt.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class VersionComparatorTest {

    private final VersionComparator comparator = new VersionComparator();

    @Test
    public void compareTreatsMinorVersionsAsHigherThanWholeVersion() {
        assertTrue(comparator.compare("4.1", "4.0") > 0);
        assertTrue(comparator.compare("2.1", "2") > 0);
    }

    @Test
    public void compareTreatsAdditionalNumericSegmentsAsNewer() {
        assertTrue(comparator.compare("4.0", "4") > 0);
        assertTrue(comparator.compare("2.0.0", "2") > 0);
    }

    @Test
    public void compareHandlesIdentityAndNullInputs() {
        String sameReference = new String("1.2.3");

        assertEquals(0, comparator.compare(sameReference, sameReference));
        assertTrue(comparator.compare(null, "1.0") < 0);
        assertTrue(comparator.compare("1.0", null) > 0);
        assertEquals(0, comparator.compare(null, null));
    }

    @Test
    public void compareTreatsNumericTokensAsNewerThanQualifierTokens() {
        assertTrue(comparator.compare("1.0.1", "1.0-alpha") > 0);
        assertTrue(comparator.compare("1.0-alpha", "1.0.1") < 0);
        assertTrue(comparator.compare("1.0", "1.0-alpha") > 0);
    }

    @Test
    public void compareOrdersKnownQualifiersAndReleaseCorrectly() {
        assertTrue(comparator.compare("1.0-alpha", "1.0-beta") < 0);
        assertTrue(comparator.compare("1.0-beta", "1.0-snapshot") < 0);
        assertTrue(comparator.compare("1.0-snapshot", "1.0-rc") < 0);
        assertTrue(comparator.compare("1.0-rc", "1.0") < 0);
        assertEquals(0, comparator.compare("1.0-SNAPSHOT", "1.0-snapShot"));
    }

    @Test
    public void compareFallsBackToCaseInsensitiveLexicalAndLengthComparisonForUnknownQualifiers() {
        assertTrue(comparator.compare("1.0-preview", "1.0-release") < 0);
        assertEquals(0, comparator.compare("1.0-preview", "1.0-PREVIEW"));
        assertTrue(comparator.compare("1.0-alpha1", "1.0-alpha") > 0);
    }

    @Test
    public void compareIgnoresDifferentSeparatorCharactersBetweenTokens() {
        assertEquals(0, comparator.compare("1-0_rc1", "1.0.rc1"));
        assertEquals(0, comparator.compare("2__0--beta", "2.0.beta"));
    }

    @Test
    public void compareHandlesVeryLargeNumericTokensWithoutOverflowing() {
        assertTrue(comparator.compare("100000000000000000000", "2") > 0);
        assertEquals(0, comparator.compare("9223372036854775808", "9223372036854775807"));
    }

    @Test
    public void helperMethodsDelegateToCompareConsistently() {
        assertTrue(comparator.isEqual("1.0", "1.0"));
        assertFalse(comparator.isEqual("1.0", "1.1"));

        assertTrue(comparator.isLessThan("1.0-alpha", "1.0-beta"));
        assertFalse(comparator.isLessThan("1.0", "1.0"));

        assertTrue(comparator.isGreaterThan("2.0", "1.9"));
        assertFalse(comparator.isGreaterThan("1.0", "1.0"));

        assertTrue(comparator.isGreaterThanOrEqual("1.0", "1.0"));
        assertTrue(comparator.isGreaterThanOrEqual("1.0.1", "1.0"));

        assertTrue(comparator.isLessThanOrEqual("1.0", "1.0"));
        assertTrue(comparator.isLessThanOrEqual("1.0-beta", "1.0"));
    }
}