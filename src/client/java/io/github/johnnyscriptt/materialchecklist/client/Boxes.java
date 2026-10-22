package io.github.johnnyscriptt.materialchecklist.client;

import io.github.johnnyscriptt.materialchecklist.core.math.BlockPosL;
import io.github.johnnyscriptt.materialchecklist.core.search.HighlightState;
import io.github.johnnyscriptt.materialchecklist.world.DoubleChests;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Turns container positions into the boxes the renderer draws. */
public final class Boxes {
    private Boxes() {
    }

    /**
     * Folds each double chest's two halves into a single rectangle so it reads as the one
     * container it is, which also makes a "3 containers" message match what the player sees.
     */
    public static HighlightState of(Minecraft client, List<BlockPos> positions, String dimensionKey,
                                    long nowMs, long lifetimeMs) {
        Set<BlockPos> unclaimed = new LinkedHashSet<>(positions);
        List<Long> mins = new ArrayList<>();
        List<Long> maxs = new ArrayList<>();

        for (BlockPos pos : positions) {
            if (!unclaimed.remove(pos)) {
                continue; // already swallowed by its other half
            }
            BlockPos partner = client.level == null ? null : DoubleChests.partnerOf(client.level, pos);
            if (partner != null && unclaimed.remove(partner)) {
                mins.add(BlockPosL.pack(Math.min(pos.getX(), partner.getX()),
                        Math.min(pos.getY(), partner.getY()), Math.min(pos.getZ(), partner.getZ())));
                maxs.add(BlockPosL.pack(Math.max(pos.getX(), partner.getX()),
                        Math.max(pos.getY(), partner.getY()), Math.max(pos.getZ(), partner.getZ())));
            } else {
                long packed = BlockPosL.pack(pos.getX(), pos.getY(), pos.getZ());
                mins.add(packed);
                maxs.add(packed);
            }
        }
        return new HighlightState(toArray(mins), toArray(maxs), dimensionKey, nowMs, lifetimeMs);
    }

    /** The dimension the local player is in, in the same form the states store. */
    public static String dimensionKey(Minecraft client) {
        return client.level == null ? "" : client.level.dimension().identifier().toString();
    }

    private static long[] toArray(List<Long> values) {
        long[] out = new long[values.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = values.get(i);
        }
        return out;
    }
}
