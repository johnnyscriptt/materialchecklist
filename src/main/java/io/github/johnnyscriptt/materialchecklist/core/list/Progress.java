package io.github.johnnyscriptt.materialchecklist.core.list;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;

/**
 * How far along a list is, given how many of each item the player has. Rows still missing
 * come first so the HUD shows what is left to gather; within each group the list's own order
 * is kept, which is the order the builder wrote it in.
 */
public final class Progress {
    private final List<Row> rows;
    private final int doneCount;
    private final double fraction;

    private Progress(List<Row> rows, int doneCount, double fraction) {
        this.rows = rows;
        this.doneCount = doneCount;
        this.fraction = fraction;
    }

    public record Row(Entry entry, long have, boolean done) {
        public long remaining() {
            return done ? 0 : Math.max(0, entry.needed() - have);
        }
    }

    public static Progress of(List<Entry> entries, ToLongFunction<String> have) {
        List<Row> missing = new ArrayList<>();
        List<Row> done = new ArrayList<>();
        long needed = 0;
        long gathered = 0;
        for (Entry entry : entries) {
            long count = Math.max(0, have.applyAsLong(entry.itemId()));
            boolean isDone = entry.manuallyChecked() || count >= entry.needed();
            (isDone ? done : missing).add(new Row(entry, count, isDone));
            needed += entry.needed();
            // A ticked entry counts as fully gathered; surplus of one item can't make up for another.
            gathered += isDone ? entry.needed() : Math.min(count, entry.needed());
        }
        List<Row> rows = new ArrayList<>(missing);
        rows.addAll(done);
        return new Progress(List.copyOf(rows), done.size(), needed == 0 ? 0.0 : (double) gathered / needed);
    }

    public List<Row> rows() {
        return rows;
    }

    public int doneCount() {
        return doneCount;
    }

    public int total() {
        return rows.size();
    }

    /** Share of all items gathered, so 1 of 2 rows done can still read as nearly there. */
    public double fraction() {
        return fraction;
    }

    public boolean complete() {
        return !rows.isEmpty() && doneCount == rows.size();
    }
}
