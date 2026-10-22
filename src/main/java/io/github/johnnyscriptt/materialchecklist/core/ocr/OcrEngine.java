package io.github.johnnyscriptt.materialchecklist.core.ocr;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.tesseract.ETEXT_DESC;
import org.bytedeco.tesseract.ResultIterator;
import org.bytedeco.tesseract.TessBaseAPI;
import org.bytedeco.tesseract.global.tesseract;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Offline text recognition over Tesseract. Opening one loads the native libraries and the
 * model, which takes a second or two, so callers keep a single engine and reuse it. Tesseract
 * keeps per-image state, so recognition is serialised.
 *
 * <p>Tesseract reads words; amounts drawn in the game's own font are then re-read by
 * {@link BitmapDigitReader}, which knows that font's slashed zero. Where it isn't sure, the
 * Tesseract reading stands.
 */
public final class OcrEngine implements AutoCloseable {
    /**
     * Everything a material list is written with. Without this, digits in a pixel font come
     * back as accented letters ("96" read as "Fé"), which no amount of clean-up can undo.
     */
    static final String ALLOWED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            + " .,:;+-=x()[]|/'&#_";

    /** Gaps wider than this many word-heights are column breaks, not spaces. */
    private static final double COLUMN_GAP = 1.5;
    /**
     * Digits sit one font pixel apart and a space is four, against seven for the digit height;
     * a gap under this share of the height is inside one number.
     */
    private static final double SAME_NUMBER_GAP = 0.35;
    private static final Pattern NUMBER = Pattern.compile("[0-9][0-9,.]*");

    private final TessBaseAPI api;

    private OcrEngine(TessBaseAPI api) {
        this.api = api;
    }

    /** @param tessdataDir a folder holding the model, as returned by {@link Tessdata#install}. */
    public static OcrEngine open(Path tessdataDir) throws IOException {
        TessBaseAPI api = new TessBaseAPI();
        if (api.Init(tessdataDir.toAbsolutePath().toString(), Tessdata.LANGUAGE) != 0) {
            api.close();
            throw new IOException("Tesseract could not load its model from " + tessdataDir);
        }
        // A material list is one block of rows; reading it as such keeps each row on one line.
        api.SetPageSegMode(tesseract.PSM_SINGLE_BLOCK);
        api.SetVariable("tessedit_char_whitelist", ALLOWED);
        return new OcrEngine(api);
    }

    public String recognize(BufferedImage image) {
        return recognize(image, null);
    }

    /**
     * @param numberGlyphs the game font's digit shapes, or null to trust Tesseract's numbers
     * @return one line per text row; wide gaps between words become tabs
     */
    public synchronized String recognize(BufferedImage image, GlyphSet numberGlyphs) {
        ImagePrep.Gray gray = ImagePrep.prepare(image);
        api.SetImage(gray.pixels(), gray.width(), gray.height(), 1, gray.width());
        try {
            if (api.Recognize((ETEXT_DESC) null) != 0) {
                return "";
            }
            return assemble(image, gray.scale(), numberGlyphs);
        } finally {
            api.Clear();
        }
    }

    private record Word(String text, Rectangle box, boolean lineStart) {
    }

    private String assemble(BufferedImage image, int scale, GlyphSet numberGlyphs) {
        List<Word> words = words(scale);
        if (numberGlyphs != null) {
            words = rereadNumbers(image, words, numberGlyphs);
        }
        StringBuilder out = new StringBuilder();
        Word previous = null;
        for (Word word : words) {
            if (word.lineStart()) {
                if (!out.isEmpty()) {
                    out.append('\n');
                }
            } else if (previous != null) {
                boolean column = word.box().x - right(previous) > COLUMN_GAP * word.box().height;
                out.append(column ? '\t' : ' ');
            }
            out.append(word.text());
            previous = word;
        }
        return out.toString();
    }

    /** Tesseract's words with their boxes, mapped back from the enlarged image to the original. */
    private List<Word> words(int scale) {
        List<Word> out = new ArrayList<>();
        ResultIterator words = api.GetIterator();
        if (words == null) {
            return out;
        }
        int[] left = new int[1];
        int[] top = new int[1];
        int[] right = new int[1];
        int[] bottom = new int[1];
        try {
            do {
                BytePointer pointer = words.GetUTF8Text(tesseract.RIL_WORD);
                if (pointer == null) {
                    continue;
                }
                String text = pointer.getString(StandardCharsets.UTF_8);
                pointer.deallocate();
                if (!words.BoundingBox(tesseract.RIL_WORD, left, top, right, bottom)) {
                    continue;
                }
                Rectangle box = new Rectangle(left[0] / scale, top[0] / scale,
                        Math.max(1, (right[0] - left[0]) / scale), Math.max(1, (bottom[0] - top[0]) / scale));
                out.add(new Word(text, box, words.IsAtBeginningOf(tesseract.RIL_TEXTLINE)));
            } while (words.Next(tesseract.RIL_WORD));
        } finally {
            words.close();
        }
        return out;
    }

    /**
     * Re-reads every word with the game-font reader, then joins number fragments Tesseract split
     * apart ("2 0 3", "40 96"): pieces closer together than a space are one number. Every word
     * gets a try, not only ones with digits in, because Tesseract reads "1088" in this font as
     * "LASS"; the reader refuses real words, so nothing is lost.
     */
    private static List<Word> rereadNumbers(BufferedImage image, List<Word> words, GlyphSet glyphs) {
        List<Word> read = new ArrayList<>(words.size());
        for (Word word : words) {
            String digits = BitmapDigitReader.read(image, word.box(), glyphs);
            read.add(digits == null ? word : new Word(digits, word.box(), word.lineStart()));
        }
        List<Word> merged = new ArrayList<>(read.size());
        for (Word word : read) {
            Word last = merged.isEmpty() ? null : merged.getLast();
            if (last != null && !word.lineStart() && NUMBER.matcher(last.text()).matches()
                    && NUMBER.matcher(word.text()).matches()
                    && word.box().x - right(last) < SAME_NUMBER_GAP * Math.max(last.box().height, word.box().height)) {
                Rectangle union = last.box().union(word.box());
                String digits = BitmapDigitReader.read(image, union, glyphs);
                merged.set(merged.size() - 1, new Word(digits != null ? digits : last.text() + word.text(),
                        union, last.lineStart()));
            } else {
                merged.add(word);
            }
        }
        return merged;
    }

    private static int right(Word word) {
        return word.box().x + word.box().width;
    }

    @Override
    public synchronized void close() {
        api.End();
        api.close();
    }
}
