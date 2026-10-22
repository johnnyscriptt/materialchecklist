package io.github.johnnyscriptt.materialchecklist.core.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Resolves the item names people type, and OCR reads, to real items.
 *
 * <p>Both sides go through the same normalisation (case, punctuation, namespace, and a crude
 * singular form of each word), so "oak logs", "Oak_Log" and "minecraft:oak_log" all land on
 * Oak Log. The singular rule doesn't need to be good English, only consistent: "Oak Leaves"
 * and "oak leaves" become the same odd word and still match.
 *
 * <p>Anything not exact is scored by edit distance, which is what OCR mistakes look like
 * ("Cobb1estone"). Names a menu cut short ("Polished Blackstone Brick St...") match by prefix.
 */
public final class ItemNameMatcher {
    /** Below this the guess is more likely wrong than right, so the line is left unmatched. */
    public static final double MIN_SCORE = 0.6;
    /** At or above this a line is accepted without asking the player to check it. */
    public static final double CONFIDENT = 0.85;

    private static final Pattern REGISTRY_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_/.-]+");
    private static final double PREFIX_UNIQUE = 0.95;
    private static final double PREFIX_SHARED = 0.88;

    private final List<Normalized> candidates;

    public record Match(Candidate candidate, double score) {
        public boolean confident() {
            return score >= CONFIDENT;
        }
    }

    private record Normalized(Candidate candidate, String name, String id) {
    }

    public ItemNameMatcher(List<Candidate> items) {
        List<Normalized> list = new ArrayList<>(items.size());
        for (Candidate item : items) {
            list.add(new Normalized(item, normalize(item.displayName()), normalize(item.id())));
        }
        this.candidates = List.copyOf(list);
    }

    /** @return the closest item, or null when nothing is close enough to be worth suggesting. */
    public Match best(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.strip();
        boolean truncated = trimmed.endsWith("...") || trimmed.endsWith("…");
        String query = normalize(trimmed);
        if (query.isEmpty()) {
            return null;
        }
        if (truncated) {
            Match prefix = byPrefix(query);
            if (prefix != null) {
                return prefix;
            }
        }

        Normalized bestItem = null;
        double bestScore = 0;
        for (Normalized item : candidates) {
            double score = Math.max(similarity(query, item.name()), similarity(query, item.id()));
            // Ties go to the shorter name: "Stone" over "Stone Bricks" for a near-miss of either.
            if (score > bestScore || (score == bestScore && bestItem != null
                    && item.name().length() < bestItem.name().length())) {
                bestScore = score;
                bestItem = item;
            }
        }
        return bestItem == null || bestScore < MIN_SCORE ? null : new Match(bestItem.candidate(), bestScore);
    }

    private Match byPrefix(String prefix) {
        Normalized shortest = null;
        int found = 0;
        for (Normalized item : candidates) {
            if (item.name().startsWith(prefix)) {
                found++;
                if (shortest == null || item.name().length() < shortest.name().length()) {
                    shortest = item;
                }
            }
        }
        return shortest == null ? null
                : new Match(shortest.candidate(), found == 1 ? PREFIX_UNIQUE : PREFIX_SHARED);
    }

    static double similarity(String a, String b) {
        if (a.equals(b)) {
            return 1.0;
        }
        int longest = Math.max(a.length(), b.length());
        return longest == 0 ? 0.0 : Math.max(0.0, 1.0 - editDistance(a, b) / longest);
    }

    /**
     * Edit distance where swapping letters OCR mixes up costs less than a real typo. In the
     * game's pixel font B and G, P and F, O and D sit a pixel or two apart.
     */
    private static double editDistance(String a, String b) {
        double[] previous = new double[b.length() + 1];
        double[] current = new double[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                double cost = substitution(a.charAt(i - 1), b.charAt(j - 1));
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            double[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }

    private static double substitution(char x, char y) {
        if (x == y) {
            return 0;
        }
        String pair = x < y ? "" + x + y : "" + y + x;
        return LOOKALIKES.contains(pair) ? LOOKALIKE_COST : 1;
    }

    /** Sorted letter pairs OCR swaps, and what swapping one costs. */
    private static final Set<String> LOOKALIKES = Set.of("bg", "fp", "do", "ce", "il", "1l", "0o", "5s", "8b", "hn", "uv");
    private static final double LOOKALIKE_COST = 0.4;

    static String normalize(String text) {
        String s = text.toLowerCase(Locale.ROOT);
        if (REGISTRY_ID.matcher(s).matches()) {
            s = s.substring(s.indexOf(':') + 1); // drop the namespace
        }
        s = s.replace('_', ' ').replaceAll("[^a-z0-9 ]", " ").strip();
        if (s.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String word : s.split("\\s+")) {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(singular(word));
        }
        return out.toString();
    }

    private static String singular(String word) {
        if (word.length() <= 3) {
            return word;
        }
        if (word.endsWith("ies")) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.endsWith("xes") || word.endsWith("ches") || word.endsWith("shes") || word.endsWith("sses")) {
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }
}
