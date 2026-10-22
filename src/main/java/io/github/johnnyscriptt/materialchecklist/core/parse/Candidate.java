package io.github.johnnyscriptt.materialchecklist.core.parse;

/**
 * An item a list line can resolve to: its registry id, the name the player sees, and its
 * stack size, which turns "3 stacks" into a number. Kept free of Minecraft types so parsing
 * is unit-testable.
 */
public record Candidate(String id, String displayName, int maxStackSize) {
}
