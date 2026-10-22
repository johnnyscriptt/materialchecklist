package io.github.johnnyscriptt.materialchecklist.client;

import io.github.johnnyscriptt.materialchecklist.client.config.ConfigIo;
import io.github.johnnyscriptt.materialchecklist.client.net.ClientNet;
import io.github.johnnyscriptt.materialchecklist.core.list.Progress;
import io.github.johnnyscriptt.materialchecklist.core.search.HighlightState;
import io.github.johnnyscriptt.materialchecklist.net.FindResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * "Find items": asks the server which nearby containers hold anything the active list is still
 * missing, and holds the answer for the renderer and the HUD's "nearby" markers.
 */
public final class FindController {
    private static HighlightState state = HighlightState.EMPTY;
    private static Set<String> foundItems = Set.of();
    private static int pendingRadius;

    private FindController() {
    }

    public static void find(Minecraft client) {
        if (!ClientNet.serverHasMod()) {
            Hud.say(client, Component.translatable("materialchecklist.status.no_server_mod"));
            return;
        }
        Progress progress = ProgressModel.progress();
        if (progress == null || progress.total() == 0) {
            Hud.say(client, Component.translatable("materialchecklist.status.no_list"));
            return;
        }
        List<Identifier> missing = progress.rows().stream().filter(row -> !row.done())
                .map(row -> Identifier.tryParse(row.entry().itemId())).filter(id -> id != null).toList();
        if (missing.isEmpty()) {
            Hud.say(client, Component.translatable("materialchecklist.find.nothing_missing"));
            return;
        }
        pendingRadius = ConfigIo.get().radius;
        ClientNet.find(missing, pendingRadius);
    }

    public static void onResults(Minecraft client, FindResultPayload payload) {
        if (client.level == null) {
            return;
        }
        Set<String> found = new HashSet<>();
        payload.found().forEach(id -> found.add(id.toString()));
        foundItems = Set.copyOf(found);
        if (payload.positions().isEmpty()) {
            state = HighlightState.EMPTY;
            Hud.say(client, Component.translatable("materialchecklist.find.none", pendingRadius));
            return;
        }
        state = Boxes.of(client, payload.positions(), Boxes.dimensionKey(client), System.currentTimeMillis(),
                ConfigIo.get().highlightLifetimeMs());
        Hud.say(client, Component.translatable("materialchecklist.find.found", state.count(), found.size()));
    }

    public static HighlightState state() {
        return state;
    }

    /** True while the last Find's highlights are up and it turned up this item somewhere. */
    public static boolean foundNearby(Minecraft client, String itemId) {
        return foundItems.contains(itemId) && state.visibleIn(Boxes.dimensionKey(client), System.currentTimeMillis());
    }

    public static void clear() {
        state = HighlightState.EMPTY;
        foundItems = Set.of();
    }
}
