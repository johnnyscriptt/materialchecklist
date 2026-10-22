package io.github.johnnyscriptt.materialchecklist.core.list;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A named material list. Immutable: every edit returns a new project, which the store then
 * saves. The id is what tracked containers are filed under on the server, so renaming a
 * project never loses its chests.
 */
public record Project(String id, String name, List<Entry> entries) {
    public static final String UNTITLED = "Untitled";

    public Project {
        name = name == null || name.isBlank() ? UNTITLED : name.strip();
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public Project withName(String newName) {
        return new Project(id, newName, entries);
    }

    /** Adds {@code more} to this list: amounts of items already on it are added together. */
    public Project merged(List<Entry> more) {
        Map<String, Entry> byItem = new LinkedHashMap<>();
        for (Entry entry : entries) {
            byItem.put(entry.itemId(), entry);
        }
        for (Entry entry : more) {
            byItem.merge(entry.itemId(), entry,
                    (old, added) -> new Entry(old.itemId(), old.needed() + added.needed(), old.manuallyChecked()));
        }
        return new Project(id, name, new ArrayList<>(byItem.values()));
    }

    /** Replaces the entry for the same item in place, or appends it if the item is new. */
    public Project withEntry(Entry replacement) {
        List<Entry> out = new ArrayList<>(entries);
        for (int i = 0; i < out.size(); i++) {
            if (out.get(i).itemId().equals(replacement.itemId())) {
                out.set(i, replacement);
                return new Project(id, name, out);
            }
        }
        out.add(replacement);
        return new Project(id, name, out);
    }

    public Project without(String itemId) {
        return new Project(id, name, entries.stream().filter(e -> !e.itemId().equals(itemId)).toList());
    }
}
