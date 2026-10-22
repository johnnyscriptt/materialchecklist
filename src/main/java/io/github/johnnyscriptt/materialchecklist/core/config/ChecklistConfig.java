package io.github.johnnyscriptt.materialchecklist.core.config;

import io.github.johnnyscriptt.materialchecklist.core.search.SearchRules;

/**
 * Player settings, saved as JSON. Plain public fields so Gson can read a file written by an
 * older version: anything missing arrives as zero or null, which {@link #clamp} reads as
 * "use the default".
 */
public final class ChecklistConfig {
    public static final int DEFAULT_ROWS = 8;
    public static final int MIN_ROWS = 3;
    public static final int MAX_ROWS = 20;
    public static final int DEFAULT_HIGHLIGHT_SECONDS = 30;

    public enum Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT;

        public Corner next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public boolean right() {
            return this == TOP_RIGHT || this == BOTTOM_RIGHT;
        }

        public boolean bottom() {
            return this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
        }
    }

    public boolean hudVisible = true;
    /** Top right, because MiniHUD's info lines and the chat own the left side by default. */
    public Corner hudCorner = Corner.TOP_RIGHT;
    public int hudRows = DEFAULT_ROWS;
    public boolean hideCompleted = false;
    /** Show amounts as "3 st + 5" rather than "197". */
    public boolean stackNotation = true;
    public int radius = SearchRules.DEFAULT_RADIUS;
    public int highlightSeconds = DEFAULT_HIGHLIGHT_SECONDS;
    /** Whether the ender chest counts toward the list (the server knows it without opening). */
    public boolean countEnderChest = true;
    /** Ring the slots holding still-missing items whenever a container is open. */
    public boolean glowMissingSlots = true;

    public void clamp() {
        hudRows = hudRows == 0 ? DEFAULT_ROWS : Math.clamp(hudRows, MIN_ROWS, MAX_ROWS);
        radius = radius == 0 ? SearchRules.DEFAULT_RADIUS : SearchRules.clampRadius(radius);
        highlightSeconds = highlightSeconds <= 0 ? DEFAULT_HIGHLIGHT_SECONDS : Math.min(highlightSeconds, 600);
        if (hudCorner == null) {
            hudCorner = Corner.TOP_RIGHT;
        }
    }

    public long highlightLifetimeMs() {
        return highlightSeconds * 1000L;
    }
}
