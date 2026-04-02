package de.theredend2000.advancedhunt.migration.legacy;

import de.theredend2000.advancedhunt.model.ActRule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LegacyActRuleTranslatorTest {

    private static LegacyEggsParser.LegacyRequirements requirements(String order, YamlConfiguration raw) throws Exception {
        Constructor<LegacyEggsParser.LegacyRequirements> constructor =
            LegacyEggsParser.LegacyRequirements.class.getDeclaredConstructor(String.class, ConfigurationSection.class);
        constructor.setAccessible(true);
        return constructor.newInstance(order, raw);
    }

    private static void enable(ConfigurationSection root, String sectionName, String key) {
        ConfigurationSection section = root.getConfigurationSection(sectionName);
        if (section == null) {
            section = root.createSection(sectionName);
        }
        section.set(key, true);
    }

    private static Set<String> actFormats(List<ActRule> rules) {
        return rules.stream().map(ActRule::getActFormat).collect(Collectors.toSet());
    }

    @Test
    public void translateReturnsAlwaysAvailableRuleWhenRequirementsAreMissing() {
        List<ActRule> rules = LegacyActRuleTranslator.translate(UUID.randomUUID(), null);

        assertEquals(1, rules.size());
        assertEquals("[*] [*] [NONE]", rules.get(0).getActFormat());
        assertTrue(rules.get(0).isEnabled());
    }

    @Test
    public void translateOrCreatesSeparateRulesForIndependentConstraintCategories() throws Exception {
        YamlConfiguration raw = new YamlConfiguration();
        enable(raw, "Hours", "9");
        enable(raw, "Weekday", "Monday");
        enable(raw, "Month", "12");

        List<ActRule> rules = LegacyActRuleTranslator.translate(UUID.randomUUID(), requirements("OR", raw));
        Set<String> formats = actFormats(rules);

        assertEquals(3, rules.size());
        assertTrue(formats.contains("[*] [1h] [0 0 9 * * ?]"));
        assertTrue(formats.contains("[*] [24h] [0 0 0 ? * MON]"));
        assertTrue(formats.contains("[*] [24h] [0 0 0 * 12 ?]"));
    }

    @Test
    public void translateOrCompactsConsecutiveDatesIntoDateRanges() throws Exception {
        YamlConfiguration raw = new YamlConfiguration();
        enable(raw, "Date", "2026-01-10");
        enable(raw, "Date", "2026-01-11");
        enable(raw, "Date", "2026-01-13");

        List<ActRule> rules = LegacyActRuleTranslator.translate(UUID.randomUUID(), requirements("OR", raw));
        Set<String> formats = actFormats(rules);

        assertEquals(2, rules.size());
        assertTrue(formats.contains("[2026-01-10:2026-01-11] [*] [NONE]"));
        assertTrue(formats.contains("[2026-01-13:2026-01-13] [*] [NONE]"));
    }

    @Test
    public void translateAndBuildsSingleRulePerDateRangeWithCombinedConstraints() throws Exception {
        YamlConfiguration raw = new YamlConfiguration();
        enable(raw, "Hours", "9");
        enable(raw, "Weekday", "Monday");
        enable(raw, "Month", "12");
        enable(raw, "Date", "2026-12-24");
        enable(raw, "Date", "2026-12-25");

        List<ActRule> rules = LegacyActRuleTranslator.translate(UUID.randomUUID(), requirements("AND", raw));

        assertEquals(1, rules.size());
        assertEquals("[2026-12-24:2026-12-25] [1h] [0 0 9 ? 12 MON]", rules.get(0).getActFormat());
    }

    @Test
    public void translateNormalizesAndOrdersWeekdaysForQuartz() throws Exception {
        YamlConfiguration raw = new YamlConfiguration();
        enable(raw, "Weekday", "Sunday");
        enable(raw, "Weekday", "monday");
        enable(raw, "Weekday", "Wednesday");

        List<ActRule> rules = LegacyActRuleTranslator.translate(UUID.randomUUID(), requirements("OR", raw));

        assertEquals(1, rules.size());
        assertEquals("[*] [24h] [0 0 0 ? * MON,WED,SUN]", rules.get(0).getActFormat());
    }

    @Test
    public void translateIntersectsExplicitMonthsWithSeasonMonths() throws Exception {
        YamlConfiguration raw = new YamlConfiguration();
        enable(raw, "Month", "1");
        enable(raw, "Month", "6");
        enable(raw, "Season", "winter");

        List<ActRule> rules = LegacyActRuleTranslator.translate(UUID.randomUUID(), requirements("OR", raw));

        assertEquals(1, rules.size());
        assertEquals("[*] [24h] [0 0 0 * 1 ?]", rules.get(0).getActFormat());
    }
}