package io.github.johnnyscriptt.materialchecklist.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;
import io.github.johnnyscriptt.materialchecklist.client.config.ConfigIo;
import io.github.johnnyscriptt.materialchecklist.client.hud.ChecklistHud;
import io.github.johnnyscriptt.materialchecklist.client.net.ClientNet;
import io.github.johnnyscriptt.materialchecklist.client.render.HighlightRenderer;
import io.github.johnnyscriptt.materialchecklist.client.ui.ChecklistScreen;
import io.github.johnnyscriptt.materialchecklist.client.ui.ContainerTrackButton;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Client entrypoint. Owns the hotkeys, the checklist screens, the HUD and the renderers.
 */
public final class MaterialChecklistClient implements ClientModInitializer {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MaterialChecklistMod.MOD_ID, "main"));

    public static final KeyMapping OPEN_KEY =
            new KeyMapping("key.materialchecklist.open", InputConstants.KEY_B, CATEGORY);
    public static final KeyMapping HUD_KEY =
            new KeyMapping("key.materialchecklist.hud", InputConstants.KEY_N, CATEGORY);
    public static final KeyMapping TRACK_KEY =
            new KeyMapping("key.materialchecklist.track", InputConstants.KEY_K, CATEGORY);
    public static final KeyMapping FIND_KEY =
            new KeyMapping("key.materialchecklist.find", InputConstants.KEY_J, CATEGORY);

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(OPEN_KEY);
        KeyMappingHelper.registerKeyMapping(HUD_KEY);
        KeyMappingHelper.registerKeyMapping(TRACK_KEY);
        KeyMappingHelper.registerKeyMapping(FIND_KEY);

        ClientNet.register();
        HighlightRenderer.register();
        ChecklistHud.register();
        ContainerTrackButton.register();

        // Leaving a world invalidates everything the server told us: it belongs to a level that's gone.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ProgressModel.clear();
            TrackedController.clear();
            FindController.clear();
            ItemIndex.invalidate();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ProgressModel.tick(client);
            handleKeys(client);
        });
    }

    private static void handleKeys(Minecraft client) {
        boolean inWorld = client.player != null && client.level != null && client.gui.screen() == null;
        while (OPEN_KEY.consumeClick()) {
            if (inWorld) {
                client.setScreenAndShow(new ChecklistScreen(null));
            }
        }
        while (HUD_KEY.consumeClick()) {
            if (inWorld) {
                ConfigIo.get().hudVisible = !ConfigIo.get().hudVisible;
                ConfigIo.save();
                Hud.say(client, Component.translatable(ConfigIo.get().hudVisible
                        ? "materialchecklist.status.hud_on" : "materialchecklist.status.hud_off"));
            }
        }
        while (TRACK_KEY.consumeClick()) {
            if (inWorld) {
                TrackedController.toggleLookedAt(client);
            }
        }
        while (FIND_KEY.consumeClick()) {
            if (inWorld) {
                FindController.find(client);
            }
        }
    }
}
