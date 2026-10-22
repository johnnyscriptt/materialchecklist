package io.github.johnnyscriptt.materialchecklist.core.list;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectTest {
    private static final Project HOUSE = new Project("p1", "House", List.of(
            new Entry("minecraft:glass", 10, false),
            new Entry("minecraft:oak_log", 5, true)));

    @Test
    void mergingAddsToExistingItemsAndAppendsNewOnes() {
        Project merged = HOUSE.merged(List.of(
                new Entry("minecraft:oak_log", 3, false),
                new Entry("minecraft:lantern", 4, false)));
        assertEquals(List.of(
                new Entry("minecraft:glass", 10, false),
                new Entry("minecraft:oak_log", 8, true),
                new Entry("minecraft:lantern", 4, false)), merged.entries());
    }

    @Test
    void replacingAndRemovingAnEntryKeepsTheRestInOrder() {
        Project edited = HOUSE.withEntry(new Entry("minecraft:glass", 64, false));
        assertEquals(64, edited.entries().getFirst().needed());
        Project removed = edited.without("minecraft:glass");
        assertEquals(List.of(new Entry("minecraft:oak_log", 5, true)), removed.entries());
    }

    @Test
    void entriesCannotBeChangedFromOutside() {
        List<Entry> source = new ArrayList<>(HOUSE.entries());
        Project project = new Project("p2", "Copy", source);
        source.clear();
        assertEquals(2, project.entries().size());
        assertThrows(UnsupportedOperationException.class, () -> project.entries().clear());
    }

    @Test
    void aMissingEntryListLoadsAsEmpty() {
        assertTrue(new Project("p3", "Blank", null).entries().isEmpty());
    }

    @Test
    void aBlankNameFallsBackToUntitled() {
        assertEquals("Untitled", new Project("p4", "  ", List.of()).name());
    }
}
