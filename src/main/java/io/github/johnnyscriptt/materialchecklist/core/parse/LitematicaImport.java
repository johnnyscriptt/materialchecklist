package io.github.johnnyscriptt.materialchecklist.core.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Litematica's material-list CSV export, turned into tab-separated "name, total" rows the
 * {@link ListParser} reads. The text-table export needs no conversion: the parser already
 * reads pipe-separated rows and skips the borders and header.
 *
 * <p>Total is the column that matters: it is what the whole build needs, whereas Missing
 * depends on what the exporting player happened to be carrying.
 */
public final class LitematicaImport {
    private LitematicaImport() {
    }

    /** True for comma-separated text with a header row or quoted fields, not a typed list. */
    public static boolean looksLikeCsv(String text) {
        String first = firstLine(text);
        if (first.isEmpty() || first.indexOf(',') < 0 || first.indexOf('|') >= 0) {
            return false;
        }
        String lower = first.toLowerCase(Locale.ROOT);
        return first.indexOf('"') >= 0 || (lower.contains("item") && lower.contains("total"));
    }

    public static String toRows(String csv) {
        List<String> lines = csv.lines().filter(l -> !l.isBlank()).toList();
        if (lines.isEmpty()) {
            return "";
        }
        List<String> header = fields(lines.getFirst());
        int item = indexOf(header, "item");
        int total = indexOf(header, "total");
        boolean hasHeader = item >= 0 || total >= 0;
        if (item < 0) {
            item = 0;
        }
        if (total < 0) {
            total = item == 0 ? 1 : 0;
        }

        StringBuilder out = new StringBuilder();
        for (int i = hasHeader ? 1 : 0; i < lines.size(); i++) {
            List<String> row = fields(lines.get(i));
            if (row.size() <= Math.max(item, total)) {
                continue;
            }
            out.append(row.get(item)).append('\t').append(row.get(total)).append('\n');
        }
        return out.toString();
    }

    private static int indexOf(List<String> header, String name) {
        for (int i = 0; i < header.size(); i++) {
            if (header.get(i).strip().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    /** One CSV row; quotes may wrap a field and a doubled quote inside them is a literal quote. */
    static List<String> fields(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                out.add(field.toString().strip());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        out.add(field.toString().strip());
        return out;
    }

    private static String firstLine(String text) {
        return text.lines().filter(l -> !l.isBlank()).findFirst().orElse("").strip();
    }
}
