package de.theredend2000.advancedhunt.util;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Utility class for Quartz cron expression operations.
 * Provides shared functionality for cron parsing and execution time calculations.
 */
public final class CronUtils {

    private static final ThreadLocal<CronParser> cronParser = ThreadLocal.withInitial(() ->
            new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
    );
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");

    private CronUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets the thread-local CronParser instance for Quartz cron expressions.
     * 
     * @return the thread-local CronParser
     */
    public static CronParser getParser() {
        return cronParser.get();
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
            Cron cron = cronParser.get().parse(cronExpression);
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
     * Produces a human-readable description for a Quartz cron expression.
     *
     * @param cronExpression Quartz cron expression
     * @param locale locale to use for the generated description
     * @return human-readable description, or the raw cron expression if description cannot be generated
     */
    public static String describeQuartzCron(String cronExpression, Locale locale) {
        if (cronExpression == null || cronExpression.isEmpty()) {
            return "";
        }

        try {
            Cron cron = cronParser.get().parse(cronExpression);
            cron.validate();

            Locale effectiveLocale = (locale != null) ? locale : Locale.ENGLISH;
            CronDescriptor descriptor = CronDescriptor.instance(effectiveLocale);
            String description = descriptor.describe(cron);
            if (description == null || description.trim().isEmpty()) {
                return cronExpression;
            }
            return description;
        } catch (MissingResourceException e) {
            // Locale bundle missing (possible with shaded dependencies). Fall back to English.
            try {
                Cron cron = cronParser.get().parse(cronExpression);
                cron.validate();
                String description = CronDescriptor.instance(Locale.ENGLISH).describe(cron);
                return (description == null || description.trim().isEmpty()) ? cronExpression : description;
            } catch (Exception ignored) {
                return cronExpression;
            }
        } catch (Exception e) {
            return cronExpression;
        }
    }

    /**
     * Maps a config language code (e.g. "en", "de", "pt-BR") to a Locale.
     */
    public static Locale toLocale(String languageTag) {
        if (languageTag == null || languageTag.trim().isEmpty()) {
            return Locale.ENGLISH;
        }

        // Support both "pt_BR" and "pt-BR" styles.
        Locale locale = Locale.forLanguageTag(languageTag.trim().replace('_', '-'));
        if (locale.getLanguage() == null || locale.getLanguage().trim().isEmpty() || "und".equals(locale.getLanguage())) {
            return Locale.ENGLISH;
        }
        return locale;
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
