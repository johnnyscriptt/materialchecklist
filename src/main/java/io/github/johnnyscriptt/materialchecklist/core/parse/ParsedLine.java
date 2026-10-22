package io.github.johnnyscriptt.materialchecklist.core.parse;

/**
 * One line of imported text as the parser understood it, before the player accepts it.
 *
 * @param raw   the line as it arrived, shown in the review screen next to the guess
 * @param name  the part read as the item name
 * @param count the amount, or {@link #NO_COUNT} if the line had none
 * @param match the item the name resolved to, or null if nothing was close
 */
public record ParsedLine(String raw, String name, long count, ItemNameMatcher.Match match) {
    public static final long NO_COUNT = -1;

    /** True when the player should look at this line before it goes into the list. */
    public boolean needsReview() {
        return match == null || !match.confident() || count == NO_COUNT;
    }
}
