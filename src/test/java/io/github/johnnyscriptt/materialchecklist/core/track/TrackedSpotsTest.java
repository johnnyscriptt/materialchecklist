package io.github.johnnyscriptt.materialchecklist.core.track;

import io.github.johnnyscriptt.materialchecklist.core.track.TrackedSpots.Contents;
import io.github.johnnyscriptt.materialchecklist.core.track.TrackedSpots.Spot;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackedSpotsTest {
    private static final String ALEX = "alex";
    private static final String SAM = "sam";
    private static final String HOUSE = "house";
    private static final Spot A = new Spot("minecraft:overworld", 1L);
    private static final Spot A_PARTNER = new Spot("minecraft:overworld", 2L);
    private static final Spot B = new Spot("minecraft:the_nether", 1L);

    @Test
    void togglingAddsThenRemovesTheWholeUnit() {
        TrackedSpots spots = new TrackedSpots();
        assertEquals(TrackedSpots.Result.ADDED, spots.toggle(ALEX, HOUSE, List.of(A, A_PARTNER)));
        assertEquals(Set.of(A, A_PARTNER), spots.spots(ALEX, HOUSE));
        // Toggling from either half of a double chest untracks both.
        assertEquals(TrackedSpots.Result.REMOVED, spots.toggle(ALEX, HOUSE, List.of(A_PARTNER, A)));
        assertTrue(spots.spots(ALEX, HOUSE).isEmpty());
    }

    @Test
    void playersAndProjectsAreSeparate() {
        TrackedSpots spots = new TrackedSpots();
        spots.toggle(ALEX, HOUSE, List.of(A));
        spots.toggle(SAM, HOUSE, List.of(B));
        spots.toggle(ALEX, "tower", List.of(B));
        assertEquals(Set.of(A), spots.spots(ALEX, HOUSE));
        assertEquals(Set.of(B), spots.spots(SAM, HOUSE));
        assertEquals(Set.of(B), spots.spots(ALEX, "tower"));
    }

    @Test
    void aProjectStopsAcceptingSpotsAtTheCap() {
        TrackedSpots spots = new TrackedSpots();
        for (long i = 0; i < TrackedSpots.MAX_PER_PROJECT; i++) {
            spots.toggle(ALEX, HOUSE, List.of(new Spot("d", 100 + i)));
        }
        assertEquals(TrackedSpots.Result.FULL, spots.toggle(ALEX, HOUSE, List.of(A)));
        assertEquals(TrackedSpots.MAX_PER_PROJECT, spots.spots(ALEX, HOUSE).size());
    }

    @Test
    void totalsAddLiveContentsAndRememberThem() {
        TrackedSpots spots = new TrackedSpots();
        spots.toggle(ALEX, HOUSE, List.of(A, A_PARTNER));
        Map<Spot, Contents> world = new HashMap<>();
        world.put(A, Contents.live(Map.of("minecraft:glass", 10L)));
        world.put(A_PARTNER, Contents.live(Map.of("minecraft:glass", 5L, "minecraft:stone", 1L)));
        assertEquals(Map.of("minecraft:glass", 15L, "minecraft:stone", 1L), spots.totals(ALEX, HOUSE, world::get));
        assertEquals(Map.of("minecraft:glass", 10L), spots.snapshot(A));
    }

    @Test
    void anUnloadedContainerCountsWhatItHeldLastTime() {
        TrackedSpots spots = new TrackedSpots();
        spots.toggle(ALEX, HOUSE, List.of(A));
        spots.totals(ALEX, HOUSE, spot -> Contents.live(Map.of("minecraft:glass", 7L)));
        assertEquals(Map.of("minecraft:glass", 7L), spots.totals(ALEX, HOUSE, spot -> Contents.UNLOADED));
    }

    @Test
    void aBrokenContainerIsForgottenByEveryone() {
        TrackedSpots spots = new TrackedSpots();
        spots.toggle(ALEX, HOUSE, List.of(A));
        spots.toggle(SAM, HOUSE, List.of(A));
        assertEquals(Map.of(), spots.totals(ALEX, HOUSE, spot -> Contents.GONE));
        assertTrue(spots.spots(ALEX, HOUSE).isEmpty());
        assertTrue(spots.spots(SAM, HOUSE).isEmpty());
        assertEquals(Map.of(), spots.snapshot(A));
    }

    @Test
    void survivesASaveAndLoad() {
        TrackedSpots spots = new TrackedSpots();
        spots.toggle(ALEX, HOUSE, List.of(A, A_PARTNER));
        spots.totals(ALEX, HOUSE, spot -> Contents.live(Map.of("minecraft:glass", 3L)));
        TrackedSpots loaded = TrackedSpots.fromSaved(spots.toSaved());
        assertEquals(Set.of(A, A_PARTNER), loaded.spots(ALEX, HOUSE));
        assertEquals(Map.of("minecraft:glass", 3L), loaded.snapshot(A_PARTNER));
    }

    @Test
    void untrackedSnapshotsAreDropped() {
        TrackedSpots spots = new TrackedSpots();
        spots.toggle(ALEX, HOUSE, List.of(A));
        spots.totals(ALEX, HOUSE, spot -> Contents.live(Map.of("minecraft:glass", 3L)));
        spots.toggle(ALEX, HOUSE, List.of(A));
        assertEquals(Map.of(), spots.snapshot(A));
        assertTrue(spots.toSaved().snapshots().isEmpty());
    }
}
