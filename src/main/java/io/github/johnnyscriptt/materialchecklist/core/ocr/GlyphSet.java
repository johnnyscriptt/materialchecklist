package io.github.johnnyscriptt.materialchecklist.core.ocr;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bitmap glyph shapes on an 8-row grid, as the game's pixel font defines them. Read at run time
 * from the font texture the player's game is actually using (so a resource pack's font is
 * honoured), never shipped with the mod.
 */
public final class GlyphSet {
    /**
     * What an amount in a material list is written with, plus the underscore a text cursor
     * draws when the list was screenshotted mid-edit.
     */
    public static final String NUMBER_CHARS = "0123456789,.x_";
    static final int ROWS = 8;

    record Glyph(char character, boolean[][] ink, int width) {
    }

    private final List<Glyph> glyphs;

    private GlyphSet(List<Glyph> glyphs) {
        this.glyphs = List.copyOf(glyphs);
    }

    List<Glyph> glyphs() {
        return glyphs;
    }

    /** The glyph for {@code c}, or null if this set doesn't have it. */
    Glyph glyph(char c) {
        for (Glyph glyph : glyphs) {
            if (glyph.character() == c) {
                return glyph;
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return glyphs.isEmpty();
    }

    /** @param patterns rows of '#' (ink) and '.' per character, eight rows each */
    public static GlyphSet of(Map<Character, String[]> patterns) {
        List<Glyph> out = new ArrayList<>();
        patterns.forEach((c, rows) -> {
            int width = rows[0].length();
            boolean[][] ink = new boolean[ROWS][width];
            for (int r = 0; r < ROWS && r < rows.length; r++) {
                for (int x = 0; x < width; x++) {
                    ink[r][x] = rows[r].charAt(x) == '#';
                }
            }
            out.add(new Glyph(c, ink, width));
        });
        return new GlyphSet(out);
    }

    /**
     * Reads characters out of a 16 by 16 character atlas laid out like the vanilla
     * {@code ascii.png}, where the cell of character {@code c} is at column {@code c % 16}, row
     * {@code c / 16}. High-resolution packs (16 px or 32 px cells) are sampled down to 8 rows.
     */
    public static GlyphSet fromAtlas(BufferedImage atlas, String chars) {
        int cell = atlas.getWidth() / 16;
        if (cell < ROWS || atlas.getHeight() / 16 != cell) {
            return new GlyphSet(List.of());
        }
        int sub = cell / ROWS;
        List<Glyph> out = new ArrayList<>();
        for (char c : chars.toCharArray()) {
            if (c >= 256) {
                continue;
            }
            int cx = (c % 16) * cell;
            int cy = (c / 16) * cell;
            boolean[][] full = new boolean[ROWS][ROWS];
            int width = 0;
            for (int r = 0; r < ROWS; r++) {
                for (int x = 0; x < ROWS; x++) {
                    int inked = 0;
                    for (int dy = 0; dy < sub; dy++) {
                        for (int dx = 0; dx < sub; dx++) {
                            if ((atlas.getRGB(cx + x * sub + dx, cy + r * sub + dy) >>> 24) > 0) {
                                inked++;
                            }
                        }
                    }
                    full[r][x] = inked * 2 >= sub * sub;
                    if (full[r][x]) {
                        width = Math.max(width, x + 1);
                    }
                }
            }
            if (width == 0) {
                continue;
            }
            boolean[][] ink = new boolean[ROWS][width];
            for (int r = 0; r < ROWS; r++) {
                System.arraycopy(full[r], 0, ink[r], 0, width);
            }
            out.add(new Glyph(c, ink, width));
        }
        return new GlyphSet(out);
    }
}
