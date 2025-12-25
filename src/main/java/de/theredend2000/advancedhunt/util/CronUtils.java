package de.theredend2000.advancedHunt.util;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Utility class for Quartz cron expression operations.
 * Provides shared functionality for cron parsing and execution time calculations.
 */
public final class CronUtils {

    private static final CronParser PARSER = new CronParser(
            CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ)
    );
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");

    private CronUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets the shared CronParser instance for Quartz cron expressions.
     * 
     * @return the shared CronParser
     */
    public static CronParser getParser() {
        return PARSER;
    }

    /**
     * Gets the shared DateTimeFormatter for displaying execution times.
     * 
     * @return the shared DateTimeFormatter
     */
    public static DateTimeFormatter getDateFormat() {
        return DATE_FORMAT;
    }

    /**
     * Calculates the next execution times for a cron expression.
     * 
     * @param cronExpression the Quartz cron expression
     * @param count the number of executions to calculate
     * @return a list of formatted execution times, or an empty list if invalid
     */
    public static List<String> getNextExecutions(String cronExpression, int count) {
        if (cronExpression == null || cronExpression.isEmpty() || count <= 0) {
            return Collections.emptyList();
        }
        
        List<String> executions = new ArrayList<>(count);
        try {
            Cron cron = PARSER.parse(cronExpression);
            ExecutionTime executionTime = ExecutionTime.forCron(cron);
            ZonedDateTime now = ZonedDateTime.now();
            
            Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(now);
            for (int i = 0; i < count && nextExecution.isPresent(); i++) {
                executions.add(nextExecution.get().format(DATE_FORMAT));
                nextExecution = executionTime.nextExecution(nextExecution.get());
            }
        } catch (Exception e) {
            // Invalid expression - return empty list
        }
        return executions;
    }

    /**
     * Formats a ZonedDateTime using the standard date format.
     * 
     * @param dateTime the datetime to format
     * @return the formatted string
     */
    public static String formatDateTime(ZonedDateTime dateTime) {
        return dateTime.format(DATE_FORMAT);
    }
}
