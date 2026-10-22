package io.github.johnnyscriptt.materialchecklist.core.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemFilterTest {
    private static final ItemEntry DIAMOND = new ItemEntry("minecraft:diamond", "Diamond");
    private static final ItemEntry DIAMOND_SWORD =
            new ItemEntry("minecraft:diamond_sword", "Diamond Sword");
    private static final ItemEntry OAK_PLANKS = new ItemEntry("minecraft:oak_planks", "Oak Planks");
    private static final ItemEntry CHEST = new ItemEntry("minecraft:chest", "Chest");
    private static final List<ItemEntry> ALL = List.of(DIAMOND, DIAMOND_SWORD, OAK_PLANKS, CHEST);

    @Test
    void emptyQueryReturnsEverythingInOrder() {
        assertEquals(ALL, ItemFilter.filter(ALL, ""));
        assertEquals(ALL, ItemFilter.filter(ALL, "   "));
        assertEquals(ALL, ItemFilter.filter(ALL, null));
    }

    @Test
    void matchesDisplayNameSubstringCaseInsensitively() {
        assertEquals(List.of(DIAMOND, DIAMOND_SWORD), ItemFilter.filter(ALL, "diam"));
        assertEquals(List.of(DIAMOND, DIAMOND_SWORD), ItemFilter.filter(ALL, "DIAM"));
        assertEquals(List.of(DIAMOND_SWORD), ItemFilter.filter(ALL, "diamond sw"));
    }

    @Test
    void matchesRegistryIdSoUnderscoreQueriesWork() {
        assertEquals(List.of(DIAMOND_SWORD), ItemFilter.filter(ALL, "diamond_sword"));
        assertEquals(List.of(OAK_PLANKS), ItemFilter.filter(ALL, "oak_pl"));
    }

    @Test
    void queryIsTrimmedBeforeMatching() {
        assertEquals(List.of(CHEST), ItemFilter.filter(ALL, "  chest  "));
    }

    @Test
    void nonMatchingQueryReturnsEmptyList() {
        assertTrue(ItemFilter.filter(ALL, "netherite").isEmpty());
    }

    @Test
    void namespaceQueryMatchesEverythingInThatNamespace() {
        assertEquals(ALL, ItemFilter.filter(ALL, "minecraft:"));
    }
}
