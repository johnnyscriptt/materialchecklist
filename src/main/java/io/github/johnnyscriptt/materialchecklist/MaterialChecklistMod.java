package io.github.johnnyscriptt.materialchecklist;

import io.github.johnnyscriptt.materialchecklist.core.ocr.OcrSelfTest;
import io.github.johnnyscriptt.materialchecklist.net.CountsPayload;
import io.github.johnnyscriptt.materialchecklist.net.CountsRequestPayload;
import io.github.johnnyscriptt.materialchecklist.net.FindRequestPayload;
import io.github.johnnyscriptt.materialchecklist.net.FindResultPayload;
import io.github.johnnyscriptt.materialchecklist.net.TrackTogglePayload;
import io.github.johnnyscriptt.materialchecklist.net.TrackedListPayload;
import io.github.johnnyscriptt.materialchecklist.net.TrackedQueryPayload;
import io.github.johnnyscriptt.materialchecklist.server.ChecklistServer;
import io.github.johnnyscriptt.materialchecklist.server.TrackedStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Common entrypoint. Registers the tracking, counting and Find payloads and their server-side
 * receivers, so the mod works on dedicated servers and, via the integrated server, in
 * singleplayer.
 */
public final class MaterialChecklistMod implements ModInitializer {
    public static final String MOD_ID = "materialchecklist";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(TrackTogglePayload.TYPE, TrackTogglePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TrackedQueryPayload.TYPE, TrackedQueryPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CountsRequestPayload.TYPE, CountsRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FindRequestPayload.TYPE, FindRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TrackedListPayload.TYPE, TrackedListPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CountsPayload.TYPE, CountsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FindResultPayload.TYPE, FindResultPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TrackTogglePayload.TYPE,
                (payload, context) -> ChecklistServer.onToggle(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(TrackedQueryPayload.TYPE,
                (payload, context) -> ChecklistServer.onQuery(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(CountsRequestPayload.TYPE,
                (payload, context) -> ChecklistServer.onCounts(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(FindRequestPayload.TYPE,
                (payload, context) -> ChecklistServer.onFind(context.player(), payload));

        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> ChecklistServer.forget(handler.getPlayer()));
        ServerLifecycleEvents.SERVER_STARTED.register(TrackedStore::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> TrackedStore.unload());
        ServerTickEvents.END_SERVER_TICK.register(server -> TrackedStore.tick());

        if (OcrSelfTest.requested()) {
            runOcrSelfTest();
        }
    }

    /** Folder the OCR model is unpacked to; shared by the client import and the self-test. */
    public static Path ocrCacheDir() {
        return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("tessdata");
    }

    private static void runOcrSelfTest() {
        try {
            LOGGER.info("OCR self-test read: \"{}\"", OcrSelfTest.run(ocrCacheDir()));
        } catch (Throwable t) {
            LOGGER.error("OCR self-test failed", t);
        }
    }
}
