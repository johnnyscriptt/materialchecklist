package io.github.johnnyscriptt.materialchecklist.core.parse;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OcrCleanupTest {

    @Test
    void fixesLettersReadInsideNumbers() {
        assertEquals("Oak Planks 1024", OcrCleanup.clean("Oak Planks 1O24"));
        assertEquals("Glass 128", OcrCleanup.clean("Glass l28"));
        assertEquals("Stone 15", OcrCleanup.clean("Stone 1S"));
    }

    @Test
    void stripsCursorsAndStrayMarksStuckToNumbers() {
        assertEquals("Cobblestone 640", OcrCleanup.clean("Cobblestone 640_"));
        assertEquals("Lantern 12", OcrCleanup.clean("Lantern '12~"));
    }

    @Test
    void leavesUnitsAndWordsAlone() {
        assertEquals("Glass 2SB", OcrCleanup.clean("Glass 2SB"));
        assertEquals("Oak Log 3 st + 5", OcrCleanup.clean("Oak Log 3 st + 5"));
        assertEquals("Iron Bars 10", OcrCleanup.clean("Iron Bars 10"));
        assertEquals("Lantern x64", OcrCleanup.clean("Lantern x64"));
    }

    @Test
    void tableBordersReadAsLettersBecomeColumnBreaks() {
        assertEquals("Oak Log\t512\t500", OcrCleanup.clean("l Oak Log | 512 I 500 |"));
    }

    @Test
    void dropsTheScrapsAnItemIconLeavesBeforeTheName() {
        assertEquals("Sandstone\t9", OcrCleanup.clean("e Sandstone\t9"));
        assertEquals("Observer\t94", OcrCleanup.clean("be Observer\t94"));
        assertEquals("Block of Redstone\t39", OcrCleanup.clean("(P Block of Redstone\t39"));
        assertEquals("Obsidian\t4", OcrCleanup.clean("___ Obsidian\t4"));
        // Real short words and amounts at the start stay.
        assertEquals("TNT 5", OcrCleanup.clean("TNT 5"));
        assertEquals("64 Oak Log", OcrCleanup.clean("64 Oak Log"));
        assertEquals("x64 Glass", OcrCleanup.clean("x64 Glass"));
    }

    @Test
    void dropsLinesWithNoWordInThem() {
        assertEquals("Glass 12", OcrCleanup.clean("~~ -- ..\nGlass 12\n|| 1 ||\n  \n"));
    }

    @Test
    void dotLeadersBecomeAColumnBreak() {
        assertEquals("Lantern\t9", OcrCleanup.clean("Lantern ........ 9"));
    }

    @Test
    void keepsLineOrder() {
        assertEquals(List.of("Glass 1", "Stone 2"), OcrCleanup.clean("Glass 1\nStone 2").lines().toList());
    }
}
