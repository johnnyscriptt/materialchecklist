package io.github.johnnyscriptt.materialchecklist.core.parse;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemNameMatcherTest {
    static final List<Candidate> ITEMS = List.of(
            new Candidate("minecraft:oak_planks", "Oak Planks", 64),
            new Candidate("minecraft:oak_log", "Oak Log", 64),
            new Candidate("minecraft:oak_wood", "Oak Wood", 64),
            new Candidate("minecraft:oak_slab", "Oak Slab", 64),
            new Candidate("minecraft:spruce_log", "Spruce Log", 64),
            new Candidate("minecraft:stone", "Stone", 64),
            new Candidate("minecraft:stone_bricks", "Stone Bricks", 64),
            new Candidate("minecraft:stone_brick_stairs", "Stone Brick Stairs", 64),
            new Candidate("minecraft:cobblestone", "Cobblestone", 64),
            new Candidate("minecraft:glass", "Glass", 64),
            new Candidate("minecraft:glass_pane", "Glass Pane", 64),
            new Candidate("minecraft:sweet_berries", "Sweet Berries", 64),
            new Candidate("minecraft:polished_blackstone_brick_stairs", "Polished Blackstone Brick Stairs", 64),
            new Candidate("minecraft:polished_blackstone_brick_wall", "Polished Blackstone Brick Wall", 64),
            new Candidate("minecraft:ender_pearl", "Ender Pearl", 16),
            new Candidate("minecraft:lantern", "Lantern", 64),
            new Candidate("minecraft:iron_bars", "Iron Bars", 64),
            new Candidate("minecraft:iron_axe", "Iron Axe", 1));

    private final ItemNameMatcher matcher = new ItemNameMatcher(ITEMS);

    @Test
    void exactNameIsAFullMatch() {
        ItemNameMatcher.Match match = matcher.best("Oak Planks");
        assertEquals("minecraft:oak_planks", match.candidate().id());
        assertEquals(1.0, match.score(), 1e-9);
    }

    @Test
    void caseAndPunctuationDoNotMatter() {
        assertEquals("minecraft:stone_bricks", matcher.best("  stone-BRICKS: ").candidate().id());
    }

    @Test
    void pluralsMatchTheSingularName() {
        assertEquals("minecraft:oak_log", matcher.best("oak logs").candidate().id());
        assertEquals("minecraft:glass_pane", matcher.best("Glass Panes").candidate().id());
        assertEquals("minecraft:sweet_berries", matcher.best("sweet berry").candidate().id());
        assertTrue(matcher.best("oak logs").confident());
    }

    @Test
    void registryIdsMatchToo() {
        assertEquals("minecraft:glass_pane", matcher.best("minecraft:glass_pane").candidate().id());
        assertEquals("minecraft:oak_slab", matcher.best("oak_slab").candidate().id());
    }

    @Test
    void ocrTyposStillFindTheItem() {
        ItemNameMatcher.Match match = matcher.best("Cobb1estone");
        assertEquals("minecraft:cobblestone", match.candidate().id());
        assertTrue(match.confident(), "score " + match.score());
        assertEquals("minecraft:spruce_log", matcher.best("Spruoe Log").candidate().id());
    }

    @Test
    void lettersOcrConfusesCostLessThanRandomOnes() {
        // The game font's B/G, P/F and O/D are a pixel or two apart; a real typo is not.
        assertEquals("minecraft:iron_bars", matcher.best("Iron Gare").candidate().id());
        ItemNameMatcher.Match planks = matcher.best("Dak Flanks");
        assertEquals("minecraft:oak_planks", planks.candidate().id());
        assertTrue(planks.confident(), "score " + planks.score());
        assertTrue(matcher.best("Glass Fane").confident());
    }

    @Test
    void exactWinsOverNearNeighbours() {
        assertEquals("minecraft:stone", matcher.best("Stone").candidate().id());
        assertEquals("minecraft:glass", matcher.best("glass").candidate().id());
    }

    @Test
    void truncatedNamesMatchByPrefix() {
        ItemNameMatcher.Match match = matcher.best("Polished Blackstone Brick St...");
        assertEquals("minecraft:polished_blackstone_brick_stairs", match.candidate().id());
        assertTrue(match.confident(), "score " + match.score());
    }

    @Test
    void nonsenseIsNotMatched() {
        assertNull(matcher.best("Total Missing Available"));
        assertNull(matcher.best("qwzx"));
        assertNull(matcher.best(""));
    }

    @Test
    void aWeakGuessIsReportedButNotConfident() {
        ItemNameMatcher.Match match = matcher.best("Oak Plank Stair");
        assertTrue(match == null || !match.confident(), () -> "too sure: " + match);
    }
}
