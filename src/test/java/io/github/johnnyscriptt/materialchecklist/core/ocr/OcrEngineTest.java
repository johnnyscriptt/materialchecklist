package io.github.johnnyscriptt.materialchecklist.core.ocr;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the real Tesseract natives, so this doubles as the check that they load on this
 * platform at all. The images are drawn here rather than stored, so the expected text is known
 * exactly.
 */
class OcrEngineTest {
    @TempDir
    static Path cache;

    private static OcrEngine engine;

    @BeforeAll
    static void start() throws Exception {
        engine = OcrEngine.open(Tessdata.install(cache));
    }

    @AfterAll
    static void stop() {
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    void readsDarkTextOnLight() {
        BufferedImage image = render(Color.WHITE, Color.BLACK,
                "Oak Planks 128", "Cobblestone 64", "Glass Pane 3 stacks");
        String text = engine.recognize(image).toLowerCase(Locale.ROOT);
        assertTrue(text.contains("oak planks"), text);
        assertTrue(text.contains("128"), text);
        assertTrue(text.contains("cobblestone"), text);
        assertTrue(text.contains("glass pane"), text);
    }

    @Test
    void readsLightTextOnDarkLikeAGameMenu() {
        BufferedImage image = render(new Color(24, 24, 24), new Color(230, 230, 230),
                "Stone Bricks 1,024", "Spruce Log 96");
        String text = engine.recognize(image).toLowerCase(Locale.ROOT);
        assertTrue(text.contains("stone bricks"), text);
        assertTrue(text.contains("spruce log"), text);
        assertTrue(text.contains("96"), text);
    }

    private static BufferedImage render(Color background, Color ink, String... lines) {
        BufferedImage image = new BufferedImage(420, 40 + lines.length * 34, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(background);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(ink);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], 16, 40 + i * 34);
        }
        g.dispose();
        return image;
    }
}
