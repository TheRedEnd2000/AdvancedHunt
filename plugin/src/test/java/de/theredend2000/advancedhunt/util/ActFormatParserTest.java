package de.theredend2000.advancedhunt.util;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.Assert.*;

public class ActFormatParserTest {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private static ZonedDateTime at(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZONE);
    }

    private static ActFormatParser.ActSchedule schedule(String actFormat) {
        Optional<ActFormatParser.ActSchedule> parsed = ActFormatParser.parse(actFormat);
        assertTrue(parsed.isPresent());
        return parsed.get();
    }

    @Test
    public void parseAcceptsBracketedDateWindowRule() {
        Optional<ActFormatParser.ActSchedule> parsed = ActFormatParser.parse(
            "[2026-04-03:2026-04-10] [*] [NONE]"
        );

        assertTrue(parsed.isPresent());
        assertEquals("2026-04-03:2026-04-10", parsed.get().getDateRange());
        assertEquals("*", parsed.get().getDuration());
        assertEquals("NONE", parsed.get().getCron());
    }

    @Test
    public void parseAcceptsTokenizedRepeatingRule() {
        Optional<ActFormatParser.ActSchedule> parsed = ActFormatParser.parse(
            "2026-04-03:2026-04-10 2h 0 0 9 * * ?"
        );

        assertTrue(parsed.isPresent());
        assertEquals("2026-04-03:2026-04-10", parsed.get().getDateRange());
        assertEquals("2h", parsed.get().getDuration());
        assertEquals("0 0 9 * * ?", parsed.get().getCron());
    }

    @Test
    public void parseRejectsCopiedBracketedExampleWrappedInBackticks() {
        Optional<ActFormatParser.ActSchedule> parsed = ActFormatParser.parse(
            "`[2026-04-03:2026-04-10] [*] [NONE]`"
        );

        assertFalse(parsed.isPresent());
    }

    @Test
    public void parseRejectsCodeFencedQuotedExample() {
        Optional<ActFormatParser.ActSchedule> parsed = ActFormatParser.parse(
            "```\n\"[2026-04-03:2026-04-10] [*] [NONE]\"\n```"
        );

        assertFalse(parsed.isPresent());
    }

    @Test
    public void noneRuleRequiresCurrentTimeToBeInsideDateRange() {
        ActFormatParser.ActSchedule schedule = schedule("[2026-04-03:2026-04-10] [*] [NONE]");

        assertTrue(schedule.isAvailable(at(2026, 4, 5, 12, 0), null));
        assertFalse(schedule.isAvailable(at(2026, 4, 11, 0, 0), null));
    }

    @Test
    public void cronRuleBecomesUnavailableAtExactExpirationBoundary() {
        ActFormatParser.ActSchedule schedule = schedule("[*] [2h] [0 0 9 * * ?]");

        assertTrue(schedule.isAvailable(at(2026, 4, 3, 10, 59), null));
        assertFalse(schedule.isAvailable(at(2026, 4, 3, 11, 0), null));
    }

    @Test
    public void permanentCronRuleStaysAvailableAfterFirstTrigger() {
        ActFormatParser.ActSchedule schedule = schedule("[2026-04-03:2026-04-10] [*] [0 0 9 * * ?]");

        assertFalse(schedule.isAvailable(at(2026, 4, 3, 8, 59), null));
        assertTrue(schedule.isAvailable(at(2026, 4, 3, 12, 0), null));
    }

    @Test
    public void getNextTriggerFindsFirstExecutionWithinRuleStart() {
        ActFormatParser.ActSchedule schedule = schedule("[2026-04-03:2026-04-10] [2h] [0 0 9 * * ?]");
        Optional<ZonedDateTime> next = schedule.getNextTrigger(at(2026, 4, 1, 12, 0));

        assertTrue(next.isPresent());
        assertEquals(at(2026, 4, 3, 9, 0), next.get());
    }

    @Test
    public void getNextTriggerReturnsEmptyWhenNextExecutionFallsAfterRuleEnd() {
        ActFormatParser.ActSchedule schedule = schedule("[2026-04-03:2026-04-03] [2h] [0 0 9 * * ?]");

        assertFalse(schedule.getNextTrigger(at(2026, 4, 3, 10, 0)).isPresent());
    }
}