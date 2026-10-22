package io.github.johnnyscriptt.materialchecklist.core.list;

/**
 * One line of a material list: an item by registry id, how many the build needs, and whether
 * the player has ticked it off by hand (already placed, or sourced some other way).
 */
public record Entry(String itemId, long needed, boolean manuallyChecked) {

    public Entry withNeeded(long newNeeded) {
        return new Entry(itemId, Math.max(1, newNeeded), manuallyChecked);
    }

    public Entry toggled() {
        return new Entry(itemId, needed, !manuallyChecked);
    }
}
