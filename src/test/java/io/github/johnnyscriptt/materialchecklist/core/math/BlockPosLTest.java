package io.github.johnnyscriptt.materialchecklist.core.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockPosLTest {
    @Test
    void roundTripsPositiveCoordinates() {
        long packed = BlockPosL.pack(120, 64, 3000);
        assertEquals(120, BlockPosL.unpackX(packed));
        assertEquals(64, BlockPosL.unpackY(packed));
        assertEquals(3000, BlockPosL.unpackZ(packed));
    }

    @Test
    void roundTripsNegativeCoordinates() {
        long packed = BlockPosL.pack(-1200, -60, -45);
        assertEquals(-1200, BlockPosL.unpackX(packed));
        assertEquals(-60, BlockPosL.unpackY(packed));
        assertEquals(-45, BlockPosL.unpackZ(packed));
    }

    @Test
    void roundTripsWorldHeightExtremes() {
        long packed = BlockPosL.pack(0, 319, 0);
        assertEquals(319, BlockPosL.unpackY(packed));

        long deep = BlockPosL.pack(0, -64, 0);
        assertEquals(-64, BlockPosL.unpackY(deep));
    }

    @Test
    void distinctPositionsProduceDistinctKeys() {
        assertEquals(BlockPosL.pack(1, 2, 3), BlockPosL.pack(1, 2, 3));
        long a = BlockPosL.pack(1, 2, 3);
        long b = BlockPosL.pack(3, 2, 1);
        org.junit.jupiter.api.Assertions.assertNotEquals(a, b);
    }
}
