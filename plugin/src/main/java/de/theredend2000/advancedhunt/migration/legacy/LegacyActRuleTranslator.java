package de.theredend2000.advancedhunt.migration.legacy;

import de.theredend2000.advancedhunt.model.ActRule;
import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Translates legacy Requirements/RequirementsOrder into new ACT rules.
 *
 * This aims for a "referable" outcome: rules are explicit and editable.
 */
public final class LegacyActRuleTranslator {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private enum LegacySeason {
        WINTER, SPRING, SUMMER, FALL
    }

    private LegacyActRuleTranslator() {
    }

    public static List<ActRule> translate(UUID collectionId, LegacyEggsParser.LegacyRequirements legacy) {
        if (legacy == null || legacy.raw == null) {
            // Always available
            return Collections.singletonList(alwaysRule(collectionId, "Always Available"));
        }

        String order = legacy.order == null ? "OR" : legacy.order.trim().toUpperCase(Locale.ROOT);
        if (!order.equals("AND") && !order.equals("OR")) {
            order = "OR";
        }

        ConfigurationSection req = legacy.raw;

        // Extract constraints
        Set<Integer> hours = readIntKeys(req.getConfigurationSection("Hours"), 0, 23);
        Set<Integer> months = readIntKeys(req.getConfigurationSection("Month"), 1, 12);
        Set<Integer> years = readIntKeys(req.getConfigurationSection("Year"), 1970, 2099);
        Set<String> weekdays = readWeekdays(req.getConfigurationSection("Weekday"));
        Set<LegacySeason> seasons = readSeasons(req.getConfigurationSection("Season"));
        List<LocalDateRange> dateRanges = readDateRanges(req.getConfigurationSection("Date"));

        // Season -> months (intersect with explicit months if present)
        Set<Integer> seasonMonths = seasonsToMonths(seasons);
        if (!seasonMonths.isEmpty()) {
            if (months.isEmpty()) {
                months = seasonMonths;
            } else {
                months.retainAll(seasonMonths);
            }
        }

        // Year constraints are best expressed via date-range(s) to avoid weird once-per-year cron logic.
        if (!years.isEmpty() && dateRanges.isEmpty()) {
            dateRanges = years.stream()
                .sorted()
                .map(y -> new LocalDateRange(LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31)))
                .collect(Collectors.toList());
        }

        if (order.equals("AND")) {
            return translateAnd(collectionId, hours, weekdays, months, dateRanges);
        }

        // OR: create separate rules per category for editability.
        List<ActRule> rules = new ArrayList<>();

        if (!hours.isEmpty()) {
            rules.add(hoursRule(collectionId, "Legacy: Hours", hours));
        }
        if (!weekdays.isEmpty()) {
            rules.add(weekdayRule(collectionId, "Legacy: Weekdays", weekdays));
        }
        if (!months.isEmpty()) {
            rules.add(monthRule(collectionId, "Legacy: Months", months));
        }
        if (!dateRanges.isEmpty()) {
            rules.addAll(dateRangeRules(collectionId, "Legacy: Dates", dateRanges));
        }

        if (rules.isEmpty()) {
            rules.add(alwaysRule(collectionId, "Always Available"));
        }

        return rules;
    }

    private static List<ActRule> translateAnd(UUID collectionId,
                                             Set<Integer> hours,
                                             Set<String> weekdays,
                                             Set<Integer> months,
                                             List<LocalDateRange> dateRanges) {
        // If no explicit constraints, always.
        boolean hasAny = !(hours.isEmpty() && weekdays.isEmpty() && months.isEmpty() && dateRanges.isEmpty());
        if (!hasAny) {
            return Collections.singletonList(alwaysRule(collectionId, "Always Available"));
        }

        // Date ranges: if empty, use '*'
        List<LocalDateRange> ranges = dateRanges.isEmpty()
            ? Collections.singletonList(LocalDateRange.ANY)
            : dateRanges;

        // If we only have date-range constraints, we can represent it as always active in that window.
        boolean onlyDateRange = !ranges.isEmpty() && hours.isEmpty() && weekdays.isEmpty() && months.isEmpty();
        if (onlyDateRange) {
            return dateRangeRules(collectionId, "Legacy: Dates", ranges);
        }

        // Otherwise build one rule per date-range chunk (OR across rules, AND inside each).
        List<ActRule> out = new ArrayList<>();
        for (LocalDateRange r : ranges) {
            String name = r == LocalDateRange.ANY ? "Legacy: AND (no date restriction)" : "Legacy: AND " + r.start + ".." + r.end;
            ActRule rule = new ActRule(UUID.randomUUID(), collectionId, name);
            rule.setDateRange(r.toActDateRange());

            // Hours constraint -> 1h windows; otherwise daily 24h windows.
            if (!hours.isEmpty()) {
                rule.setDuration("1h");
                rule.setCronExpression(buildCron(hours, months, weekdays));
            } else {
                rule.setDuration("24h");
                rule.setCronExpression(buildCron(Collections.singleton(0), months, weekdays));
            }

            out.add(rule);
        }

        return out;
    }

    private static ActRule alwaysRule(UUID collectionId, String name) {
        ActRule rule = new ActRule(UUID.randomUUID(), collectionId, name);
        rule.setDateRange("*");
        rule.setDuration("*");
        rule.setCronExpression("NONE");
        rule.setEnabled(true);
        return rule;
    }

    private static ActRule hoursRule(UUID collectionId, String name, Set<Integer> hours) {
        ActRule rule = new ActRule(UUID.randomUUID(), collectionId, name);
        rule.setDateRange("*");
        rule.setDuration("1h");
        rule.setCronExpression(buildCron(hours, Collections.emptySet(), Collections.emptySet()));
        return rule;
    }

    private static ActRule weekdayRule(UUID collectionId, String name, Set<String> weekdays) {
        ActRule rule = new ActRule(UUID.randomUUID(), collectionId, name);
        rule.setDateRange("*");
        rule.setDuration("24h");
        rule.setCronExpression(buildCron(Collections.singleton(0), Collections.emptySet(), weekdays));
        return rule;
    }

    private static ActRule monthRule(UUID collectionId, String name, Set<Integer> months) {
        ActRule rule = new ActRule(UUID.randomUUID(), collectionId, name);
        rule.setDateRange("*");
        rule.setDuration("24h");
        rule.setCronExpression(buildCron(Collections.singleton(0), months, Collections.emptySet()));
        return rule;
    }

    private static List<ActRule> dateRangeRules(UUID collectionId, String name, List<LocalDateRange> ranges) {
        List<ActRule> rules = new ArrayList<>();
        for (LocalDateRange r : ranges) {
            ActRule rule = new ActRule(UUID.randomUUID(), collectionId, name + " " + r.start + ".." + r.end);
            rule.setDateRange(r.toActDateRange());
            rule.setDuration("*");
            rule.setCronExpression("NONE");
            rules.add(rule);
        }
        return rules;
    }

    private static String buildCron(Set<Integer> hours, Set<Integer> months, Set<String> weekdays) {
        // Quartz: sec min hour dom month dow
        String sec = "0";
        String min = "0";
        String hour = joinInts(hours, "*");

        String month = joinInts(months, "*");

        final String dom;
        final String dow;
        if (weekdays != null && !weekdays.isEmpty()) {
            dom = "?";
            dow = String.join(",", weekdays);
        } else {
            dom = "*";
            dow = "?";
        }

        return String.format("%s %s %s %s %s %s", sec, min, hour, dom, month, dow);
    }

    private static String joinInts(Set<Integer> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        List<Integer> sorted = values.stream().sorted().collect(Collectors.toList());
        return sorted.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static Set<Integer> readIntKeys(ConfigurationSection sec, int min, int max) {
        if (sec == null) return Collections.emptySet();
        Set<Integer> out = new HashSet<>();
        for (String k : sec.getKeys(false)) {
            if (!sec.getBoolean(k)) continue;
            try {
                int v = Integer.parseInt(k);
                if (v >= min && v <= max) {
                    out.add(v);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    private static Set<String> readWeekdays(ConfigurationSection sec) {
        if (sec == null) return Collections.emptySet();
        Set<String> out = new HashSet<>();
        for (String k : sec.getKeys(false)) {
            if (!sec.getBoolean(k)) continue;
            String norm = k.trim().toUpperCase(Locale.ROOT);
            // Accept MONDAY/MON/etc.
            if (norm.startsWith("MON")) out.add("MON");
            else if (norm.startsWith("TUE")) out.add("TUE");
            else if (norm.startsWith("WED")) out.add("WED");
            else if (norm.startsWith("THU")) out.add("THU");
            else if (norm.startsWith("FRI")) out.add("FRI");
            else if (norm.startsWith("SAT")) out.add("SAT");
            else if (norm.startsWith("SUN")) out.add("SUN");
        }
        // stable ordering (Quartz accepts any order, but nicer for diffs)
        return out.stream()
            .sorted(Comparator.comparingInt(LegacyActRuleTranslator::weekdayOrder))
            .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static int weekdayOrder(String s) {
        switch (s) {
            case "MON": return 1;
            case "TUE": return 2;
            case "WED": return 3;
            case "THU": return 4;
            case "FRI": return 5;
            case "SAT": return 6;
            case "SUN": return 7;
            default: return 99;
        }
    }

    private static Set<LegacySeason> readSeasons(ConfigurationSection sec) {
        if (sec == null) return Collections.emptySet();
        Map<LegacySeason, Boolean> map = new EnumMap<>(LegacySeason.class);
        for (String k : sec.getKeys(false)) {
            if (!sec.getBoolean(k)) continue;
            String norm = k.trim().toUpperCase(Locale.ROOT);
            if (norm.startsWith("WIN")) map.put(LegacySeason.WINTER, true);
            else if (norm.startsWith("SPR")) map.put(LegacySeason.SPRING, true);
            else if (norm.startsWith("SUM")) map.put(LegacySeason.SUMMER, true);
            else if (norm.startsWith("FAL") || norm.startsWith("AUT")) map.put(LegacySeason.FALL, true);
        }
        return map.keySet();
    }

    private static Set<Integer> seasonsToMonths(Set<LegacySeason> seasons) {
        if (seasons == null || seasons.isEmpty()) return Collections.emptySet();
        Set<Integer> months = new HashSet<>();
        for (LegacySeason s : seasons) {
            switch (s) {
                case WINTER:
                    months.add(12);
                    months.add(1);
                    months.add(2);
                    break;
                case SPRING:
                    months.add(3);
                    months.add(4);
                    months.add(5);
                    break;
                case SUMMER:
                    months.add(6);
                    months.add(7);
                    months.add(8);
                    break;
                case FALL:
                    months.add(9);
                    months.add(10);
                    months.add(11);
                    break;
            }
        }
        return months;
    }

    private static List<LocalDateRange> readDateRanges(ConfigurationSection sec) {
        if (sec == null) return Collections.emptyList();

        List<LocalDate> dates = new ArrayList<>();
        for (String key : sec.getKeys(false)) {
            if (!sec.getBoolean(key)) continue;
            try {
                dates.add(LocalDate.parse(key, ISO));
            } catch (DateTimeParseException ignored) {
            }
        }
        if (dates.isEmpty()) return Collections.emptyList();

        dates.sort(Comparator.naturalOrder());
        List<LocalDateRange> ranges = new ArrayList<>();

        LocalDate start = dates.get(0);
        LocalDate prev = start;
        for (int i = 1; i < dates.size(); i++) {
            LocalDate d = dates.get(i);
            if (d.equals(prev.plusDays(1))) {
                prev = d;
                continue;
            }
            ranges.add(new LocalDateRange(start, prev));
            start = d;
            prev = d;
        }
        ranges.add(new LocalDateRange(start, prev));

        return ranges;
    }

    private static final class LocalDateRange {
        static final LocalDateRange ANY = new LocalDateRange(null, null);

        final LocalDate start;
        final LocalDate end;

        LocalDateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }

        String toActDateRange() {
            if (start == null || end == null) {
                return "*";
            }
            return start.format(ISO) + ":" + end.format(ISO);
        }
    }
}
