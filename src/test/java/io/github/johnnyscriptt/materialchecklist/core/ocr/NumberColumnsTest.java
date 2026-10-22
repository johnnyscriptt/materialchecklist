package io.github.johnnyscriptt.materialchecklist.core.ocr;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class NumberColumnsTest {
    private static Rectangle box(int x, int y, int w) {
        return new Rectangle(x, y, w, 14);
    }

    @Test
    void aWordLinedUpUnderNumbersIsInANumberColumn() {
        // Three rows: name at x=20, total at x=200. Row three's total came back as letters.
        List<Rectangle> boxes = List.of(
                box(20, 0, 80), box(200, 0, 30),
                box(20, 20, 90), box(200, 20, 24),
                box(20, 40, 70), box(201, 40, 9));
        boolean[] numeric = {false, true, false, true, false, false};
        assertArrayEquals(new boolean[]{false, false, false, false, false, true},
                NumberColumns.suspects(boxes, numeric));
    }

    @Test
    void rightAlignedColumnsCountToo() {
        List<Rectangle> boxes = List.of(box(170, 0, 40), box(186, 20, 24), box(201, 40, 9));
        boolean[] numeric = {true, true, false};
        assertArrayEquals(new boolean[]{false, false, true}, NumberColumns.suspects(boxes, numeric));
    }

    @Test
    void oneNumberIsNotAColumn() {
        List<Rectangle> boxes = List.of(box(200, 0, 30), box(200, 20, 30));
        boolean[] numeric = {true, false};
        assertArrayEquals(new boolean[]{false, false}, NumberColumns.suspects(boxes, numeric));
    }

    @Test
    void namesBesideTheColumnAreLeftAlone() {
        List<Rectangle> boxes = List.of(box(200, 0, 30), box(200, 20, 30), box(20, 40, 120));
        boolean[] numeric = {true, true, false};
        assertArrayEquals(new boolean[]{false, false, false}, NumberColumns.suspects(boxes, numeric));
    }
}
