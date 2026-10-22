package io.github.johnnyscriptt.materialchecklist.core.search;

import java.util.List;

/**
 * Limits the server enforces on every search request, kept in one place so the client and
 * the server agree on what a legal search looks like.
 */
public final class SearchRules {
    public static final int MIN_RADIUS = 8;
    public static final int MAX_RADIUS = 64;
    public static final int DEFAULT_RADIUS = 16;

    /** Past this many containers the highlights are visual noise anyway. */
    public static final int MAX_RESULTS = 512;

    /** Half a second between searches — enough to stop a held mouse button spamming scans. */
    public static final int COOLDOWN_TICKS = 10;

    public static final long NEVER_REQUESTED = Long.MIN_VALUE;

    private SearchRules() {
    }

    public static int clampRadius(int radius) {
        return Math.clamp(radius, MIN_RADIUS, MAX_RADIUS);
    }

    public static boolean cooldownOk(long lastTick, long nowTick) {
        if (lastTick == NEVER_REQUESTED || nowTick < lastTick) {
            // A tick counter that went backwards means the server restarted; don't strand
            // the player behind a timestamp that will never elapse.
            return true;
        }
        return nowTick - lastTick >= COOLDOWN_TICKS;
    }

    public static <T> List<T> truncate(List<T> results) {
        return results.size() <= MAX_RESULTS ? results : List.copyOf(results.subList(0, MAX_RESULTS));
    }
}
