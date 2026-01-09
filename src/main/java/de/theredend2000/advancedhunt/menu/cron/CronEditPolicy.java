package de.theredend2000.advancedhunt.menu.cron;

import java.util.Objects;

/**
 * Small policy/config object controlling cron editor behavior per context.
 */
public final class CronEditPolicy {

    private final String cronTypeMessageKey;
    private final boolean allowSpecialValues;
    private final boolean allowNone;
    private final String defaultBuilderExpression;

    public static final String SPECIAL_NONE = "NONE";

    public CronEditPolicy(String cronTypeMessageKey,
                          boolean allowSpecialValues,
                          boolean allowNone,
                          String defaultBuilderExpression) {
        this.cronTypeMessageKey = cronTypeMessageKey;
        this.allowSpecialValues = allowSpecialValues;
        this.allowNone = allowNone;
        this.defaultBuilderExpression = defaultBuilderExpression;
    }

    public String cronTypeMessageKey() {
        return cronTypeMessageKey;
    }

    public boolean allowSpecialValues() {
        return allowSpecialValues;
    }

    public boolean allowNone() {
        return allowNone;
    }

    public String defaultBuilderExpression() {
        return defaultBuilderExpression;
    }

    public static CronEditPolicy progressReset() {
        return new CronEditPolicy("gui.cron.type.reset", false, false, "0 0 0 * * ? *");
    }

    public static CronEditPolicy actSchedule() {
        return new CronEditPolicy("gui.cron.type.act", true, true, "0 0 0 * * ? *");
    }

    public boolean isNone(String value) {
        return value != null && value.equalsIgnoreCase(SPECIAL_NONE);
    }

    public boolean isSpecial(String value) {
        if (!allowSpecialValues || value == null) {
            return false;
        }
        return isNone(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CronEditPolicy)) return false;
        CronEditPolicy that = (CronEditPolicy) o;
        return allowSpecialValues == that.allowSpecialValues
                && allowNone == that.allowNone
                && Objects.equals(cronTypeMessageKey, that.cronTypeMessageKey)
                && Objects.equals(defaultBuilderExpression, that.defaultBuilderExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cronTypeMessageKey, allowSpecialValues, allowNone, defaultBuilderExpression);
    }

    @Override
    public String toString() {
        return "CronEditPolicy{" +
                "cronTypeMessageKey='" + cronTypeMessageKey + '\'' +
                ", allowSpecialValues=" + allowSpecialValues +
                ", allowNone=" + allowNone +
                ", defaultBuilderExpression='" + defaultBuilderExpression + '\'' +
                '}';
    }
}
