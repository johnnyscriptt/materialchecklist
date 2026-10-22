package io.github.johnnyscriptt.materialchecklist.core.search;

/**
 * The boxes currently glowing, plus everything needed to decide when to stop drawing them:
 * the dimension they were found in and when they arrived.
 *
 * <p>A highlight is a block <em>range</em> rather than a single position so that a double
 * chest draws as one rectangle around both halves instead of two cubes with a seam down the
 * middle. Most boxes are a single block, where the min and max corners are equal.
 */
public final class HighlightState {
    /** Highlights spend their last few seconds fading rather than blinking out. */
    public static final long FADE_MS = 3_000L;

    public static final HighlightState EMPTY =
            new HighlightState(new long[0], new long[0], "", 0L, 0L);

    private final long[] mins;
    private final long[] maxs;
    private final String dimensionKey;
    private final long createdAtMs;
    private final long lifetimeMs;

    public HighlightState(long[] mins, long[] maxs, String dimensionKey, long createdAtMs,
                          long lifetimeMs) {
        if (mins.length != maxs.length) {
            throw new IllegalArgumentException(
                    "every highlight needs both corners: " + mins.length + " vs " + maxs.length);
        }
        this.mins = mins.clone();
        this.maxs = maxs.clone();
        this.dimensionKey = dimensionKey;
        this.createdAtMs = createdAtMs;
        this.lifetimeMs = lifetimeMs;
    }

    /** Packed lower corner of each box. */
    public long[] mins() {
        return mins.clone();
    }

    /** Packed upper corner of each box, inclusive — equal to the min for a single block. */
    public long[] maxs() {
        return maxs.clone();
    }

    /** Number of boxes drawn, which is what the player reads as "containers". */
    public int count() {
        return mins.length;
    }

    public boolean isEmpty() {
        return mins.length == 0;
    }

    public boolean expired(long nowMs) {
        return nowMs - createdAtMs >= lifetimeMs;
    }

    /** True when these highlights should still be drawn for a player in {@code dimensionKey}. */
    public boolean visibleIn(String currentDimensionKey, long nowMs) {
        return !isEmpty() && !expired(nowMs) && dimensionKey.equals(currentDimensionKey);
    }

    public float fadeAlpha(long nowMs) {
        long remaining = createdAtMs + lifetimeMs - nowMs;
        if (remaining <= 0L) {
            return 0.0f;
        }
        long fadeWindow = Math.min(FADE_MS, lifetimeMs);
        if (fadeWindow <= 0L || remaining >= fadeWindow) {
            return 1.0f;
        }
        return (float) remaining / fadeWindow;
    }
}
