package io.github.johnnyscriptt.materialchecklist.client;

import io.github.johnnyscriptt.materialchecklist.core.parse.Candidate;
import io.github.johnnyscriptt.materialchecklist.core.parse.ItemNameMatcher;
import io.github.johnnyscriptt.materialchecklist.core.search.ItemEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every item the game knows, with the name the player sees, built once and reused by the
 * picker, the name matcher and every screen that draws an item from its id. Dropped when
 * leaving a world, because the next server may register a different set of items.
 */
public final class ItemIndex {

    public record Indexed(ItemEntry entry, ItemStack stack, Identifier id) {
    }

    private static List<Indexed> items;
    private static Map<String, Indexed> byId;
    private static ItemNameMatcher matcher;

    private ItemIndex() {
    }

    public static synchronized List<Indexed> all() {
        ensure();
        return items;
    }

    public static synchronized ItemNameMatcher matcher() {
        ensure();
        if (matcher == null) {
            List<Candidate> candidates = new ArrayList<>(items.size());
            for (Indexed item : items) {
                candidates.add(new Candidate(item.entry().id(), item.entry().displayName(),
                        item.stack().getMaxStackSize()));
            }
            matcher = new ItemNameMatcher(candidates);
        }
        return matcher;
    }

    /** The item for a registry id, or air when this game doesn't have it (a modded list). */
    public static synchronized ItemStack stack(String id) {
        ensure();
        Indexed item = byId.get(id);
        return item == null ? ItemStack.EMPTY : item.stack();
    }

    public static Component name(String id) {
        ItemStack stack = stack(id);
        return stack.isEmpty() ? Component.literal(id) : stack.getHoverName();
    }

    public static int stackSize(String id) {
        ItemStack stack = stack(id);
        return stack.isEmpty() ? 64 : stack.getMaxStackSize();
    }

    public static synchronized void invalidate() {
        items = null;
        byId = null;
        matcher = null;
    }

    private static void ensure() {
        if (items != null) {
            return;
        }
        List<Indexed> entries = new ArrayList<>();
        Map<String, Indexed> map = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            Indexed indexed = new Indexed(new ItemEntry(id.toString(), stack.getHoverName().getString()), stack, id);
            entries.add(indexed);
            map.put(id.toString(), indexed);
        }
        items = List.copyOf(entries);
        byId = map;
    }
}
