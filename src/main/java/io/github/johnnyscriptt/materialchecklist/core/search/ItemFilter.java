package io.github.johnnyscriptt.materialchecklist.core.search;

import java.util.List;
import java.util.Locale;

/**
 * Creative-menu style filtering: a query matches an item when it appears either in the
 * localized name ("diamond sw") or in the registry id ("diamond_sword"), so both the way
 * players read an item and the way they type it work.
 */
public final class ItemFilter {
    private ItemFilter() {
    }

    public static List<ItemEntry> filter(List<ItemEntry> entries, String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return entries;
        }
        return entries.stream().filter(entry -> matches(entry, normalized)).toList();
    }

    /** @param normalizedQuery a non-empty query already passed through {@link #normalize}. */
    public static boolean matches(ItemEntry entry, String normalizedQuery) {
        return entry.displayName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || entry.id().toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    public static String normalize(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }
}
