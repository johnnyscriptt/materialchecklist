package io.github.johnnyscriptt.materialchecklist.core.parse;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LitematicaImportTest {
    private final ListParser parser = new ListParser(new ItemNameMatcher(ItemNameMatcherTest.ITEMS));

    @Test
    void readsTheCsvExportByItsTotalColumn() {
        String csv = """
                "Item","Total","Missing","Available"
                "Oak Planks",512,500,12
                "Stone Bricks",1088,1088,0
                "Glass Pane",36,0,36
                """;
        assertTrue(LitematicaImport.looksLikeCsv(csv));
        List<ParsedLine> lines = parser.parse(LitematicaImport.toRows(csv));
        assertEquals(3, lines.size(), () -> "lines: " + lines);
        assertEquals("minecraft:oak_planks", lines.get(0).match().candidate().id());
        assertEquals(512, lines.get(0).count());
        assertEquals(1088, lines.get(1).count());
        assertEquals(36, lines.get(2).count());
    }

    @Test
    void usesTheTotalColumnEvenWhenItIsNotFirst() {
        String csv = """
                Missing,Item,Total
                3,Lantern,9
                """;
        List<ParsedLine> lines = parser.parse(LitematicaImport.toRows(csv));
        assertEquals(9, lines.getFirst().count());
    }

    @Test
    void readsQuotedNamesContainingCommas() {
        String csv = """
                "Item","Total"
                "Oak Log, stripped",40
                """;
        assertEquals("Oak Log, stripped\t40", LitematicaImport.toRows(csv).strip());
    }

    @Test
    void plainListsAreNotMistakenForCsv() {
        assertFalse(LitematicaImport.looksLikeCsv("Oak Planks 1,024\nGlass 12"));
        assertFalse(LitematicaImport.looksLikeCsv("| Item | Total |\n| Glass | 12 |"));
    }
}
