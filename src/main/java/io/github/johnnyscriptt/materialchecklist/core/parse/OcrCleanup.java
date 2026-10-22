package io.github.johnnyscriptt.materialchecklist.core.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Repairs the mistakes OCR makes on material lists before the {@link ListParser} sees them.
 *
 * <ul>
 *   <li>Inside a number, letters that look like digits are read back as digits: O for 0, l and
 *       I for 1, S for 5, Z for 2, B for 8. Amounts with a unit ("2SB") are left alone.</li>
 *   <li>Table borders come back as stray "|", "l" or "I" tokens; those become column breaks,
 *       as do dot leaders ("Lantern ........ 9").</li>
 *   <li>Lines with no word in them (border rows, icon noise) are dropped.</li>
 * </ul>
 */
public final class OcrCleanup {
    private static final Pattern DOT_LEADER = Pattern.compile("\\s+[.·]{3,}\\s*|[.·]{5,}\\s*");
    private static final Pattern BORDER_TOKEN = Pattern.compile("[|lI\\[\\]!¦]+");
    private static final Pattern UNIT_AMOUNT = Pattern.compile("(?i)\\d+(sb|st|x)");
    private static final Pattern DIGIT_LIKE = Pattern.compile("[0-9OoIlSZB,.]+");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
    private static final Pattern STRAY_MARKS = Pattern.compile("^[_~'`\"]+|[_~'`\"]+$");
    private static final Pattern WORD = Pattern.compile("\\p{L}{2,}");
    private static final String BREAK = "\t";

    private OcrCleanup() {
    }

    public static String clean(String ocrText) {
        List<String> out = new ArrayList<>();
        for (String raw : ocrText.split("\\R")) {
            String line = cleanLine(raw);
            if (WORD.matcher(line).find()) {
                out.add(line);
            }
        }
        return String.join("\n", out);
    }

    private static String cleanLine(String raw) {
        String s = DOT_LEADER.matcher(raw).replaceAll(BREAK).replace("|", " | ");
        List<String> parts = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        for (String token : s.split("[ ]+")) {
            if (token.isEmpty()) {
                continue;
            }
            if (token.equals(BREAK) || BORDER_TOKEN.matcher(token).matches()) {
                flush(parts, cell);
                continue;
            }
            String[] pieces = token.split(BREAK, -1);
            for (int i = 0; i < pieces.length; i++) {
                if (i > 0) {
                    flush(parts, cell); // a dot leader sat between these pieces
                }
                if (pieces[i].isEmpty()) {
                    continue;
                }
                if (!cell.isEmpty()) {
                    cell.append(' ');
                }
                cell.append(fixDigits(pieces[i]));
            }
        }
        flush(parts, cell);
        if (!parts.isEmpty()) {
            parts.set(0, dropIconScraps(parts.getFirst()));
        }
        return String.join(BREAK, parts);
    }

    /**
     * An item icon in front of the name comes back as a scrap: "e", "be", "(P", "___". A token
     * of at most two characters with no digit, or with no letter at all, is dropped while a
     * word follows it.
     */
    private static String dropIconScraps(String firstCell) {
        String[] tokens = firstCell.split(" ");
        int start = 0;
        while (start < tokens.length - 1 && isScrap(tokens[start])
                && !tokens[start + 1].isEmpty() && Character.isLetter(tokens[start + 1].charAt(0))) {
            start++;
        }
        return String.join(" ", java.util.Arrays.copyOfRange(tokens, start, tokens.length));
    }

    private static boolean isScrap(String token) {
        if (HAS_DIGIT.matcher(token).find()) {
            return false;
        }
        return token.length() <= 2 || !WORD.matcher(token).find() && token.chars().noneMatch(Character::isLetter);
    }

    private static void flush(List<String> parts, StringBuilder cell) {
        if (!cell.isEmpty()) {
            parts.add(cell.toString());
            cell.setLength(0);
        }
    }

    private static String fixDigits(String raw) {
        // A text cursor or a speck of noise often sticks to the end of a number ("640_").
        String token = HAS_DIGIT.matcher(raw).find() ? STRAY_MARKS.matcher(raw).replaceAll("") : raw;
        if (token.isEmpty()) {
            return raw;
        }
        if (!HAS_DIGIT.matcher(token).find() || UNIT_AMOUNT.matcher(token).matches()
                || !DIGIT_LIKE.matcher(token).matches()) {
            return token;
        }
        StringBuilder out = new StringBuilder(token.length());
        for (char c : token.toCharArray()) {
            out.append(switch (c) {
                case 'O', 'o' -> '0';
                case 'I', 'l' -> '1';
                case 'S' -> '5';
                case 'Z' -> '2';
                case 'B' -> '8';
                default -> c;
            });
        }
        return out.toString();
    }
}
