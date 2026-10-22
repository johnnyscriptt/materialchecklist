package io.github.johnnyscriptt.materialchecklist.core.ocr;

import org.junit.jupiter.api.Test;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Numbers drawn the way the game draws them (a pixel font at a whole-number GUI scale, with a
 * drop shadow one font pixel down and right) must come back exactly, including after the
 * blur and compression of a video frame.
 */
class BitmapDigitReaderTest {
    /** Glyph shapes as the game's default font draws them (rows 0..7, '#' is ink). */
    static final Map<Character, String[]> PATTERNS = new LinkedHashMap<>();

    static {
        PATTERNS.put('0', new String[]{".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###.", "....."});
        PATTERNS.put('1', new String[]{"..#..", ".##..", "..#..", "..#..", "..#..", "..#..", "#####", "....."});
        PATTERNS.put('2', new String[]{".###.", "#...#", "....#", "..##.", ".#...", "#...#", "#####", "....."});
        PATTERNS.put('3', new String[]{".###.", "#...#", "....#", "..##.", "....#", "#...#", ".###.", "....."});
        PATTERNS.put('4', new String[]{"...##", "..#.#", ".#..#", "#...#", "#####", "....#", "....#", "....."});
        PATTERNS.put('5', new String[]{"#####", "#....", "####.", "....#", "....#", "#...#", ".###.", "....."});
        PATTERNS.put('6', new String[]{"..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###.", "....."});
        PATTERNS.put('7', new String[]{"#####", "#...#", "....#", "...#.", "..#..", "..#..", "..#..", "....."});
        PATTERNS.put('8', new String[]{".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###.", "....."});
        PATTERNS.put('9', new String[]{".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##..", "....."});
        PATTERNS.put(',', new String[]{".", ".", ".", ".", ".", ".", "#", "#"});
        PATTERNS.put('.', new String[]{".", ".", ".", ".", ".", ".", "#", "."});
        PATTERNS.put('x', new String[]{".....", ".....", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "....."});
    }

    private static final GlyphSet GLYPHS = GlyphSet.of(PATTERNS);

    @Test
    void readsNumbersAtWholeScales() {
        for (int scale = 1; scale <= 4; scale++) {
            for (String number : new String[]{"1088", "640", "512", "96", "1234567890"}) {
                BufferedImage image = render(number, scale, true, new Color(20, 20, 20), Color.WHITE);
                assertEquals(number, BitmapDigitReader.read(image, all(image), GLYPHS), "scale " + scale);
            }
        }
    }

    @Test
    void readsSeparatorsAndMultiplier() {
        BufferedImage image = render("1,024", 3, true, new Color(20, 20, 20), Color.WHITE);
        assertEquals("1,024", BitmapDigitReader.read(image, all(image), GLYPHS));
        BufferedImage times = render("19x64", 2, true, new Color(20, 20, 20), Color.WHITE);
        assertEquals("19x64", BitmapDigitReader.read(times, all(times), GLYPHS));
    }

    @Test
    void readsColouredTextLikeLitematicaDoes() {
        BufferedImage red = render("640", 2, true, new Color(30, 30, 40), new Color(255, 85, 85));
        assertEquals("640", BitmapDigitReader.read(red, all(red), GLYPHS));
    }

    @Test
    void readsDarkDigitsOnALightBackground() {
        BufferedImage image = render("1088", 3, false, new Color(235, 235, 235), new Color(40, 40, 40));
        assertEquals("1088", BitmapDigitReader.read(image, all(image), GLYPHS));
    }

    @Test
    void survivesAVideoFrame() throws IOException {
        // GUI scale 4, shown 60% as big and JPEG compressed: 2.4 screen pixels per font pixel.
        BufferedImage image = jpeg(shrink(render("1088", 4, true, new Color(20, 20, 20), Color.WHITE), 0.6), 0.6f);
        assertEquals("1088", BitmapDigitReader.read(image, all(image), GLYPHS));
        BufferedImage other = jpeg(shrink(render("640", 4, true, new Color(20, 20, 20), Color.WHITE), 0.6), 0.6f);
        assertEquals("640", BitmapDigitReader.read(other, all(other), GLYPHS));
    }

    @Test
    void readsDigitsWhoseShadowHasBledIntoThem() throws IOException {
        // A GUI-scale-2 screenshot shown at 80% on a web page (1.6 screen pixels per font
        // pixel): smoothing smears the text into its shadow, so glyphs read as glyph-plus-shadow.
        // Modelled on the real Litematica screenshot in the OCR fixtures.
        for (String number : new String[]{"1207", "1066", "1180", "1206"}) {
            BufferedImage sharp = render(number, 2, true, new Color(30, 30, 30), Color.WHITE);
            BufferedImage soft = jpeg(shrink(sharp, 0.8), 0.9f);
            assertEquals(number, BitmapDigitReader.read(soft, all(soft), GLYPHS, true), number);
        }
    }

    @Test
    void wordsThatAreNotNumbersStayStrict() {
        // A solid bar is nothing like a digit at any tolerance, and the strict reading the
        // engine uses for words without digits must still refuse a blurred number.
        BufferedImage sharp = renderWithShadow("1207", 3, new Color(30, 30, 30), Color.WHITE, new Color(200, 200, 200));
        assertNull(BitmapDigitReader.read(BitmapDigitReaderTest.blurred(sharp), all(sharp), GLYPHS, false));
    }

    static BufferedImage blurred(BufferedImage sharp) {
        try {
            BufferedImage soft = jpeg(shrink(shrink(sharp, 0.5), 1.25), 0.85f);
            BufferedImage out = new BufferedImage(sharp.getWidth(), sharp.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.drawImage(soft, 0, 0, null);
            g.dispose();
            return out;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void onlyReadsInsideTheBoxItIsGiven() {
        BufferedImage image = render("12   34", 3, true, new Color(20, 20, 20), Color.WHITE);
        int digitW = 6 * 3;
        Rectangle firstWord = new Rectangle(4, 0, 2 * digitW, image.getHeight());
        assertEquals("12", BitmapDigitReader.read(image, firstWord, GLYPHS));
    }

    @Test
    void refusesShapesThatAreNotDigits() {
        BufferedImage image = new BufferedImage(60, 30, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(20, 20, 20));
        g.fillRect(0, 0, 60, 30);
        g.setColor(Color.WHITE);
        g.fillRect(6, 6, 40, 16); // a solid bar
        g.dispose();
        assertNull(BitmapDigitReader.read(image, all(image), GLYPHS));
    }

    @Test
    void refusesAnEmptyBox() {
        BufferedImage image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        assertNull(BitmapDigitReader.read(image, all(image), GLYPHS));
    }

    static Rectangle all(BufferedImage image) {
        return new Rectangle(0, 0, image.getWidth(), image.getHeight());
    }

    static BufferedImage renderWithShadow(String text, int scale, Color background, Color ink, Color shadow) {
        BufferedImage image = render(text, scale, false, background, ink);
        Graphics2D g = image.createGraphics();
        draw(g, text, scale, 3, 3, shadow);
        draw(g, text, scale, 2, 2, ink);
        g.dispose();
        return image;
    }

    /** Draws {@code text} in the test font; a space advances four font pixels, as in game. */
    static BufferedImage render(String text, int scale, boolean shadow, Color background, Color ink) {
        int widthPx = 4;
        for (char c : text.toCharArray()) {
            widthPx += c == ' ' ? 4 : PATTERNS.get(c)[0].length() + 1;
        }
        BufferedImage image = new BufferedImage((widthPx + 4) * scale, 12 * scale, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(background);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        Color shadowColour = new Color(ink.getRed() / 4, ink.getGreen() / 4, ink.getBlue() / 4);
        if (shadow) {
            draw(g, text, scale, 2 + 1, 2 + 1, shadowColour);
        }
        draw(g, text, scale, 2, 2, ink);
        g.dispose();
        return image;
    }

    private static void draw(Graphics2D g, String text, int scale, int ox, int oy, Color colour) {
        g.setColor(colour);
        int x = ox;
        for (char c : text.toCharArray()) {
            if (c == ' ') {
                x += 4;
                continue;
            }
            String[] rows = PATTERNS.get(c);
            for (int r = 0; r < rows.length; r++) {
                for (int col = 0; col < rows[r].length(); col++) {
                    if (rows[r].charAt(col) == '#') {
                        g.fillRect((x + col) * scale, (oy + r) * scale, scale, scale);
                    }
                }
            }
            x += rows[0].length() + 1;
        }
    }

    static BufferedImage shrink(BufferedImage source, double factor) {
        int w = (int) Math.round(source.getWidth() * factor);
        int h = (int) Math.round(source.getHeight() * factor);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    static BufferedImage jpeg(BufferedImage source, float quality) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        try (MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(bytes)) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(source, null, null), param);
        }
        writer.dispose();
        return ImageIO.read(new ByteArrayInputStream(bytes.toByteArray()));
    }
}
