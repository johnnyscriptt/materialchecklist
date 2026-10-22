package io.github.johnnyscriptt.materialchecklist.core.ocr;

import java.awt.Rectangle;
import java.util.List;

/**
 * Finds words that sit in a column of numbers but didn't come back as one. In a material-list
 * table, a cell lined up under other amounts is an amount too, whatever Tesseract made of it
 * ("a" for 4, "r)" for 0), so it deserves the lenient number reading.
 */
final class NumberColumns {
    /** Edges this many word-heights apart still count as lined up. */
    private static final double ALIGN = 0.6;
    /** Numbers needed above or below before a run of cells counts as a column. */
    private static final int MIN_NUMBERS = 2;

    private NumberColumns() {
    }

    /** @return for each box, true when it isn't numeric but is lined up with a column of numbers */
    static boolean[] suspects(List<Rectangle> boxes, boolean[] numeric) {
        boolean[] out = new boolean[boxes.size()];
        for (int i = 0; i < boxes.size(); i++) {
            if (numeric[i]) {
                continue;
            }
            Rectangle box = boxes.get(i);
            double tolerance = ALIGN * box.height;
            int aligned = 0;
            for (int j = 0; j < boxes.size(); j++) {
                if (!numeric[j]) {
                    continue;
                }
                Rectangle other = boxes.get(j);
                boolean left = Math.abs(box.x - other.x) <= tolerance;
                boolean right = Math.abs((box.x + box.width) - (other.x + other.width)) <= tolerance;
                if (left || right) {
                    aligned++;
                }
            }
            out[i] = aligned >= MIN_NUMBERS;
        }
        return out;
    }
}
