package io.github.johnnyscriptt.materialchecklist.core.search;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchRulesTest {
    @Test
    void radiusIsClampedToTheSupportedRange() {
        assertEquals(SearchRules.MIN_RADIUS, SearchRules.clampRadius(0));
        assertEquals(SearchRules.MIN_RADIUS, SearchRules.clampRadius(-100));
        assertEquals(16, SearchRules.clampRadius(16));
        assertEquals(SearchRules.MAX_RADIUS, SearchRules.clampRadius(1000));
    }

    @Test
    void firstRequestIsAlwaysAllowed() {
        assertTrue(SearchRules.cooldownOk(SearchRules.NEVER_REQUESTED, 0));
        assertTrue(SearchRules.cooldownOk(SearchRules.NEVER_REQUESTED, 5_000));
    }

    @Test
    void repeatRequestIsBlockedUntilTheCooldownElapses() {
        assertFalse(SearchRules.cooldownOk(100, 100));
        assertFalse(SearchRules.cooldownOk(100, 109));
        assertTrue(SearchRules.cooldownOk(100, 110));
        assertTrue(SearchRules.cooldownOk(100, 500));
    }

    @Test
    void tickCounterGoingBackwardsDoesNotLockThePlayerOut() {
        // Server restart resets the tick counter; a stale future timestamp must not stick.
        assertTrue(SearchRules.cooldownOk(9_000, 5));
    }

    @Test
    void resultsAreTruncatedToTheCap() {
        List<Integer> tooMany = new ArrayList<>();
        for (int i = 0; i < SearchRules.MAX_RESULTS + 50; i++) {
            tooMany.add(i);
        }
        List<Integer> truncated = SearchRules.truncate(tooMany);
        assertEquals(SearchRules.MAX_RESULTS, truncated.size());
        assertEquals(0, truncated.get(0));
        assertEquals(SearchRules.MAX_RESULTS - 1, truncated.get(SearchRules.MAX_RESULTS - 1));
    }

    @Test
    void shortResultListIsReturnedUntouched() {
        List<Integer> few = List.of(1, 2, 3);
        assertSame(few, SearchRules.truncate(few));
    }
}
