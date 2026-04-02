package de.theredend2000.advancedhunt.managers;

import de.theredend2000.advancedhunt.model.ActRule;
import de.theredend2000.advancedhunt.model.Collection;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ActRuleEvaluatorTest {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private static String dateRange(LocalDate start, LocalDate end) {
        return start.toString() + ":" + end.toString();
    }

    private static ActRule noneRule(LocalDate start, LocalDate end) {
        ActRule rule = new ActRule(UUID.randomUUID(), UUID.randomUUID(), "rule");
        rule.setDateRange(dateRange(start, end));
        rule.setDuration("*");
        rule.setCronExpression("NONE");
        return rule;
    }

    private static Collection collection(boolean enabled, ActRule... rules) {
        Collection collection = new Collection(UUID.randomUUID(), "Test Collection", enabled);
        collection.setActRules(java.util.Arrays.asList(rules));
        return collection;
    }

    @Test
    public void enabledCollectionWithoutRulesIsAvailable() {
        ActRuleEvaluator evaluator = new ActRuleEvaluator(null);

        assertTrue(evaluator.isCollectionAvailable(collection(true)));
    }

    @Test
    public void disabledCollectionIsUnavailableEvenWhenRuleIsActive() {
        LocalDate today = LocalDate.now(ZONE);
        ActRuleEvaluator evaluator = new ActRuleEvaluator(null);

        assertFalse(evaluator.isCollectionAvailable(collection(false, noneRule(today, today))));
    }

    @Test
    public void anyActiveRuleMakesCollectionAvailable() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate tomorrow = today.plusDays(1);
        ActRuleEvaluator evaluator = new ActRuleEvaluator(null);

        Collection collection = collection(true, noneRule(tomorrow, tomorrow), noneRule(today, today));

        assertTrue(evaluator.isCollectionAvailable(collection));
    }

    @Test
    public void allInactiveRulesMakeCollectionUnavailable() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfterTomorrow = today.plusDays(2);
        ActRuleEvaluator evaluator = new ActRuleEvaluator(null);

        Collection collection = collection(true, noneRule(tomorrow, tomorrow), noneRule(dayAfterTomorrow, dayAfterTomorrow));

        assertFalse(evaluator.isCollectionAvailable(collection));
    }

    @Test
    public void clearAvailabilityCacheLetsUpdatedRulesTakeEffectImmediately() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate tomorrow = today.plusDays(1);
        ActRuleEvaluator evaluator = new ActRuleEvaluator(null);
        Collection collection = collection(true, noneRule(today, today));

        assertTrue(evaluator.isCollectionAvailable(collection));

        collection.setActRules(Collections.singletonList(noneRule(tomorrow, tomorrow)));

        assertTrue(evaluator.isCollectionAvailable(collection));

        evaluator.clearAvailabilityCache(collection.getId());

        assertFalse(evaluator.isCollectionAvailable(collection));
    }
}