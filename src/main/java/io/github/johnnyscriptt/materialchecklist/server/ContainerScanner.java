package io.github.johnnyscriptt.materialchecklist.server;

import io.github.johnnyscriptt.materialchecklist.core.math.SearchGeometry;
import io.github.johnnyscriptt.materialchecklist.core.search.SearchRules;
import io.github.johnnyscriptt.materialchecklist.world.DoubleChests;
import io.github.johnnyscriptt.materialchecklist.world.StackCounter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the containers near a player that hold any item still missing from their list,
 * looking inside shulker boxes too.
 *
 * <p>Works off the block-entity map each loaded chunk already keeps, so the cost scales with
 * how many containers are nearby rather than with the volume of the search sphere. Chunks
 * that aren't loaded are skipped: a search never forces chunk loading.
 */
public final class ContainerScanner {

    private ContainerScanner() {
    }

    /**
     * @param origin the player's eye position
     * @param found  receives every wanted item that turned up somewhere
     * @return the nearest {@link SearchRules#MAX_RESULTS} containers within {@code radius}
     *         holding at least one wanted item
     */
    public static List<BlockPos> findContainersWithAny(ServerLevel level, Vec3 origin, int radius,
                                                       Set<Item> wanted, Set<Item> found) {
        List<Hit> hits = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();

        int originX = Mth.floor(origin.x);
        int originZ = Mth.floor(origin.z);
        for (int cx = SearchGeometry.chunkMin(originX, radius); cx <= SearchGeometry.chunkMax(originX, radius); cx++) {
            for (int cz = SearchGeometry.chunkMin(originZ, radius); cz <= SearchGeometry.chunkMax(originZ, radius); cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk != null) {
                    collectFromChunk(chunk, origin, radius, wanted, found, hits, seen);
                }
            }
        }

        hits.sort(Comparator.comparingDouble(Hit::distanceSq));
        return SearchRules.truncate(hits.stream().map(Hit::pos).toList());
    }

    private static void collectFromChunk(LevelChunk chunk, Vec3 origin, int radius, Set<Item> wanted,
                                         Set<Item> found, List<Hit> hits, Set<BlockPos> seen) {
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockPos pos = entry.getKey();
            if (!SearchGeometry.withinSphere(pos.getX(), pos.getY(), pos.getZ(),
                    origin.x, origin.y, origin.z, radius)) {
                continue;
            }
            BlockEntity blockEntity = entry.getValue();
            if (!(blockEntity instanceof Container container) || isUnopenedLootContainer(blockEntity)) {
                continue;
            }
            if (!StackCounter.collectWanted(container, wanted, found)) {
                continue;
            }
            addHit(hits, seen, pos.immutable(), origin);
            // A double chest is one container to the player, so both halves light up together.
            BlockPos partner = DoubleChests.partnerOf(chunk, pos);
            if (partner != null) {
                addHit(hits, seen, partner, origin);
            }
        }
    }

    private static void addHit(List<Hit> hits, Set<BlockPos> seen, BlockPos pos, Vec3 origin) {
        if (seen.add(pos)) {
            hits.add(new Hit(pos, pos.distToCenterSqr(origin)));
        }
    }

    /**
     * Structure loot is only rolled when a player first opens the container, and reading a slot
     * is what triggers that roll, so these are left alone entirely.
     */
    public static boolean isUnopenedLootContainer(BlockEntity blockEntity) {
        return blockEntity instanceof RandomizableContainer randomizable
                && randomizable.getLootTable() != null;
    }

    private record Hit(BlockPos pos, double distanceSq) {
    }
}
