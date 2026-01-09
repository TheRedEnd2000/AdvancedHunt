package de.theredend2000.advancedhunt.util;

import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser and evaluator for ACT (Availability-Cycle-Timing) format scheduling rules.
 * Format: [DATE_RANGE] [DURATION] [CRON_EXPRESSION]
 * 
 * Components:
 * - DATE_RANGE: ISO date range (2025-12-24:2025-12-31) or * for always
 * - DURATION: Time period (2h, 30m, 1d) or * for permanent
 * - CRON_EXPRESSION: Quartz cron expression or NONE
 */
public class ActFormatParser {
    
    private static final Pattern ACT_PATTERN = Pattern.compile(
            "\\[([^]]+)]\\s+\\[([^]]+)]\\s+\\[([^]]+)]"
    );
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhd])");
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
        "(\\d{4}-\\d{2}-\\d{2}):(\\d{4}-\\d{2}-\\d{2})"
    );
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Parses an ACT format string into an ActSchedule object
     * @param actFormat the ACT format string
     * @return Optional containing ActSchedule if valid, empty if invalid
     */
    public static Optional<ActSchedule> parse(String actFormat) {
        if (actFormat == null || actFormat.trim().isEmpty()) {
            return Optional.empty();
        }

        Matcher matcher = ACT_PATTERN.matcher(actFormat.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String dateRange = matcher.group(1).trim();
        String duration = matcher.group(2).trim();
        String cron = matcher.group(3).trim();

        try {
            return Optional.of(new ActSchedule(dateRange, duration, cron));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Validates a date range string
     * @param dateRange the date range (e.g., "2025-12-24:2025-12-31" or "*")
     * @return true if valid
     */
    public static boolean isValidDateRange(String dateRange) {
        if (dateRange == null) return false;
        if (dateRange.equals("*") || dateRange.equalsIgnoreCase("ALWAYS")) {
            return true;
        }
        
        Matcher matcher = DATE_RANGE_PATTERN.matcher(dateRange);
        if (!matcher.matches()) {
            return false;
        }

        try {
            String startStr = matcher.group(1);
            String endStr = matcher.group(2);
            LocalDate.parse(startStr, DATE_FORMATTER);
            LocalDate.parse(endStr, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Validates a duration string
     * @param duration the duration (e.g., "2h", "30m", "*")
     * @return true if valid
     */
    public static boolean isValidDuration(String duration) {
        if (duration == null) return false;
        if (duration.equals("*") || duration.equalsIgnoreCase("PERMANENT")) {
            return true;
        }
        return DURATION_PATTERN.matcher(duration).matches();
    }

    /**
     * Validates a cron expression or special keyword
     * @param cron the cron expression or NONE
     * @return true if valid
     */
    public static boolean isValidCron(String cron) {
        if (cron == null) return false;
        if (cron.equalsIgnoreCase("NONE")) {
            return true;
        }
        return ValidationUtil.validateCron(cron);
    }

    /**
     * Converts a duration string to human-readable format
     * @param duration the duration string (e.g., "2h", "30m", "*")
     * @return human-readable description
     */
    public static String getHumanReadableDuration(String duration) {
        if (duration == null) return "Invalid";
        if (duration.equals("*") || duration.equalsIgnoreCase("PERMANENT")) {
            return "Permanent";
        }

        Matcher matcher = DURATION_PATTERN.matcher(duration);
        if (!matcher.matches()) {
            return duration; // Return as-is if invalid
        }

        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);

        switch (unit) {
            case "s":
                return value == 1 ? "1 Second" : value + " Seconds";
            case "m":
                return value == 1 ? "1 Minute" : value + " Minutes";
            case "h":
                return value == 1 ? "1 Hour" : value + " Hours";
            case "d":
                return value == 1 ? "1 Day" : value + " Days";
            default:
                return duration;
        }
    }

    /**
     * Converts a date range string to human-readable format
     * @param dateRange the date range (e.g., "2025-12-24:2025-12-31" or "*")
     * @return human-readable description
     */
    public static String getHumanReadableDateRange(String dateRange) {
        if (dateRange == null) return "Invalid";
        if (dateRange.equals("*") || dateRange.equalsIgnoreCase("ALWAYS")) {
            return "Always Active";
        }

        Matcher matcher = DATE_RANGE_PATTERN.matcher(dateRange);
        if (!matcher.matches()) {
            return dateRange; // Return as-is if invalid
        }

        return matcher.group(1) + " to " + matcher.group(2);
    }

    /**
     * Converts a cron expression to human-readable format.
     * This overload supports locale selection and custom strings for NONE/invalid cases.
     *
     * @param cron the cron expression or NONE
     * @param locale locale for the generated description (used when cron is valid)
     * @param noneText text used when cron is NONE
     * @param invalidText text used when cron is null/blank/invalid
     * @return human-readable description
     */
    public static String getHumanReadableCron(String cron, Locale locale, String noneText, String invalidText) {
        if (cron == null || cron.trim().isEmpty()) {
            return invalidText;
        }
        if (cron.equalsIgnoreCase("NONE")) {
            return noneText;
        }
        if (!isValidCron(cron)) {
            return invalidText;
        }
        return CronUtils.describeQuartzCron(cron, locale);
    }

    /**
     * Parses ACT format into individual components
     * @param actFormat the ACT format string
     * @return ActComponents object containing individual components, or null if invalid
     */
    public static ActComponents parseToComponents(String actFormat) {
        if (actFormat == null || actFormat.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = ACT_PATTERN.matcher(actFormat.trim());
        if (!matcher.matches()) {
            return null;
        }

        String dateRange = matcher.group(1).trim();
        String duration = matcher.group(2).trim();
        String cron = matcher.group(3).trim();

        return new ActComponents(dateRange, duration, cron);
    }

    /**
     * Builds an ACT format string from individual components
     * @param dateRange the date range component
     * @param duration the duration component
     * @param cron the cron component
     * @return formatted ACT string
     */
    public static String buildActFormat(String dateRange, String duration, String cron) {
        return "[" + dateRange + "] [" + duration + "] [" + cron + "]";
    }

    /**
     * Simple container for ACT components
     */
    public static class ActComponents {
        private final String dateRange;
        private final String duration;
        private final String cron;

        public ActComponents(String dateRange, String duration, String cron) {
            this.dateRange = dateRange;
            this.duration = duration;
            this.cron = cron;
        }

        public String getDateRange() {
            return dateRange;
        }

        public String getDuration() {
            return duration;
        }

        public String getCron() {
            return cron;
        }

        /**
         * Validates all components
         * @return true if all components are valid
         */
        public boolean isValid() {
            return isValidDateRange(dateRange) && 
                   isValidDuration(duration) && 
                   isValidCron(cron);
        }
    }

    /**
     * Parses a duration string into a Duration object
     * @param durationStr the duration string (e.g., "2h", "30m")
     * @return Duration object or null for permanent
     */
    static Duration parseDuration(String durationStr) {
        if (durationStr.equals("*") || durationStr.equalsIgnoreCase("PERMANENT")) {
            return null; // Permanent = no expiration
        }

        Matcher matcher = DURATION_PATTERN.matcher(durationStr);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid duration format: " + durationStr);
        }

        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);

        switch (unit) {
            case "s": return Duration.ofSeconds(value);
            case "m": return Duration.ofMinutes(value);
            case "h": return Duration.ofHours(value);
            case "d": return Duration.ofDays(value);
            default: throw new IllegalArgumentException("Unknown duration unit: " + unit);
        }
    }

    /**
     * Represents a parsed and validated ACT schedule
     */
    public static class ActSchedule {
        private final String dateRangeStr;
        private final String durationStr;
        private final String cronStr;
        private final ZonedDateTime ruleStart;
        private final ZonedDateTime ruleEnd;
        private final Duration activeDuration;
        private final ExecutionTime executionTime;
        private final boolean isNone;

        ActSchedule(String dateRange, String duration, String cron) {
            this.dateRangeStr = dateRange;
            this.durationStr = duration;
            this.cronStr = cron;

            ZoneId serverZone = ZoneId.systemDefault();

            // Parse date range
            if (dateRange.equals("*") || dateRange.equalsIgnoreCase("ALWAYS")) {
                this.ruleStart = null;
                this.ruleEnd = null;
            } else {
                Matcher matcher = DATE_RANGE_PATTERN.matcher(dateRange);
                if (!matcher.matches()) {
                    throw new IllegalArgumentException("Invalid date range: " + dateRange);
                }

                LocalDate startDate = LocalDate.parse(matcher.group(1), DATE_FORMATTER);
                LocalDate endDate = LocalDate.parse(matcher.group(2), DATE_FORMATTER);

                this.ruleStart = startDate.atStartOfDay(serverZone);
                this.ruleEnd = endDate.atTime(LocalTime.MAX).atZone(serverZone);
            }

            // Parse duration
            this.activeDuration = parseDuration(duration);

            // Parse cron
            this.isNone = cron.equalsIgnoreCase("NONE");

            if (this.isNone) {
                this.executionTime = null;
            } else {
                try {
                    CronParser parser = CronUtils.getParser();
                    Cron parsedCron = parser.parse(cron);
                    this.executionTime = ExecutionTime.forCron(parsedCron);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid cron expression: " + cron, e);
                }
            }
        }

        /**
         * Checks if this rule is currently applicable (within date range)
         * @param now current time
         * @return true if the rule is active
         */
        public boolean isRuleActive(ZonedDateTime now) {
            if (ruleStart == null && ruleEnd == null) {
                return true; // Always active
            }
            return !now.isBefore(ruleStart) && !now.isAfter(ruleEnd);
        }

        /**
         * Checks if collection should be available based on this rule
         * @param now current time
         * @param lastActivation last time this rule triggered (null if never)
         * @return true if collection is currently available
         */
        public boolean isAvailable(ZonedDateTime now, ZonedDateTime lastActivation) {
            if (!isRuleActive(now)) {
                return false;
            }

            // NONE means: no schedule required. If the date range matches, the rule is available.
            // This keeps common cases ("always available", seasonal windows) simple.
            if (isNone) {
                return true;
            }

            // Cron-based: check if we're within duration window of last trigger
            Optional<ZonedDateTime> lastExecution = executionTime.lastExecution(now);
            if (!lastExecution.isPresent()) {
                return false;
            }

            ZonedDateTime lastTrigger = lastExecution.get();
            
            // Must be within rule date range
            if (ruleStart != null && lastTrigger.isBefore(ruleStart)) {
                return false;
            }
            if (ruleEnd != null && lastTrigger.isAfter(ruleEnd)) {
                return false;
            }

            if (activeDuration == null) {
                // Permanent availability after first trigger
                return true;
            }

            // Check if within duration window
            ZonedDateTime expirationTime = lastTrigger.plus(activeDuration);
            return now.isBefore(expirationTime);
        }

        /**
         * Gets the next trigger time for this rule
         * @param now current time
         * @return Optional containing next trigger time
         */
        public Optional<ZonedDateTime> getNextTrigger(ZonedDateTime now) {
            if (isNone || executionTime == null) {
                return Optional.empty();
            }

            Optional<ZonedDateTime> next = executionTime.nextExecution(now);
            if (!next.isPresent()) {
                return Optional.empty();
            }

            ZonedDateTime nextTime = next.get();
            
            // Check if next trigger is within rule date range
            if (ruleEnd != null && nextTime.isAfter(ruleEnd)) {
                return Optional.empty();
            }
            if (ruleStart != null && nextTime.isBefore(ruleStart)) {
                // Find first execution within range
                return executionTime.nextExecution(ruleStart.minusSeconds(1));
            }

            return Optional.of(nextTime);
        }

        public String getDateRange() {
            return dateRangeStr;
        }

        public String getDuration() {
            return durationStr;
        }

        public String getCron() {
            return cronStr;
        }

        public boolean isNone() {
            return isNone;
        }

        public Duration getActiveDuration() {
            return activeDuration;
        }
    }
}
