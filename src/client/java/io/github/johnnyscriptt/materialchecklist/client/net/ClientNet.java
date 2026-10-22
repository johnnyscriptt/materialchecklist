package io.github.johnnyscriptt.materialchecklist.client.net;

import io.github.johnnyscriptt.materialchecklist.client.FindController;
import io.github.johnnyscriptt.materialchecklist.client.ProgressModel;
import io.github.johnnyscriptt.materialchecklist.client.TrackedController;
import io.github.johnnyscriptt.materialchecklist.core.list.Entry;
import io.github.johnnyscriptt.materialchecklist.core.list.Project;
import io.github.johnnyscriptt.materialchecklist.net.CountsPayload;
import io.github.johnnyscriptt.materialchecklist.net.CountsRequestPayload;
import io.github.johnnyscriptt.materialchecklist.net.FindRequestPayload;
import io.github.johnnyscriptt.materialchecklist.net.FindResultPayload;
import io.github.johnnyscriptt.materialchecklist.net.NetLimits;
import io.github.johnnyscriptt.materialchecklist.net.TrackTogglePayload;
import io.github.johnnyscriptt.materialchecklist.net.TrackedListPayload;
import io.github.johnnyscriptt.materialchecklist.net.TrackedQueryPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Everything the client sends and receives, in one place. */
public final class ClientNet {
    private ClientNet() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(CountsPayload.TYPE,
                (payload, context) -> ProgressModel.onCounts(payload));
        ClientPlayNetworking.registerGlobalReceiver(TrackedListPayload.TYPE,
                (payload, context) -> TrackedController.onList(context.client(), payload));
        ClientPlayNetworking.registerGlobalReceiver(FindResultPayload.TYPE,
                (payload, context) -> FindController.onResults(context.client(), payload));
    }

    /**
     * Vanilla servers (and servers without this mod) can't see the payloads; on those only the
     * HUD and inventory counting work.
     */
    public static boolean serverHasMod() {
        return ClientPlayNetworking.canSend(CountsRequestPayload.TYPE);
    }

    public static void requestCounts(Project project, boolean enderChest) {
        List<Identifier> items = new ArrayList<>();
        for (Entry entry : project.entries()) {
            Identifier id = Identifier.tryParse(entry.itemId());
            if (id != null && items.size() < NetLimits.ITEMS) {
                items.add(id);
            }
        }
        ClientPlayNetworking.send(new CountsRequestPayload(project.id(), items, enderChest));
    }

    public static void queryTracked(Project project) {
        if (serverHasMod()) {
            ClientPlayNetworking.send(new TrackedQueryPayload(project.id()));
        }
    }

    public static void toggleTrack(Project project, BlockPos pos) {
        ClientPlayNetworking.send(new TrackTogglePayload(project.id(), pos));
    }

    public static void find(List<Identifier> items, int radius) {
        List<Identifier> capped = items.size() <= NetLimits.ITEMS ? items : items.subList(0, NetLimits.ITEMS);
        ClientPlayNetworking.send(new FindRequestPayload(List.copyOf(capped), radius));
    }
}
