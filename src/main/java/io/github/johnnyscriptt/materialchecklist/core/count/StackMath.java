package io.github.johnnyscriptt.materialchecklist.core.count;

import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Item counts in the units builders use: plain numbers, stacks and shulker boxes, in both
 * directions. A stack is the item's own stack size, so "3 stacks" of ender pearls is 48.
 */
public final class StackMath {
    public static final int SHULKER_SLOTS = 27;
    public static final int DOUBLE_CHEST_SLOTS = 54;

    /** One amount: "12", "x64", "64x", "19 x 64", or a number with a unit word after it. */
    private static final Pattern TERM = Pattern.compile(
            "(\\d+)\\s*x\\s*(\\d+)"
                    + "|x\\s*(\\d+)"
                    + "|(\\d+)\\s*(shulker\\s*boxes|shulker\\s*box|shulkers|shulker|sbs|sb"
                    + "|double\\s*chests|double\\s*chest|dcs|dc|stacks|stack|stks|stk|st|x)?");
    private static final Pattern SEPARATOR = Pattern.compile("\\s*and\\s+|[\\s+,]*");
    private static final Pattern THOUSANDS = Pattern.compile("(?<=\\d)[,.](?=\\d{3}(?!\\d))");

    private StackMath() {
    }

    /** "19 st + 18", "1 sb + 4 st + 16", or just "40" when it is under a stack. */
    public static String format(long count, int stackSize) {
        if (stackSize <= 1 || count < stackSize) {
            return Long.toString(count);
        }
        long shulker = (long) SHULKER_SLOTS * stackSize;
        long boxes = count / shulker;
        long stacks = (count % shulker) / stackSize;
        long items = count % stackSize;
        StringBuilder out = new StringBuilder();
        append(out, boxes, " sb");
        append(out, stacks, " st");
        append(out, items, "");
        return out.toString();
    }

    private static void append(StringBuilder out, long value, String unit) {
        if (value == 0) {
            return;
        }
        if (!out.isEmpty()) {
            out.append(" + ");
        }
        out.append(value).append(unit);
    }

    /**
     * Reads an amount such as "1,234", "3 stacks", "2SB", "1sb+3st+5" or "19 x 64 + 18".
     * Brackets and "=" restate an amount ("1234 (19 x 64 + 18)"), so only one side counts.
     *
     * @return empty unless the whole text is an amount
     */
    public static OptionalLong parse(String text, int stackSize) {
        if (text == null) {
            return OptionalLong.empty();
        }
        String s = text.toLowerCase(Locale.ROOT).replace('×', 'x').strip();
        // The first statement of the amount decides; the restatement only stands in when the
        // first part is empty. Anything else before it ("Flank= 512") is a name, not an amount.
        int equals = s.indexOf('=');
        if (equals >= 0) {
            String left = s.substring(0, equals);
            return left.isBlank() ? parse(s.substring(equals + 1), stackSize) : parse(left, stackSize);
        }
        int open = s.indexOf('(');
        if (open >= 0) {
            int close = s.indexOf(')', open);
            String inside = s.substring(open + 1, close < 0 ? s.length() : close);
            String outside = s.substring(0, open) + (close < 0 ? "" : s.substring(close + 1));
            return outside.isBlank() ? parse(inside, stackSize) : parse(outside, stackSize);
        }
        return sum(THOUSANDS.matcher(s).replaceAll(""), Math.max(1, stackSize));
    }

    private static OptionalLong sum(String s, int stackSize) {
        Matcher term = TERM.matcher(s);
        long total = 0;
        int pos = 0;
        int terms = 0;
        while (pos < s.length()) {
            Matcher gap = SEPARATOR.matcher(s).region(pos, s.length());
            if (gap.lookingAt()) {
                pos = gap.end();
            }
            if (pos >= s.length()) {
                break;
            }
            term.region(pos, s.length());
            if (!term.lookingAt() || term.end() == pos) {
                return OptionalLong.empty();
            }
            total += value(term, stackSize);
            terms++;
            pos = term.end();
        }
        return terms == 0 ? OptionalLong.empty() : OptionalLong.of(total);
    }

    private static long value(Matcher term, int stackSize) {
        if (term.group(1) != null) {
            return Long.parseLong(term.group(1)) * Long.parseLong(term.group(2));
        }
        if (term.group(3) != null) {
            return Long.parseLong(term.group(3));
        }
        long n = Long.parseLong(term.group(4));
        String unit = term.group(5);
        if (unit == null || unit.equals("x")) {
            return n;
        }
        if (unit.startsWith("sh") || unit.startsWith("sb")) {
            return n * SHULKER_SLOTS * stackSize;
        }
        if (unit.startsWith("d")) {
            return n * DOUBLE_CHEST_SLOTS * stackSize;
        }
        return n * stackSize;
    }
}
