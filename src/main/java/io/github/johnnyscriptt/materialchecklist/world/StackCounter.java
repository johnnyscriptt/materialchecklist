package io.github.johnnyscriptt.materialchecklist.world;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Counts items the way a player would: a shulker box full of glass in a chest is that much
 * glass in the chest. Both the server (marked containers, ender chest) and the client (the
 * player's own inventory) count through here, so a shulker counts the same wherever it is.
 *
 * <p>Item identity is the item type alone, ignoring names and enchantments, which is how
 * material lists are written.
 */
public final class StackCounter {
    /** A shulker inside a shulker is impossible in vanilla, but modded bags nest; stop somewhere. */
    private static final int MAX_DEPTH = 3;

    private StackCounter() {
    }

    public static Map<String, Long> count(Container container) {
        Map<String, Long> counts = new HashMap<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            add(container.getItem(slot), counts);
        }
        return counts;
    }

    public static void add(ItemStack stack, Map<String, Long> into) {
        add(stack, into, 0);
    }

    private static void add(ItemStack stack, Map<String, Long> into, int depth) {
        if (stack.isEmpty()) {
            return;
        }
        into.merge(id(stack.getItem()), (long) stack.getCount(), Long::sum);
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null && depth < MAX_DEPTH) {
            for (ItemStack inner : (Iterable<ItemStack>) contents.nonEmptyItemCopyStream()::iterator) {
                add(inner, into, depth + 1);
            }
        }
    }

    /**
     * Records in {@code found} every item from {@code wanted} that is in the container,
     * shulker contents included.
     *
     * @return true if anything wanted was there
     */
    public static boolean collectWanted(Container container, Set<Item> wanted, Set<Item> found) {
        boolean any = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            any |= collectWanted(container.getItem(slot), wanted, found, 0);
        }
        return any;
    }

    private static boolean collectWanted(ItemStack stack, Set<Item> wanted, Set<Item> found, int depth) {
        if (stack.isEmpty()) {
            return false;
        }
        boolean any = false;
        if (wanted.contains(stack.getItem())) {
            found.add(stack.getItem());
            any = true;
        }
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null && depth < MAX_DEPTH) {
            for (ItemStack inner : (Iterable<ItemStack>) contents.nonEmptyItemCopyStream()::iterator) {
                any |= collectWanted(inner, wanted, found, depth + 1);
            }
        }
        return any;
    }

    public static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }
}
