package de.theredend2000.advancedhunt.util;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ActFormatParserTest {

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
    public void parseAcceptsCopiedBracketedExampleWrappedInBackticks() {
        Optional<ActFormatParser.ActSchedule> parsed = ActFormatParser.parse(
            "`[2026-04-03:2026-04-10] [*] [NONE]`"
        );

        assertTrue(parsed.isPresent());
        assertEquals("2026-04-03:2026-04-10", parsed.get().getDateRange());
        assertEquals("*", parsed.get().getDuration());
        assertEquals("NONE", parsed.get().getCron());
    }
}