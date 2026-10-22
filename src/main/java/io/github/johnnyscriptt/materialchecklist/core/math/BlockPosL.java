package io.github.johnnyscriptt.materialchecklist.core.math;

/**
 * Block positions packed into a single long (x:26 bits, z:26, y:12 — same split
 * as vanilla) so result sets are cheap primitive arrays.
 */
public final class BlockPosL {
    private static final int X_BITS = 26;
    private static final int Z_BITS = 26;
    private static final int Y_BITS = 12;

    private BlockPosL() {
    }

    public static long pack(int x, int y, int z) {
        return ((x & ((1L << X_BITS) - 1)) << (Z_BITS + Y_BITS))
                | ((z & ((1L << Z_BITS) - 1)) << Y_BITS)
                | (y & ((1L << Y_BITS) - 1));
    }

    public static int unpackX(long packed) {
        return (int) (packed << (64 - X_BITS - Z_BITS - Y_BITS) >> (64 - X_BITS));
    }

    public static int unpackZ(long packed) {
        return (int) (packed << (64 - Z_BITS - Y_BITS) >> (64 - Z_BITS));
    }

    public static int unpackY(long packed) {
        return (int) (packed << (64 - Y_BITS) >> (64 - Y_BITS));
    }
}
