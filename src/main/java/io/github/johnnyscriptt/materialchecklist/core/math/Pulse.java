package io.github.johnnyscriptt.materialchecklist.core.math;

/**
 * The shared brighten/dim cycle. The world overlay and the in-chest slot glow both read it, so
 * a chest and the items inside it breathe in step.
 */
public final class Pulse {

    private Pulse() {
    }

    /** Smooth cycle between {@code floor} and 1, starting halfway and rising. */
    public static float at(long nowMs, long periodMs, float floor) {
        double phase = 2.0 * Math.PI * (nowMs % periodMs) / periodMs;
        float wave = (float) (0.5 + 0.5 * Math.sin(phase));
        return floor + (1.0f - floor) * wave;
    }
}
