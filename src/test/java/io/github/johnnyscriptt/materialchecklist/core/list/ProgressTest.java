package io.github.johnnyscriptt.materialchecklist.core.list;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressTest {
    private static final Entry GLASS = new Entry("minecraft:glass", 100, false);
    private static final Entry LOG = new Entry("minecraft:oak_log", 64, false);
    private static final Entry LANTERN = new Entry("minecraft:lantern", 10, false);

    private static Progress of(List<Entry> entries, Map<String, Long> have) {
        return Progress.of(entries, id -> have.getOrDefault(id, 0L));
    }

    @Test
    void anEntryIsDoneOnceYouHaveEnough() {
        Progress progress = of(List.of(GLASS, LOG), Map.of("minecraft:glass", 100L, "minecraft:oak_log", 63L));
        assertTrue(progress.rows().stream().filter(r -> r.entry() == GLASS).findFirst().orElseThrow().done());
        assertEquals(1, progress.doneCount());
        assertEquals(2, progress.total());
        assertFalse(progress.complete());
    }

    @Test
    void missingRowsComeFirstAndKeepListOrder() {
        Progress progress = of(List.of(GLASS, LOG, LANTERN), Map.of("minecraft:glass", 500L));
        assertEquals(List.of(LOG, LANTERN, GLASS), progress.rows().stream().map(Progress.Row::entry).toList());
    }

    @Test
    void remainingNeverGoesNegative() {
        Progress progress = of(List.of(LANTERN), Map.of("minecraft:lantern", 40L));
        assertEquals(0, progress.rows().getFirst().remaining());
        assertEquals(40, progress.rows().getFirst().have());
    }

    @Test
    void aManualTickCountsAsDone() {
        Progress progress = of(List.of(new Entry("minecraft:glass", 100, true)), Map.of());
        assertTrue(progress.complete());
        assertEquals(1.0, progress.fraction(), 1e-9);
    }

    @Test
    void fractionWeighsByItemsNotRows() {
        // 50 of 100 glass and 0 of 10 lanterns: 50 of 110 items gathered.
        Progress progress = of(List.of(GLASS, LANTERN), Map.of("minecraft:glass", 50L));
        assertEquals(50.0 / 110.0, progress.fraction(), 1e-9);
    }

    @Test
    void surplusDoesNotInflateTheFraction() {
        Progress progress = of(List.of(GLASS, LANTERN), Map.of("minecraft:glass", 1000L));
        assertEquals(100.0 / 110.0, progress.fraction(), 1e-9);
    }

    @Test
    void anEmptyListIsNotComplete() {
        Progress progress = of(List.of(), Map.of());
        assertFalse(progress.complete());
        assertEquals(0.0, progress.fraction(), 1e-9);
    }
}
