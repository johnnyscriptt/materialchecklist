package io.github.johnnyscriptt.materialchecklist.core.search;

/**
 * One searchable item in the picker: its registry id ("minecraft:diamond_sword") and the
 * player's localized name for it ("Diamond Sword"). Kept free of Minecraft types so the
 * filtering is unit-testable.
 */
public record ItemEntry(String id, String displayName) {
}
