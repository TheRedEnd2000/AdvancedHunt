package de.theredend2000.advancedhunt.menu.cron;

/**
 * Small policy/config object controlling cron editor behavior per context.
 */
public record CronEditPolicy(
        boolean allowSpecialValues,
        boolean allowNone,
        boolean allowManual,
        String defaultBuilderExpression
) {

    public static final String SPECIAL_NONE = "NONE";
    public static final String SPECIAL_MANUAL = "MANUAL";

    public static CronEditPolicy progressReset() {
        return new CronEditPolicy(false, false, false, "0 0 0 * * ? *");
    }

    public static CronEditPolicy actSchedule() {
        return new CronEditPolicy(true, true, true, "0 0 0 * * ? *");
    }

    public boolean isNone(String value) {
        return value != null && value.equalsIgnoreCase(SPECIAL_NONE);
    }

    public boolean isManual(String value) {
        return value != null && value.equalsIgnoreCase(SPECIAL_MANUAL);
    }

    public boolean isSpecial(String value) {
        if (!allowSpecialValues || value == null) {
            return false;
        }
        return isNone(value) || isManual(value);
    }
}
