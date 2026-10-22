package io.github.johnnyscriptt.materialchecklist.core.ocr;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * A support diagnostic: reads a line of text it draws itself, end to end through the natives
 * unpacked from the installed jar. Started with {@code -Dmaterialchecklist.ocrSelfTest=true}
 * on client or server, so a "screenshot import does nothing" report can be narrowed to whether
 * the OCR engine loads on that machine at all.
 */
public final class OcrSelfTest {
    public static final String PROPERTY = "materialchecklist.ocrSelfTest";
    static final String SAMPLE = "Oak Planks 128";

    private OcrSelfTest() {
    }

    public static boolean requested() {
        return Boolean.getBoolean(PROPERTY);
    }

    /** @return the recognised text, trimmed. */
    public static String run(Path cacheDir) throws Exception {
        BufferedImage image = new BufferedImage(360, 60, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
        g.drawString(SAMPLE, 16, 38);
        g.dispose();
        try (OcrEngine engine = OcrEngine.open(Tessdata.install(cacheDir))) {
            return engine.recognize(image).trim();
        }
    }
}
