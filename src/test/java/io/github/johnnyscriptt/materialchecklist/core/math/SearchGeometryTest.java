package io.github.johnnyscriptt.materialchecklist.core.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchGeometryTest {
    @Test
    void chunkBoundsCoverTheRadiusAroundOrigin() {
        assertEquals(-1, SearchGeometry.chunkMin(0, 8));
        assertEquals(0, SearchGeometry.chunkMax(0, 8));
    }

    @Test
    void chunkBoundsFloorCorrectlyForNegativeCoordinates() {
        // -20 sits in chunk -2 (blocks -32..-17); -20 - 8 = -28 is still chunk -2.
        assertEquals(-2, SearchGeometry.chunkMin(-20, 8));
        // -20 + 8 = -12 is chunk -1.
        assertEquals(-1, SearchGeometry.chunkMax(-20, 8));
    }

    @Test
    void chunkBoundsSpanMultipleChunksForLargeRadius() {
        assertEquals(-4, SearchGeometry.chunkMin(0, 64));
        assertEquals(4, SearchGeometry.chunkMax(0, 64));
    }

    @Test
    void blockAtPlayerPositionIsWithinAnyRadius() {
        assertTrue(SearchGeometry.withinSphere(10, 64, 10, 10.5, 64.5, 10.5, 8));
    }

    @Test
    void blockJustInsideRadiusMatchesAndJustOutsideDoesNot() {
        // Block centre (0.5, 64.5, 0.5); player at the same centre offset by 8 on X.
        assertTrue(SearchGeometry.withinSphere(8, 64, 0, 0.5, 64.5, 0.5, 8));
        assertFalse(SearchGeometry.withinSphere(9, 64, 0, 0.5, 64.5, 0.5, 8));
    }

    @Test
    void distanceIsSphericalNotCubic() {
        // Corner of the bounding cube: (8, 8) away on two axes is ~11.3 blocks — outside r=8.
        assertFalse(SearchGeometry.withinSphere(8, 64, 8, 0.5, 64.5, 0.5, 8));
    }

    @Test
    void verticalDistanceCounts() {
        assertTrue(SearchGeometry.withinSphere(0, 70, 0, 0.5, 64.5, 0.5, 8));
        assertFalse(SearchGeometry.withinSphere(0, 80, 0, 0.5, 64.5, 0.5, 8));
    }
}
