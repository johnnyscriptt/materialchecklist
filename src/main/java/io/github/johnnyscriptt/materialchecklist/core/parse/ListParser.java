package io.github.johnnyscriptt.materialchecklist.core.parse;

import io.github.johnnyscriptt.materialchecklist.core.count.StackMath;
import io.github.johnnyscriptt.materialchecklist.core.list.Entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reads a material list out of free text: typed, pasted from a video description, a
 * spreadsheet or a Litematica export, or produced by OCR.
 *
 * <p>Each line is split into an item name and an amount. Lines with column breaks (pipes, tabs,
 * wide gaps, a colon or a spaced dash) take the first amount column after the name, which is
 * the "Total" in Litematica's tables. Other lines take an amount from the end ("Glass 64",
 * "Glass x64") or else the start ("64 Glass", "2 shulkers of glass").
 */
public final class ListParser {
    private static final Pattern CELL_BREAK = Pattern.compile("\\s*\\|\\s*|\\t+|\\s{2,}|\\s*:\\s+|\\s+[-–]\\s+");
    private static final Pattern BULLET = Pattern.compile("^[-*•·>]+\\s*");
    private static final Pattern NUMBERING = Pattern.compile("^\\d+[.)]\\s+");
    private static final Pattern BORDER = Pattern.compile("^[\\s+\\-=|_]*$");
    private static final Pattern HAS_LETTER = Pattern.compile("\\p{L}");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
    private static final Set<String> HEADER_WORDS = Set.of(
            "item", "items", "total", "missing", "available", "name", "amount", "count", "quantity",
            "qty", "block", "blocks", "material", "materials", "list", "needed", "required", "have");

    private static final int DEFAULT_STACK = 64;

    private final ItemNameMatcher matcher;

    public ListParser(ItemNameMatcher matcher) {
        this.matcher = matcher;
    }

    public List<ParsedLine> parse(String text) {
        List<ParsedLine> out = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            ParsedLine line = parseLine(raw);
            if (line != null) {
                out.add(line);
            }
        }
        return out;
    }

    /**
     * The accepted lines as list entries: unmatched lines are dropped, repeats of the same item
     * are added together, and a line with no amount counts as one.
     */
    public static List<Entry> toEntries(List<ParsedLine> lines) {
        Map<String, Long> totals = new LinkedHashMap<>();
        for (ParsedLine line : lines) {
            if (line.match() == null) {
                continue;
            }
            long count = line.count() == ParsedLine.NO_COUNT ? 1 : line.count();
            totals.merge(line.match().candidate().id(), count, Long::sum);
        }
        List<Entry> entries = new ArrayList<>();
        totals.forEach((id, count) -> entries.add(new Entry(id, count, false)));
        return entries;
    }

    private ParsedLine parseLine(String raw) {
        String s = raw.strip();
        if (s.isEmpty() || BORDER.matcher(s).matches() || isHeader(s)) {
            return null;
        }
        s = BULLET.matcher(s).replaceFirst("");
        String unnumbered = NUMBERING.matcher(s).replaceFirst("");
        if (!unnumbered.equals(s) && HAS_DIGIT.matcher(unnumbered).find()) {
            s = unnumbered;
        }

        List<String> cells = Arrays.stream(CELL_BREAK.split(s)).map(String::strip).filter(c -> !c.isEmpty()).toList();
        Split split = cells.size() >= 2 ? fromCells(cells) : null;
        if (split == null) {
            split = fromTokens(s);
        }
        if (split == null) {
            split = new Split(s, null);
        }

        String name = cleanName(split.name());
        if (!HAS_LETTER.matcher(name).find()) {
            return null;
        }
        ItemNameMatcher.Match match = matcher.best(name);
        long count = ParsedLine.NO_COUNT;
        if (split.countText() != null) {
            int stack = match == null ? DEFAULT_STACK : match.candidate().maxStackSize();
            count = StackMath.parse(split.countText(), stack).orElse(ParsedLine.NO_COUNT);
        }
        if (match == null && count == ParsedLine.NO_COUNT) {
            return null; // prose, not a list line
        }
        return new ParsedLine(raw.strip(), name, count, match);
    }

    private record Split(String name, String countText) {
    }

    private static Split fromCells(List<String> cells) {
        int nameIndex = -1;
        for (int i = 0; i < cells.size(); i++) {
            if (HAS_LETTER.matcher(cells.get(i)).find() && !isCount(cells.get(i))) {
                nameIndex = i;
                break;
            }
        }
        if (nameIndex < 0) {
            return null;
        }
        for (int i = nameIndex + 1; i < cells.size(); i++) {
            if (isCount(cells.get(i))) {
                return new Split(cells.get(nameIndex), cells.get(i));
            }
        }
        for (int i = 0; i < nameIndex; i++) {
            if (isCount(cells.get(i))) {
                return new Split(cells.get(nameIndex), cells.get(i));
            }
        }
        // No amount column; the name cell may still carry one ("Glass 12 | notes").
        return fromTokens(cells.get(nameIndex));
    }

    private static Split fromTokens(String s) {
        String[] tokens = s.split("\\s+");
        // Longest amount at the end that still leaves a name.
        for (int k = 1; k < tokens.length; k++) {
            String name = join(tokens, 0, k);
            String amount = join(tokens, k, tokens.length);
            if (HAS_LETTER.matcher(name).find() && isCount(amount)) {
                return new Split(name, amount);
            }
        }
        // Otherwise the longest amount at the start.
        for (int k = tokens.length - 1; k >= 1; k--) {
            String amount = join(tokens, 0, k);
            String name = join(tokens, k, tokens.length);
            if (HAS_LETTER.matcher(name).find() && isCount(amount)) {
                return new Split(name, amount);
            }
        }
        return null;
    }

    private static boolean isCount(String text) {
        OptionalLong value = StackMath.parse(text, DEFAULT_STACK);
        return value.isPresent();
    }

    private static String join(String[] tokens, int from, int to) {
        return String.join(" ", Arrays.copyOfRange(tokens, from, to));
    }

    private static String cleanName(String name) {
        String s = name.strip()
                .replaceFirst("^[^\\p{L}\\p{N}]+", "")
                .replaceFirst("[\\s,:;=|*\\-]+$", "");
        return s.replaceFirst("(?i)^of\\s+", "").strip();
    }

    private static boolean isHeader(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.contains("material list")) {
            return true;
        }
        String[] words = lower.replaceAll("[^a-z ]", " ").strip().split("\\s+");
        if (words.length < 2 || HAS_DIGIT.matcher(line).find()) {
            return false;
        }
        for (String word : words) {
            if (!HEADER_WORDS.contains(word)) {
                return false;
            }
        }
        return true;
    }
}
