package io.github.johnnyscriptt.materialchecklist.core.math;

/**
 * The maths behind a radius search: which chunk columns the sphere touches, and whether a
 * given block actually falls inside it.
 */
public final class SearchGeometry {
    private static final int CHUNK_SHIFT = 4;

    private SearchGeometry() {
    }

    /** Lowest chunk coordinate the search sphere can reach on one axis. */
    public static int chunkMin(int blockCoord, int radius) {
        return (blockCoord - radius) >> CHUNK_SHIFT;
    }

    /** Highest chunk coordinate the search sphere can reach on one axis. */
    public static int chunkMax(int blockCoord, int radius) {
        return (blockCoord + radius) >> CHUNK_SHIFT;
    }

    /**
     * Sphere test against the block's centre, so a radius of 8 means "8 blocks in every
     * direction" rather than the corners of a 16-wide cube reaching ~13.8.
     */
    public static boolean withinSphere(int bx, int by, int bz, double px, double py, double pz,
                                       int radius) {
        double dx = bx + 0.5 - px;
        double dy = by + 0.5 - py;
        double dz = bz + 0.5 - pz;
        return dx * dx + dy * dy + dz * dz <= (double) radius * radius;
    }
}
