package io.github.johnnyscriptt.materialchecklist.core.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HighlightStateTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";
    private static final long LIFETIME = 30_000L;

    private static HighlightState state(long createdAtMs) {
        return new HighlightState(new long[]{1L, 2L, 3L}, new long[]{1L, 2L, 3L}, OVERWORLD,
                createdAtMs, LIFETIME);
    }

    @Test
    void emptyStateIsNeverVisible() {
        assertTrue(HighlightState.EMPTY.isEmpty());
        assertFalse(HighlightState.EMPTY.visibleIn(OVERWORLD, 0));
    }

    @Test
    void freshStateIsVisibleInItsOwnDimension() {
        HighlightState s = state(1_000);
        assertFalse(s.isEmpty());
        assertEquals(3, s.count());
        assertTrue(s.visibleIn(OVERWORLD, 1_000));
        assertTrue(s.visibleIn(OVERWORLD, 29_000));
    }

    @Test
    void stateExpiresAfterItsLifetime() {
        HighlightState s = state(0);
        assertFalse(s.expired(29_999));
        assertTrue(s.expired(30_000));
        assertFalse(s.visibleIn(OVERWORLD, 30_000));
    }

    @Test
    void stateIsHiddenInADifferentDimension() {
        HighlightState s = state(0);
        assertFalse(s.visibleIn(NETHER, 1_000));
    }

    @Test
    void alphaIsFullUntilTheFadeWindowThenRampsToZero() {
        HighlightState s = state(0);
        assertEquals(1.0f, s.fadeAlpha(0), 0.001f);
        assertEquals(1.0f, s.fadeAlpha(27_000), 0.001f);
        assertEquals(0.5f, s.fadeAlpha(28_500), 0.001f);
        assertEquals(0.0f, s.fadeAlpha(30_000), 0.001f);
        assertEquals(0.0f, s.fadeAlpha(99_000), 0.001f);
    }

    @Test
    void shortLifetimeStillFadesWithoutGoingNegative() {
        HighlightState s = new HighlightState(new long[]{7L}, new long[]{7L}, OVERWORLD, 0, 1_000);
        assertTrue(s.fadeAlpha(0) <= 1.0f);
        assertTrue(s.fadeAlpha(500) > 0.0f);
        assertEquals(0.0f, s.fadeAlpha(1_000), 0.001f);
    }

    @Test
    void boundsAreDefensivelyCopied() {
        long[] mins = {1L, 2L};
        long[] maxs = {3L, 4L};
        HighlightState s = new HighlightState(mins, maxs, OVERWORLD, 0, LIFETIME);
        mins[0] = 99L;
        maxs[0] = 99L;
        assertEquals(1L, s.mins()[0]);
        assertEquals(3L, s.maxs()[0]);
        s.mins()[0] = 42L;
        s.maxs()[0] = 42L;
        assertEquals(1L, s.mins()[0]);
        assertEquals(3L, s.maxs()[0]);
    }

    @Test
    void aBoxSpanningTwoBlocksCountsAsOneHighlight() {
        // A double chest is one box covering both halves, not two highlights.
        HighlightState s = new HighlightState(new long[]{1L}, new long[]{2L}, OVERWORLD, 0, LIFETIME);
        assertEquals(1, s.count());
        assertEquals(1L, s.mins()[0]);
        assertEquals(2L, s.maxs()[0]);
    }

    @Test
    void mismatchedBoundsAreRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new HighlightState(new long[]{1L, 2L}, new long[]{1L}, OVERWORLD, 0, LIFETIME));
    }
}
