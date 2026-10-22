package io.github.johnnyscriptt.materialchecklist.client;

import io.github.johnnyscriptt.materialchecklist.client.net.ClientNet;
import io.github.johnnyscriptt.materialchecklist.client.store.ProjectStore;
import io.github.johnnyscriptt.materialchecklist.core.list.Project;
import io.github.johnnyscriptt.materialchecklist.core.search.HighlightState;
import io.github.johnnyscriptt.materialchecklist.net.TrackedListPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * The containers marked for the active list, as the server last reported them, and the
 * K-key / Track-button action that marks or unmarks one.
 */
public final class TrackedController {
    /** Marked containers stay tinted for as long as they are marked. */
    private static final long FOREVER = Long.MAX_VALUE;

    private static String projectId;
    private static List<TrackedListPayload.Spot> spots = List.of();
    private static HighlightState state = HighlightState.EMPTY;
    private static String stateDimension;

    private TrackedController() {
    }

    public static void query(Project project) {
        projectId = project.id();
        spots = List.of();
        state = HighlightState.EMPTY;
        stateDimension = null;
        ClientNet.queryTracked(project);
    }

    /** Marks or unmarks the container the crosshair is on. */
    public static void toggleLookedAt(Minecraft client) {
        HitResult hit = client.hitResult;
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                && client.level != null && client.level.getBlockEntity(block.getBlockPos()) instanceof Container) {
            toggle(client, block.getBlockPos());
        } else {
            Hud.say(client, Component.translatable("materialchecklist.track.look_at_container"));
        }
    }

    public static void toggle(Minecraft client, BlockPos pos) {
        if (!ClientNet.serverHasMod()) {
            Hud.say(client, Component.translatable("materialchecklist.status.no_server_mod"));
            return;
        }
        Project project = ProjectStore.active().orElse(null);
        if (project == null) {
            Hud.say(client, Component.translatable("materialchecklist.status.no_list"));
            return;
        }
        ClientNet.toggleTrack(project, pos);
    }

    public static void onList(Minecraft client, TrackedListPayload payload) {
        Project project = ProjectStore.active().orElse(null);
        if (project == null || !project.id().equals(payload.projectId())) {
            return;
        }
        projectId = payload.projectId();
        spots = List.copyOf(payload.spots());
        stateDimension = null;
        ProgressModel.refreshSoon();

        String key = switch (payload.outcome()) {
            case TrackedListPayload.ADDED -> "materialchecklist.track.added";
            case TrackedListPayload.REMOVED -> "materialchecklist.track.removed";
            case TrackedListPayload.FULL -> "materialchecklist.track.full";
            case TrackedListPayload.NOT_A_CONTAINER -> "materialchecklist.track.look_at_container";
            case TrackedListPayload.TOO_FAR -> "materialchecklist.track.too_far";
            default -> null;
        };
        if (key != null) {
            Hud.say(client, Component.translatable(key, project.name()));
        }
    }

    /** The marked containers in the player's current dimension. */
    public static HighlightState state(Minecraft client) {
        String dimension = Boxes.dimensionKey(client);
        if (!dimension.equals(stateDimension)) {
            List<BlockPos> here = new ArrayList<>();
            for (TrackedListPayload.Spot spot : spots) {
                if (spot.dimension().equals(dimension)) {
                    here.add(spot.pos());
                }
            }
            state = Boxes.of(client, here, dimension, 0L, FOREVER);
            stateDimension = dimension;
        }
        return state;
    }

    public static boolean isTracked(Minecraft client, BlockPos pos) {
        String dimension = Boxes.dimensionKey(client);
        for (TrackedListPayload.Spot spot : spots) {
            if (spot.pos().equals(pos) && spot.dimension().equals(dimension)) {
                return true;
            }
        }
        return false;
    }

    public static int count() {
        return spots.size();
    }

    public static void clear() {
        projectId = null;
        spots = List.of();
        state = HighlightState.EMPTY;
        stateDimension = null;
    }
}
