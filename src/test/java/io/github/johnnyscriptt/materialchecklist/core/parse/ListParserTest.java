package io.github.johnnyscriptt.materialchecklist.core.parse;

import io.github.johnnyscriptt.materialchecklist.core.list.Entry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListParserTest {
    private final ListParser parser = new ListParser(new ItemNameMatcher(ItemNameMatcherTest.ITEMS));

    private ParsedLine only(String text) {
        List<ParsedLine> lines = parser.parse(text);
        assertEquals(1, lines.size(), () -> "lines: " + lines);
        return lines.getFirst();
    }

    @Test
    void nameThenCount() {
        ParsedLine line = only("Oak Planks 128");
        assertEquals("minecraft:oak_planks", line.match().candidate().id());
        assertEquals(128, line.count());
    }

    @Test
    void countThenName() {
        ParsedLine line = only("64 Cobblestone");
        assertEquals("minecraft:cobblestone", line.match().candidate().id());
        assertEquals(64, line.count());
    }

    @Test
    void multiplierAndStackForms() {
        assertEquals(64, only("Glass x64").count());
        assertEquals(3 * 64, only("Stone Bricks: 3 stacks").count());
        assertEquals(2 * 27 * 64, only("2 shulkers of glass pane").count());
        assertEquals(27 * 64 + 3 * 64 + 5, only("Spruce Log - 1sb + 3st + 5").count());
    }

    @Test
    void stackWordsUseTheMatchedItemsStackSize() {
        assertEquals(48, only("Ender Pearls 3 stacks").count());
    }

    @Test
    void tableRowsTakeTheFirstAmountColumn() {
        ParsedLine line = only("| Oak Log            |   512 |     500 |        12 |");
        assertEquals("minecraft:oak_log", line.match().candidate().id());
        assertEquals(512, line.count());
        assertEquals(96, only("Lantern\t96\t0").count());
        assertEquals(1024, only("Stone Bricks      1,024").count());
    }

    @Test
    void bulletsAndNumberingAreNotCounts() {
        assertEquals(32, only("- Oak Slab 32").count());
        assertEquals(32, only("• Oak Slab 32").count());
        ParsedLine numbered = only("3. Oak Slab 32");
        assertEquals(32, numbered.count());
        assertEquals("minecraft:oak_slab", numbered.match().candidate().id());
    }

    @Test
    void aNameWithoutACountIsKeptForReview() {
        ParsedLine line = only("Lantern");
        assertEquals("minecraft:lantern", line.match().candidate().id());
        assertEquals(ParsedLine.NO_COUNT, line.count());
        assertTrue(line.needsReview());
    }

    @Test
    void headersBordersAndBlankLinesAreSkipped() {
        String text = """
                +-------------+-------+---------+-----------+
                | Item        | Total | Missing | Available |
                +-------------+-------+---------+-----------+
                | Glass       |    12 |      12 |         0 |

                Material List for "Starter House"
                """;
        List<ParsedLine> lines = parser.parse(text);
        assertEquals(1, lines.size(), () -> "lines: " + lines);
        assertEquals(12, lines.getFirst().count());
    }

    @Test
    void unknownItemsSurviveUnmatchedForReview() {
        ParsedLine line = only("Zorblax 12");
        assertNull(line.match());
        assertEquals(12, line.count());
        assertTrue(line.needsReview());
    }

    @Test
    void confidentLinesNeedNoReview() {
        assertFalse(only("Oak Planks 128").needsReview());
    }

    @Test
    void duplicatesMergeIntoOneEntryInFirstSeenOrder() {
        List<Entry> entries = ListParser.toEntries(parser.parse("""
                Glass 10
                Oak Log 5
                glass 6
                Zorblax 3
                Lantern
                """));
        assertEquals(List.of(
                new Entry("minecraft:glass", 16, false),
                new Entry("minecraft:oak_log", 5, false),
                new Entry("minecraft:lantern", 1, false)), entries);
    }
}
