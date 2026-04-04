package de.theredend2000.advancedhunt.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ValidationUtilTest {

    @Test
    public void validateDateRangeAcceptsAlwaysAliasAndRejectsInvalidCalendarDate() {
        assertTrue(ValidationUtil.validateDateRange("ALWAYS"));
        assertFalse(ValidationUtil.validateDateRange("2026-02-30:2026-03-01"));
    }

    @Test
    public void validateDurationAcceptsPermanentAliasAndRejectsUnsupportedUnit() {
        assertTrue(ValidationUtil.validateDuration("PERMANENT"));
        assertFalse(ValidationUtil.validateDuration("8hours"));
    }

    @Test
    public void validateCronRecognizesQuartzAndRejectsPlainText() {
        assertTrue(ValidationUtil.validateCron("0 0 9 * * ?"));
        assertFalse(ValidationUtil.validateCron("every morning"));
    }

    @Test
    public void validateTimeAcceptsIsoLocalTimeAndRejectsOutOfRangeValues() {
        assertTrue(ValidationUtil.validateTime("09:15"));
        assertFalse(ValidationUtil.validateTime("25:00"));
    }
}