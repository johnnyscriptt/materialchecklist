package io.github.johnnyscriptt.materialchecklist.core.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PulseTest {
    private static final float EPS = 1e-4f;

    @Test
    void startsHalfwayBetweenFloorAndPeak() {
        assertEquals(0.8f, Pulse.at(0L, 900L, 0.6f), EPS);
    }

    @Test
    void peaksAQuarterOfTheWayThroughThePeriod() {
        assertEquals(1.0f, Pulse.at(225L, 900L, 0.6f), EPS);
    }

    @Test
    void bottomsOutAtTheFloorThreeQuartersThrough() {
        assertEquals(0.6f, Pulse.at(675L, 900L, 0.6f), EPS);
    }

    @Test
    void repeatsEveryPeriod() {
        assertEquals(Pulse.at(100L, 900L, 0.6f), Pulse.at(1000L, 900L, 0.6f), EPS);
    }

    @Test
    void neverLeavesTheFloorToPeakBand() {
        for (long t = 0; t < 2_000L; t += 7L) {
            float value = Pulse.at(t, 900L, 0.3f);
            assertTrue(value >= 0.3f - EPS && value <= 1.0f + EPS, "t=" + t + " gave " + value);
        }
    }
}
