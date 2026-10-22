package io.github.johnnyscriptt.materialchecklist.core.ocr;

import io.github.johnnyscriptt.materialchecklist.core.list.Entry;
import io.github.johnnyscriptt.materialchecklist.core.parse.Candidate;
import io.github.johnnyscriptt.materialchecklist.core.parse.ItemNameMatcher;
import io.github.johnnyscriptt.materialchecklist.core.parse.ListParser;
import io.github.johnnyscriptt.materialchecklist.core.parse.OcrCleanup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end accuracy: OCR, clean-up and parsing together, scored by how many rows come back as
 * the right item with the right amount.
 *
 * <p>Two kinds of input. Real screenshots live in {@code src/test/resources/ocr/} as
 * {@code name.png} plus {@code name.expected} ("item_id amount" per row). Synthetic Litematica
 * style tables are drawn here with the game's real font at GUI scales 2 and 3, and once more
 * the way a YouTube frame shows one: drawn at scale 4, shown at 60% and JPEG compressed. The
 * font comes from the Minecraft jar the build already has, so it is never copied into the repo.
 */
class OcrAccuracyTest {
    /** At least this share of rows must come back exactly right. */
    private static final double REQUIRED = 0.9;
    private static final List<String> FIXTURES = List.of("game-font-gui2");
    private static final String ATLAS = "assets/minecraft/textures/font/ascii.png";

    private static final String[][] TABLE = {
            {"Oak Planks", "512", "500", "12"},
            {"Stone Bricks", "1088", "1088", "0"},
            {"Glass Pane", "36", "0", "36"},
            {"Spruce Log", "96", "64", "32"},
            {"Lantern", "12", "12", "0"},
            {"Cobblestone", "640", "320", "320"},
            {"Dark Oak Stairs", "203", "203", "0"},
            {"Polished Andesite", "1730", "1000", "730"},
            {"Iron Bars", "48", "8", "40"},
            {"White Concrete", "4096", "4096", "0"},
    };

    @TempDir
    static Path cache;

    private static OcrEngine engine;
    private static ListParser parser;
    private static BufferedImage atlas;
    private static GlyphSet numbers;
    private static GlyphSet font;

    @BeforeAll
    static void start() throws Exception {
        engine = OcrEngine.open(Tessdata.install(cache));
        parser = new ListParser(new ItemNameMatcher(vanillaItems()));
        atlas = gameFontAtlas();
        numbers = GlyphSet.fromAtlas(atlas, GlyphSet.NUMBER_CHARS);
        StringBuilder printable = new StringBuilder();
        for (char c = 33; c < 127; c++) {
            printable.append(c);
        }
        font = GlyphSet.fromAtlas(atlas, printable.toString());
    }

    @AfterAll
    static void stop() {
        if (engine != null) {
            engine.close();
        }
    }

    @TestFactory
    List<DynamicTest> screenshotsReadBackAccurately() throws IOException {
        List<DynamicTest> tests = new ArrayList<>();
        for (String name : FIXTURES) {
            BufferedImage image = ImageIO.read(resource("/ocr/" + name + ".png"));
            List<Entry> expected = expected(name);
            double required = REQUIRED;
            tests.add(DynamicTest.dynamicTest(name, () -> score(name, image, expected, required)));
        }
        return tests;
    }

    @TestFactory
    List<DynamicTest> litematicaTablesReadBackAccurately() {
        List<Entry> expected = new ArrayList<>();
        for (String[] row : TABLE) {
            expected.add(new Entry("minecraft:" + row[0].toLowerCase(Locale.ROOT).replace(' ', '_'),
                    Long.parseLong(row[1]), false));
        }
        return List.of(
                DynamicTest.dynamicTest("table at GUI scale 2", () -> score("scale 2", table(2), expected)),
                DynamicTest.dynamicTest("table at GUI scale 3", () -> score("scale 3", table(3), expected)),
                DynamicTest.dynamicTest("table in a video frame", () -> score("video",
                        BitmapDigitReaderTest.jpeg(BitmapDigitReaderTest.shrink(table(4), 0.6), 0.6f), expected)));
    }

    private static void score(String label, BufferedImage image, List<Entry> expected) {
        score(label, image, expected, REQUIRED);
    }

    private static void score(String label, BufferedImage image, List<Entry> expected, double required) {
        String raw = engine.recognize(image, numbers);
        List<Entry> read = ListParser.toEntries(parser.parse(OcrCleanup.clean(raw)));
        long hits = expected.stream().filter(read::contains).count();
        String report = label + ": " + hits + "/" + expected.size() + "\nraw:\n" + raw + "\nread: " + read;
        System.out.println(report);
        assertTrue(hits >= Math.ceil(expected.size() * required), report);
    }

    /**
     * A material list laid out like Litematica's: a dark panel over a busy background, an icon
     * square per row, names in white and the Missing column in red, each with a drop shadow.
     */
    private static BufferedImage table(int scale) {
        int rowH = 12;
        int[] columnX = {16, 130, 175, 225};
        int widthPx = 290;
        int heightPx = rowH * (TABLE.length + 2) + 8;
        BufferedImage image = new BufferedImage((widthPx + 40) * scale, (heightPx + 20) * scale,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        // Grass and sky behind the panel.
        g.setColor(new Color(120, 170, 255));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(new Color(90, 150, 60));
        g.fillRect(0, image.getHeight() / 2, image.getWidth(), image.getHeight() / 2);
        g.setColor(new Color(16, 16, 24, 220));
        g.fillRect(20 * scale, 10 * scale, widthPx * scale, heightPx * scale);

        String[] header = {"Item", "Total", "Missing", "Available"};
        for (int c = 0; c < header.length; c++) {
            text(g, header[c], 20 + columnX[c], 14, scale, new Color(255, 255, 160));
        }
        Color[] icons = {new Color(160, 120, 70), new Color(130, 130, 130), new Color(200, 230, 240),
                new Color(90, 60, 30), new Color(250, 200, 90)};
        for (int r = 0; r < TABLE.length; r++) {
            int y = 14 + (r + 1) * rowH + 2;
            g.setColor(icons[r % icons.length]);
            g.fillRect((20 + 4) * scale, (y - 1) * scale, 9 * scale, 9 * scale);
            text(g, TABLE[r][0], 20 + columnX[0], y, scale, Color.WHITE);
            text(g, TABLE[r][1], 20 + columnX[1], y, scale, Color.WHITE);
            text(g, TABLE[r][2], 20 + columnX[2], y, scale, new Color(255, 85, 85));
            text(g, TABLE[r][3], 20 + columnX[3], y, scale, new Color(85, 255, 85));
        }
        g.dispose();
        return image;
    }

    /** Draws text in the game font at font-pixel position (x, y), shadow first, as the game does. */
    private static void text(Graphics2D g, String s, int x, int y, int scale, Color ink) {
        Color shadow = new Color(ink.getRed() / 4, ink.getGreen() / 4, ink.getBlue() / 4);
        glyphs(g, s, x + 1, y + 1, scale, shadow);
        glyphs(g, s, x, y, scale, ink);
    }

    private static void glyphs(Graphics2D g, String s, int x, int y, int scale, Color colour) {
        g.setColor(colour);
        int pen = x;
        for (char c : s.toCharArray()) {
            GlyphSet.Glyph glyph = font.glyph(c);
            if (glyph == null) {
                pen += 4; // space
                continue;
            }
            for (int r = 0; r < GlyphSet.ROWS; r++) {
                for (int col = 0; col < glyph.width(); col++) {
                    if (glyph.ink()[r][col]) {
                        g.fillRect((pen + col) * scale, (y + r) * scale, scale, scale);
                    }
                }
            }
            pen += glyph.width() + 1;
        }
    }

    private static List<Entry> expected(String name) throws IOException {
        List<Entry> out = new ArrayList<>();
        for (String line : read("/ocr/" + name + ".expected").split("\\R")) {
            String[] parts = line.strip().split("\\s+");
            if (parts.length == 2) {
                out.add(new Entry(parts[0], Long.parseLong(parts[1]), false));
            }
        }
        return out;
    }

    /** Where the game's name isn't the id title-cased, as real screenshots show it. */
    private static final java.util.Map<String, String> REAL_NAMES = java.util.Map.of(
            "redstone_block", "Block of Redstone");

    /** Vanilla item ids with names made from them ("oak_planks" to "Oak Planks"), close to the real ones. */
    static List<Candidate> vanillaItems() throws IOException {
        List<Candidate> out = new ArrayList<>();
        for (String id : read("/items.txt").split("\\R")) {
            String path = id.strip();
            if (path.isEmpty()) {
                continue;
            }
            String name = String.join(" ", Arrays.stream(path.split("_"))
                    .map(w -> w.substring(0, 1).toUpperCase(Locale.ROOT) + w.substring(1)).toList());
            name = REAL_NAMES.getOrDefault(path, name);
            out.add(new Candidate("minecraft:" + path, name, 64));
        }
        return out;
    }

    /** The font atlas from the Minecraft client jar: the test classpath if it's there, else Loom's cache. */
    static BufferedImage gameFontAtlas() throws IOException {
        InputStream onClasspath = OcrAccuracyTest.class.getResourceAsStream("/" + ATLAS);
        if (onClasspath != null) {
            try (onClasspath) {
                return ImageIO.read(onClasspath);
            }
        }
        Path jar = Path.of(System.getProperty("user.home"), ".gradle", "caches", "fabric-loom", "minecraftMaven",
                "net", "minecraft", "minecraft-clientonly-deobf", "26.3", "minecraft-clientonly-deobf-26.3.jar");
        try (ZipFile zip = new ZipFile(jar.toFile()); InputStream in = zip.getInputStream(zip.getEntry(ATLAS))) {
            return ImageIO.read(in);
        }
    }

    private static InputStream resource(String path) {
        InputStream in = OcrAccuracyTest.class.getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("missing test resource " + path);
        }
        return in;
    }

    private static String read(String path) throws IOException {
        try (InputStream in = resource(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
