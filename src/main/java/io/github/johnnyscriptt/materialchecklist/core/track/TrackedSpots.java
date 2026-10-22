package io.github.johnnyscriptt.materialchecklist.core.track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Which containers each player has marked for each of their lists, and what those containers
 * held the last time the server could look inside.
 *
 * <p>A container in an unloaded chunk can't be read, so its last snapshot stands in for it:
 * a project's shulkers back at base still count while the player is out gathering. A
 * container that is gone (broken, or replaced by another block) is forgotten for everyone.
 *
 * <p>A "unit" is what the player sees as one container: a single block, or both halves of a
 * double chest. Toggling either half toggles the pair.
 */
public final class TrackedSpots {
    public static final int MAX_PER_PROJECT = 256;

    public record Spot(String dimension, long pos) {
    }

    public enum Result { ADDED, REMOVED, FULL }

    /** What the world says is in a spot right now. */
    public record Contents(State state, Map<String, Long> counts) {
        public enum State { LIVE, UNLOADED, GONE }

        public static final Contents UNLOADED = new Contents(State.UNLOADED, Map.of());
        public static final Contents GONE = new Contents(State.GONE, Map.of());

        public static Contents live(Map<String, Long> counts) {
            return new Contents(State.LIVE, Map.copyOf(counts));
        }
    }

    /** Plain-data form for saving. */
    public record Saved(Map<String, Map<String, List<Spot>>> tracked, List<SnapshotEntry> snapshots) {
    }

    public record SnapshotEntry(Spot spot, Map<String, Long> counts) {
    }

    private final Map<String, Map<String, LinkedHashSet<Spot>>> byPlayer = new HashMap<>();
    private final Map<Spot, Map<String, Long>> snapshots = new HashMap<>();

    public Result toggle(String player, String project, List<Spot> unit) {
        LinkedHashSet<Spot> set = byPlayer.computeIfAbsent(player, p -> new HashMap<>())
                .computeIfAbsent(project, p -> new LinkedHashSet<>());
        if (unit.stream().anyMatch(set::contains)) {
            unit.forEach(set::remove);
            unit.forEach(this::dropSnapshotIfUnused);
            return Result.REMOVED;
        }
        if (set.size() + unit.size() > MAX_PER_PROJECT) {
            return Result.FULL;
        }
        set.addAll(unit);
        return Result.ADDED;
    }

    public Set<Spot> spots(String player, String project) {
        Map<String, LinkedHashSet<Spot>> projects = byPlayer.get(player);
        LinkedHashSet<Spot> set = projects == null ? null : projects.get(project);
        return set == null ? Set.of() : Set.copyOf(set);
    }

    public List<Spot> orderedSpots(String player, String project) {
        Map<String, LinkedHashSet<Spot>> projects = byPlayer.get(player);
        LinkedHashSet<Spot> set = projects == null ? null : projects.get(project);
        return set == null ? List.of() : List.copyOf(set);
    }

    public Map<String, Long> snapshot(Spot spot) {
        return snapshots.getOrDefault(spot, Map.of());
    }

    /**
     * Adds up everything in a project's containers. {@code world} reports each spot; live
     * readings replace the snapshot, unloaded spots fall back to it, and gone spots are dropped.
     */
    public Map<String, Long> totals(String player, String project, Function<Spot, Contents> world) {
        Map<String, Long> totals = new HashMap<>();
        for (Spot spot : orderedSpots(player, project)) {
            Contents contents = world.apply(spot);
            Map<String, Long> counts;
            switch (contents == null ? Contents.State.UNLOADED : contents.state()) {
                case LIVE -> {
                    counts = contents.counts();
                    snapshots.put(spot, counts);
                }
                case GONE -> {
                    forget(spot);
                    continue;
                }
                default -> counts = snapshot(spot);
            }
            counts.forEach((item, n) -> totals.merge(item, n, Long::sum));
        }
        return totals;
    }

    /** Removes a spot from every player's every project. */
    public void forget(Spot spot) {
        for (Map<String, LinkedHashSet<Spot>> projects : byPlayer.values()) {
            for (LinkedHashSet<Spot> set : projects.values()) {
                set.remove(spot);
            }
        }
        snapshots.remove(spot);
    }

    private void dropSnapshotIfUnused(Spot spot) {
        for (Map<String, LinkedHashSet<Spot>> projects : byPlayer.values()) {
            for (LinkedHashSet<Spot> set : projects.values()) {
                if (set.contains(spot)) {
                    return;
                }
            }
        }
        snapshots.remove(spot);
    }

    public Saved toSaved() {
        Map<String, Map<String, List<Spot>>> tracked = new HashMap<>();
        byPlayer.forEach((player, projects) -> {
            Map<String, List<Spot>> out = new HashMap<>();
            projects.forEach((project, set) -> {
                if (!set.isEmpty()) {
                    out.put(project, new ArrayList<>(set));
                }
            });
            if (!out.isEmpty()) {
                tracked.put(player, out);
            }
        });
        List<SnapshotEntry> saved = new ArrayList<>();
        snapshots.forEach((spot, counts) -> saved.add(new SnapshotEntry(spot, counts)));
        return new Saved(tracked, saved);
    }

    public static TrackedSpots fromSaved(Saved saved) {
        TrackedSpots spots = new TrackedSpots();
        if (saved == null) {
            return spots;
        }
        if (saved.tracked() != null) {
            saved.tracked().forEach((player, projects) -> projects.forEach((project, list) ->
                    spots.byPlayer.computeIfAbsent(player, p -> new HashMap<>())
                            .put(project, new LinkedHashSet<>(list))));
        }
        if (saved.snapshots() != null) {
            for (SnapshotEntry entry : saved.snapshots()) {
                if (entry.spot() != null && entry.counts() != null) {
                    spots.snapshots.put(entry.spot(), Map.copyOf(entry.counts()));
                }
            }
        }
        return spots;
    }
}
