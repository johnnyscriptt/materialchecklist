package io.github.johnnyscriptt.materialchecklist.core.count;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StackMathTest {
    private static final int STACK = 64;
    private static final long SHULKER = 27L * 64;

    @Test
    void formatsSmallCountsAsPlainNumbers() {
        assertEquals("0", StackMath.format(0, STACK));
        assertEquals("40", StackMath.format(40, STACK));
    }

    @Test
    void formatsStacksAndRemainder() {
        assertEquals("1 st", StackMath.format(64, STACK));
        assertEquals("19 st + 18", StackMath.format(19 * 64 + 18, STACK));
    }

    @Test
    void formatsShulkerBoxes() {
        assertEquals("1 sb", StackMath.format(SHULKER, STACK));
        assertEquals("2 sb + 4 st + 16", StackMath.format(2 * SHULKER + 4 * 64 + 16, STACK));
    }

    @Test
    void formatUsesTheItemsOwnStackSize() {
        assertEquals("3 st + 4", StackMath.format(52, 16));
        assertEquals("5", StackMath.format(5, 1));
    }

    @Test
    void parsesPlainNumbersWithThousandsSeparators() {
        assertEquals(OptionalLong.of(64), StackMath.parse("64", STACK));
        assertEquals(OptionalLong.of(1234), StackMath.parse("1,234", STACK));
        assertEquals(OptionalLong.of(12345), StackMath.parse("12.345", STACK));
    }

    @Test
    void parsesMultiplierMarks() {
        assertEquals(OptionalLong.of(64), StackMath.parse("x64", STACK));
        assertEquals(OptionalLong.of(64), StackMath.parse("64x", STACK));
        assertEquals(OptionalLong.of(19 * 64 + 18), StackMath.parse("19 x 64 + 18", STACK));
        assertEquals(OptionalLong.of(3 * 64), StackMath.parse("3×64", STACK));
    }

    @Test
    void parsesStackWords() {
        assertEquals(OptionalLong.of(3 * 64), StackMath.parse("3 stacks", STACK));
        assertEquals(OptionalLong.of(64), StackMath.parse("1 stack", STACK));
        assertEquals(OptionalLong.of(3 * 64 + 5), StackMath.parse("3st + 5", STACK));
        assertEquals(OptionalLong.of(48), StackMath.parse("3 stacks", 16));
    }

    @Test
    void parsesShulkerAndChestWords() {
        assertEquals(OptionalLong.of(2 * SHULKER), StackMath.parse("2 shulkers", STACK));
        assertEquals(OptionalLong.of(2 * SHULKER), StackMath.parse("2SB", STACK));
        assertEquals(OptionalLong.of(SHULKER), StackMath.parse("1 shulker box", STACK));
        assertEquals(OptionalLong.of(SHULKER + 3 * 64 + 5), StackMath.parse("1sb+3st+5", STACK));
        assertEquals(OptionalLong.of(SHULKER + 3 * 64 + 5), StackMath.parse("1 sb 3 st 5", STACK));
        assertEquals(OptionalLong.of(54L * 64), StackMath.parse("1 dc", STACK));
    }

    @Test
    void anEqualsOrBracketRestatesTheSameAmountRatherThanAddingToIt() {
        assertEquals(OptionalLong.of(1234), StackMath.parse("1234 (19 x 64 + 18)", STACK));
        assertEquals(OptionalLong.of(1234), StackMath.parse("1234 = 19 x 64 + 18", STACK));
        assertEquals(OptionalLong.of(1234), StackMath.parse("(19 x 64 + 18)", STACK));
    }

    @Test
    void rejectsTextThatIsNotACount() {
        assertTrue(StackMath.parse("oak planks", STACK).isEmpty());
        assertTrue(StackMath.parse("64 oak", STACK).isEmpty());
        assertTrue(StackMath.parse("", STACK).isEmpty());
        assertTrue(StackMath.parse("stacks", STACK).isEmpty());
        // Words before an "=" or a bracket are a name, not an amount being restated.
        assertTrue(StackMath.parse("Flank= 512", STACK).isEmpty());
        assertTrue(StackMath.parse("planks (512)", STACK).isEmpty());
    }
}
