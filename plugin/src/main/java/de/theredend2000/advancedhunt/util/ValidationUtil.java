package de.theredend2000.advancedhunt.util;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class ValidationUtil {

    private static final ThreadLocal<CronParser> cronParser = ThreadLocal.withInitial(() ->
        new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
    );

    public static boolean validateCron(String cronExpression) {
        try {
            cronParser.get().parse(cronExpression);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean validateTime(String time) {
        try {
            LocalTime.parse(time);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Validates a date range for ACT format
     * @param dateRange the date range string (e.g., "2025-12-24:2025-12-31" or "*")
     * @return true if valid
     */
    public static boolean validateDateRange(String dateRange) {
        return ActFormatParser.isValidDateRange(dateRange);
    }

    /**
     * Validates a duration for ACT format
     * @param duration the duration string (e.g., "2h", "30m", "*")
     * @return true if valid
     */
    public static boolean validateDuration(String duration) {
        return ActFormatParser.isValidDuration(duration);
    }

    /**
     * Validates a full ACT format string
     * @param actFormat the ACT format string [DATE_RANGE] [DURATION] [CRON]
     * @return true if valid
     */
    public static boolean validateActFormat(String actFormat) {
        return ActFormatParser.parse(actFormat).isPresent();
    }
}
