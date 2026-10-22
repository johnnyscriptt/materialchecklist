package io.github.johnnyscriptt.materialchecklist.server;

import io.github.johnnyscriptt.materialchecklist.core.search.SearchRules;
import io.github.johnnyscriptt.materialchecklist.core.track.TrackedSpots;
import io.github.johnnyscriptt.materialchecklist.core.track.TrackedSpots.Contents;
import io.github.johnnyscriptt.materialchecklist.core.track.TrackedSpots.Spot;
import io.github.johnnyscriptt.materialchecklist.net.CountsPayload;
import io.github.johnnyscriptt.materialchecklist.net.CountsRequestPayload;
import io.github.johnnyscriptt.materialchecklist.net.FindRequestPayload;
import io.github.johnnyscriptt.materialchecklist.net.FindResultPayload;
import io.github.johnnyscriptt.materialchecklist.net.TrackTogglePayload;
import io.github.johnnyscriptt.materialchecklist.net.TrackedListPayload;
import io.github.johnnyscriptt.materialchecklist.net.TrackedQueryPayload;
import io.github.johnnyscriptt.materialchecklist.world.DoubleChests;
import io.github.johnnyscriptt.materialchecklist.world.StackCounter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Serves the three things only the server can answer, because vanilla never sends a client
 * the contents of a container it hasn't opened: marking containers, counting what's in the
 * marked ones, and scanning nearby containers for missing items.
 *
 * <p>Every request is rate-limited per player and everything the client sent is re-checked.
 * Runs on the server thread, so the plain maps need no locking.
 */
public final class ChecklistServer {
    /** Generous reach: the player is standing at, or just opened, the container. */
    private static final double TRACK_REACH = 8.0;
    private static final int TRACK_COOLDOWN_TICKS = 4;
    private static final int COUNTS_COOLDOWN_TICKS = 10;

    private static final Map<UUID, Long> LAST_FIND = new HashMap<>();
    private static final Map<UUID, Long> LAST_TRACK = new HashMap<>();
    private static final Map<UUID, Long> LAST_COUNTS = new HashMap<>();

    private ChecklistServer() {
    }

    public static void onToggle(ServerPlayer player, TrackTogglePayload payload) {
        if (!allowed(LAST_TRACK, player, TRACK_COOLDOWN_TICKS)) {
            return;
        }
        ServerLevel level = player.level();
        BlockPos pos = payload.pos();
        int outcome;
        if (player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > TRACK_REACH * TRACK_REACH) {
            outcome = TrackedListPayload.TOO_FAR;
        } else if (!(level.getBlockEntity(pos) instanceof Container)) {
            outcome = TrackedListPayload.NOT_A_CONTAINER;
        } else {
            List<Spot> unit = new ArrayList<>();
            unit.add(spot(level, pos));
            BlockPos partner = DoubleChests.partnerOf(level, pos);
            if (partner != null) {
                unit.add(spot(level, partner));
            }
            TrackedSpots.Result result = TrackedStore.spots().toggle(player.getStringUUID(), payload.projectId(), unit);
            TrackedStore.markDirty();
            outcome = switch (result) {
                case ADDED -> TrackedListPayload.ADDED;
                case REMOVED -> TrackedListPayload.REMOVED;
                case FULL -> TrackedListPayload.FULL;
            };
        }
        sendTracked(player, payload.projectId(), outcome);
    }

    public static void onQuery(ServerPlayer player, TrackedQueryPayload payload) {
        sendTracked(player, payload.projectId(), TrackedListPayload.QUERY);
    }

    public static void onCounts(ServerPlayer player, CountsRequestPayload payload) {
        if (!allowed(LAST_COUNTS, player, COUNTS_COOLDOWN_TICKS)) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        TrackedSpots spots = TrackedStore.spots();
        String owner = player.getStringUUID();
        int before = spots.spots(owner, payload.projectId()).size();
        Map<String, Long> totals = new HashMap<>(spots.totals(owner, payload.projectId(), spot -> read(server, spot)));
        int remaining = spots.spots(owner, payload.projectId()).size();
        if (remaining != before) {
            TrackedStore.markDirty(); // something marked was broken; the store forgot it
        }
        if (payload.enderChest()) {
            StackCounter.count(player.getEnderChestInventory()).forEach((item, n) -> totals.merge(item, n, Long::sum));
        }

        Map<Identifier, Long> reply = new HashMap<>();
        for (Identifier id : payload.items()) {
            Long n = totals.get(id.toString());
            if (n != null && n > 0) {
                reply.put(id, n);
            }
        }
        ServerPlayNetworking.send(player, new CountsPayload(payload.projectId(), reply, remaining));
        if (remaining != before) {
            sendTracked(player, payload.projectId(), TrackedListPayload.QUERY);
        }
    }

    public static void onFind(ServerPlayer player, FindRequestPayload payload) {
        long now = tick(player);
        long last = LAST_FIND.getOrDefault(player.getUUID(), SearchRules.NEVER_REQUESTED);
        if (!SearchRules.cooldownOk(last, now)) {
            return;
        }
        LAST_FIND.put(player.getUUID(), now);

        Set<Item> wanted = new HashSet<>();
        for (Identifier id : payload.items()) {
            // A defaulted registry answers AIR for ids this server doesn't know.
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item != Items.AIR) {
                wanted.add(item);
            }
        }
        Set<Item> found = new HashSet<>();
        List<BlockPos> hits = wanted.isEmpty() ? List.of()
                : ContainerScanner.findContainersWithAny(player.level(), player.getEyePosition(),
                SearchRules.clampRadius(payload.radius()), wanted, found);
        List<Identifier> foundIds = found.stream().map(BuiltInRegistries.ITEM::getKey).toList();
        ServerPlayNetworking.send(player, new FindResultPayload(hits, foundIds));
    }

    /** Drops a departing player's rate-limit entries so the maps don't grow forever. */
    public static void forget(ServerPlayer player) {
        LAST_FIND.remove(player.getUUID());
        LAST_TRACK.remove(player.getUUID());
        LAST_COUNTS.remove(player.getUUID());
    }

    private static void sendTracked(ServerPlayer player, String projectId, int outcome) {
        List<TrackedListPayload.Spot> out = new ArrayList<>();
        for (Spot spot : TrackedStore.spots().orderedSpots(player.getStringUUID(), projectId)) {
            out.add(new TrackedListPayload.Spot(spot.dimension(), BlockPos.of(spot.pos())));
        }
        ServerPlayNetworking.send(player, new TrackedListPayload(projectId, out, outcome));
    }

    /** What is in a marked spot right now, as far as the server can see without loading chunks. */
    private static Contents read(MinecraftServer server, Spot spot) {
        Identifier dimension = Identifier.tryParse(spot.dimension());
        ServerLevel level = dimension == null || server == null ? null
                : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level == null) {
            return Contents.UNLOADED; // a dimension from a datapack that's switched off; keep it
        }
        BlockPos pos = BlockPos.of(spot.pos());
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            return Contents.UNLOADED;
        }
        BlockEntity blockEntity = chunk.getBlockEntities().get(pos);
        if (!(blockEntity instanceof Container container)) {
            return Contents.GONE;
        }
        if (ContainerScanner.isUnopenedLootContainer(blockEntity)) {
            return Contents.live(Map.of());
        }
        return Contents.live(StackCounter.count(container));
    }

    private static Spot spot(Level level, BlockPos pos) {
        return new Spot(level.dimension().identifier().toString(), pos.asLong());
    }

    private static boolean allowed(Map<UUID, Long> last, ServerPlayer player, int cooldown) {
        long now = tick(player);
        Long previous = last.get(player.getUUID());
        // A tick counter that went backwards means the server restarted.
        if (previous != null && now >= previous && now - previous < cooldown) {
            return false;
        }
        last.put(player.getUUID(), now);
        return true;
    }

    private static long tick(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        return server == null ? 0L : server.getTickCount();
    }
}
