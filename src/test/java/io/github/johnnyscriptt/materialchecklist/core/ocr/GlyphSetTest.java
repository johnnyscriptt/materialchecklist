package io.github.johnnyscriptt.materialchecklist.core.ocr;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlyphSetTest {

    @Test
    void aUnionKeepsEveryShapeFromBothFonts() {
        GlyphSet vanilla = GlyphSet.of(Map.of('1', BitmapDigitReaderTest.PATTERNS.get('1')));
        GlyphSet pack = GlyphSet.of(Map.of('1', new String[]{"#", "#", "#", "#", "#", "#", "#", "."}));
        GlyphSet both = GlyphSet.union(vanilla, pack);
        assertEquals(2, both.glyphs().size());
        assertTrue(both.glyphs().stream().allMatch(g -> g.character() == '1'));
    }

    @Test
    void aUnionWithAnEmptySetIsTheOtherSet() {
        GlyphSet vanilla = GlyphSet.of(BitmapDigitReaderTest.PATTERNS);
        assertEquals(vanilla.glyphs().size(), GlyphSet.union(vanilla, GlyphSet.of(Map.of())).glyphs().size());
    }

    @Test
    void theFirstFontWinsWhenBothFitAboutAsWell() {
        // A pack whose 8 is drawn exactly like the vanilla 0: a vanilla "1065" must stay 1065.
        GlyphSet pack = GlyphSet.of(Map.of('8', BitmapDigitReaderTest.PATTERNS.get('0')));
        GlyphSet both = GlyphSet.union(GlyphSet.of(BitmapDigitReaderTest.PATTERNS), pack);
        BufferedImage image = BitmapDigitReaderTest.render("1065", 3, true, new Color(20, 20, 20), Color.WHITE);
        assertEquals("1065", BitmapDigitReader.read(image, BitmapDigitReaderTest.all(image), both));
    }

    @Test
    void aPackGlyphMustFitClearlyBetterToBeatTheVanillaOne() {
        // Blur leaves the vanilla 0 a cell off, and a pack 8 happens to fit that smudge exactly.
        // Video screenshots are almost always vanilla, so a near miss must not flip the digit.
        String[] smudged = {".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".####", "....."};
        String[] original = BitmapDigitReaderTest.PATTERNS.put('0', smudged);
        BufferedImage image;
        try {
            image = BitmapDigitReaderTest.render("1065", 3, true, new Color(20, 20, 20), Color.WHITE);
        } finally {
            BitmapDigitReaderTest.PATTERNS.put('0', original);
        }
        GlyphSet pack = GlyphSet.of(Map.of('8', smudged));
        GlyphSet both = GlyphSet.union(GlyphSet.of(BitmapDigitReaderTest.PATTERNS), pack);
        assertEquals("1065", BitmapDigitReader.read(image, BitmapDigitReaderTest.all(image), both));
    }

    @Test
    void readsNumbersInEitherFontOfAUnion() {
        // A pack font whose 7 is a different shape: numbers in the vanilla font must still read.
        GlyphSet pack = GlyphSet.of(Map.of('7', new String[]{"#####", "....#", "...#.", "...#.", "..#..", "..#..", "..#..", "....."}));
        GlyphSet both = GlyphSet.union(GlyphSet.of(BitmapDigitReaderTest.PATTERNS), pack);
        BufferedImage image = BitmapDigitReaderTest.render("1207", 3, true, new Color(20, 20, 20), Color.WHITE);
        assertEquals("1207", BitmapDigitReader.read(image, BitmapDigitReaderTest.all(image), both));
    }
}
