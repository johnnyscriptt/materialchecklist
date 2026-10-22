package io.github.johnnyscriptt.materialchecklist.core.ocr;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reads a number drawn in the game's pixel font by matching it against the font's own glyphs.
 *
 * <p>General OCR models trip over this font: its zero has a slash through it and reads as 8 or
 * 6. But the font is regular enough to read directly. Every digit is five font pixels wide and
 * seven tall on a six-pixel pitch, the drop shadow is a quarter as bright as the text, and the
 * GUI scale makes every font pixel the same number of screen pixels. So: cut ink from shadow
 * and background at half the text brightness, measure the font pixel size from the digit
 * height, slice the ink into glyphs, and compare each with the templates.
 *
 * <p>Anything that doesn't match well is refused (null), and the caller keeps what Tesseract
 * read instead; the reader only ever replaces text it is sure of.
 */
public final class BitmapDigitReader {
    /** Share of a glyph's cells allowed to disagree with the template. */
    static final double MAX_DISTANCE = 0.14;
    /**
     * The allowance when the word is already known to hold digits, so the question is only
     * which digits. Rescaled web screenshots land around 1.6 screen pixels per font pixel,
     * where blur and shadow leave even the right glyph a few cells off.
     */
    static final double LENIENT_DISTANCE = 0.22;
    /** A cell counts as ink when this much of it is ink. */
    private static final double CELL_INK = 0.4;
    /** Contrast below which there is no text worth reading. */
    private static final int MIN_CONTRAST = 50;
    private static final int DIGIT_ROWS = 7;

    private BitmapDigitReader() {
    }

    /**
     * @param box roughly where the word is, in image coordinates (a little slack either way is fine)
     * @return the characters read, or null when they don't look like this font
     */
    public static String read(BufferedImage image, Rectangle box, GlyphSet glyphs) {
        return read(image, box, glyphs, false);
    }

    /** @param lenient the word is known to be a number, so tolerate a softer match */
    public static String read(BufferedImage image, Rectangle box, GlyphSet glyphs, boolean lenient) {
        double maxDistance = lenient ? LENIENT_DISTANCE : MAX_DISTANCE;
        if (glyphs.isEmpty() || box.width <= 0 || box.height <= 0) {
            return null;
        }
        int marginX = Math.round(box.height * 0.3f);
        int marginY = Math.round(box.height * 0.12f);
        Rectangle crop = new Rectangle(box.x - marginX, box.y - marginY,
                box.width + 2 * marginX, box.height + 2 * marginY).intersection(
                new Rectangle(0, 0, image.getWidth(), image.getHeight()));
        if (crop.isEmpty()) {
            return null;
        }
        boolean[][] ink = inkMask(image, crop);
        if (ink == null) {
            return null;
        }

        // Ink columns that touch the word's own box; anything else is a neighbour.
        int boxLeft = box.x - crop.x;
        int boxRight = boxLeft + box.width;
        List<int[]> runs = new ArrayList<>();
        int w = crop.width;
        int h = crop.height;
        int start = -1;
        for (int x = 0; x <= w; x++) {
            boolean has = x < w && columnHasInk(ink, x, 0, h);
            if (has && start < 0) {
                start = x;
            } else if (!has && start >= 0) {
                if (x > boxLeft && start < boxRight) {
                    runs.add(new int[]{start, x});
                }
                start = -1;
            }
        }
        if (runs.isEmpty()) {
            return null;
        }
        int top = -1;
        int bottom = -1;
        for (int y = 0; y < h; y++) {
            for (int[] run : runs) {
                if (rowHasInk(ink, y, run[0], run[1])) {
                    if (top < 0) {
                        top = y;
                    }
                    bottom = y;
                    break;
                }
            }
        }
        int height = bottom - top + 1;
        if (top < 0 || height < 5) {
            return null;
        }

        // Digits are seven rows tall; a comma reaches an eighth. Try both, keep the better read.
        Read best = null;
        for (int rows : new int[]{DIGIT_ROWS, DIGIT_ROWS + 1}) {
            Read read = readRuns(ink, runs, top, (double) height / rows, glyphs, maxDistance);
            if (read != null && (best == null || read.distance < best.distance)) {
                best = read;
            }
        }
        return best == null ? null : best.text;
    }

    private record Read(String text, double distance) {
    }

    private static Read readRuns(boolean[][] ink, List<int[]> runs, int top, double s, GlyphSet glyphs,
                                 double maxDistance) {
        StringBuilder text = new StringBuilder();
        double total = 0;
        int count = 0;
        int previousEnd = -1;
        for (int[] run : runs) {
            if (previousEnd >= 0 && run[0] - previousEnd > 2.5 * s) {
                text.append(' ');
            }
            previousEnd = run[1];
            double width = run[1] - run[0];
            int pieces = width < 3 * s ? 1 : Math.max(1, (int) Math.round((width + s) / (6 * s)));
            double pitch = (width + s) / pieces;
            double pieceWidth = pieces == 1 ? width : pitch - s;
            for (int i = 0; i < pieces; i++) {
                double x0 = run[0] + i * pitch;
                Match match = classify(ink, x0, pieceWidth, top, s, glyphs, maxDistance);
                if (match == null) {
                    return null;
                }
                text.append(match.character);
                total += match.distance;
                count++;
            }
        }
        return count == 0 ? null : new Read(text.toString(), total / count);
    }

    private record Match(char character, double distance) {
    }

    private static Match classify(boolean[][] ink, double x0, double pieceWidth, int top, double s, GlyphSet glyphs,
                                  double maxDistance) {
        double fontWidth = pieceWidth / s;
        Match best = null;
        for (GlyphSet.Glyph glyph : glyphs.glyphs()) {
            if (Math.abs(glyph.width() - fontWidth) > 1.0) {
                continue;
            }
            int mismatches = 0;
            double cellW = pieceWidth / glyph.width();
            for (int r = 0; r < GlyphSet.ROWS; r++) {
                for (int c = 0; c < glyph.width(); c++) {
                    boolean seen = fraction(ink, x0 + c * cellW, x0 + (c + 1) * cellW,
                            top + r * s, top + (r + 1) * s) >= CELL_INK;
                    if (seen != glyph.ink()[r][c]) {
                        mismatches++;
                    }
                }
            }
            double distance = (double) mismatches / (glyph.width() * GlyphSet.ROWS)
                    + 0.04 * Math.abs(glyph.width() - fontWidth);
            distance = Math.min(distance, shadowedDistance(ink, glyph, x0, fontWidth, top, s));
            distance += glyph.penalty();
            if (best == null || distance < best.distance) {
                best = new Match(glyph.character(), distance);
            }
        }
        return best == null || best.distance > maxDistance ? null : best;
    }

    /**
     * The same comparison, for when the drop shadow has bled into the ink (a rescaled or
     * compressed screenshot): the glyph is one column wider and one row taller, and the cells
     * only the shadow covers could be either, so they are left out of the count.
     */
    private static double shadowedDistance(boolean[][] ink, GlyphSet.Glyph glyph, double x0, double fontWidth,
                                           int top, double s) {
        int cols = glyph.width() + 1;
        int mismatches = 0;
        int counted = 0;
        for (int r = 0; r < GlyphSet.ROWS; r++) {
            for (int c = 0; c < cols; c++) {
                boolean want = c < glyph.width() && glyph.ink()[r][c];
                boolean shadowOnly = !want && r > 0 && c > 0 && glyph.ink()[r - 1][c - 1];
                if (shadowOnly) {
                    continue;
                }
                boolean seen = fraction(ink, x0 + c * s, x0 + (c + 1) * s, top + r * s, top + (r + 1) * s) >= CELL_INK;
                counted++;
                if (seen != want) {
                    mismatches++;
                }
            }
        }
        return counted == 0 ? 1.0 : (double) mismatches / counted + 0.04 * Math.abs(cols - fontWidth);
    }

    /** Share of ink pixels in a cell given in fractional crop coordinates. */
    private static double fraction(boolean[][] ink, double x1, double x2, double y1, double y2) {
        int h = ink.length;
        int w = ink[0].length;
        int xa = Math.max(0, (int) Math.floor(x1));
        int xb = Math.min(w, Math.max(xa + 1, (int) Math.ceil(x2)));
        int ya = Math.max(0, (int) Math.floor(y1));
        int yb = Math.min(h, Math.max(ya + 1, (int) Math.ceil(y2)));
        double inked = 0;
        double area = 0;
        for (int y = ya; y < yb; y++) {
            double wy = Math.min(y + 1, y2) - Math.max(y, y1);
            if (wy <= 0) {
                continue;
            }
            for (int x = xa; x < xb; x++) {
                double wx = Math.min(x + 1, x2) - Math.max(x, x1);
                if (wx <= 0) {
                    continue;
                }
                area += wx * wy;
                if (ink[y][x]) {
                    inked += wx * wy;
                }
            }
        }
        return area == 0 ? 0 : inked / area;
    }

    /**
     * Ink as true. Light text on a dark panel is read by its brightest channel, so coloured text
     * stands out; dark text on a light page by its darkest. The cut sits halfway between the
     * background and the text, which leaves the quarter-bright drop shadow out.
     */
    private static boolean[][] inkMask(BufferedImage image, Rectangle crop) {
        int w = crop.width;
        int h = crop.height;
        int[] rgb = image.getRGB(crop.x, crop.y, w, h, null, 0, w);
        int[] max = new int[rgb.length];
        int[] min = new int[rgb.length];
        for (int i = 0; i < rgb.length; i++) {
            int r = (rgb[i] >> 16) & 0xFF;
            int g = (rgb[i] >> 8) & 0xFF;
            int b = rgb[i] & 0xFF;
            max[i] = Math.max(r, Math.max(g, b));
            min[i] = Math.min(r, Math.min(g, b));
        }
        boolean lightBackground = percentile(max, 0.5) > 140;
        int[] value = new int[rgb.length];
        for (int i = 0; i < rgb.length; i++) {
            value[i] = lightBackground ? 255 - min[i] : max[i];
        }
        int background = percentile(value, 0.5);
        int text = percentile(value, 0.97);
        if (text - background < MIN_CONTRAST) {
            return null;
        }
        int threshold = background + (text - background) / 2;
        boolean[][] ink = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                ink[y][x] = value[y * w + x] > threshold;
            }
        }
        return ink;
    }

    private static int percentile(int[] values, double fraction) {
        int[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[Math.min(sorted.length - 1, (int) (sorted.length * fraction))];
    }

    private static boolean columnHasInk(boolean[][] ink, int x, int y0, int y1) {
        for (int y = y0; y < y1; y++) {
            if (ink[y][x]) {
                return true;
            }
        }
        return false;
    }

    private static boolean rowHasInk(boolean[][] ink, int y, int x0, int x1) {
        for (int x = x0; x < x1; x++) {
            if (ink[y][x]) {
                return true;
            }
        }
        return false;
    }
}
