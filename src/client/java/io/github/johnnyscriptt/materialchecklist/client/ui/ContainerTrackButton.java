package io.github.johnnyscriptt.materialchecklist.client.ui;

import io.github.johnnyscriptt.materialchecklist.client.ProgressModel;
import io.github.johnnyscriptt.materialchecklist.client.TrackedController;
import io.github.johnnyscriptt.materialchecklist.client.mixin.AbstractContainerScreenAccessor;
import io.github.johnnyscriptt.materialchecklist.client.net.ClientNet;
import io.github.johnnyscriptt.materialchecklist.client.store.ProjectStore;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Puts a "Track" toggle above any container opened from a block (chest, barrel, shulker box,
 * hopper...), so marking storage for a list is one click while it's open. The container is
 * the block the crosshair was on when the screen opened, which is the one right-clicked.
 *
 * <p>Closing any container screen also asks for fresh counts, since that's when contents change.
 */
public final class ContainerTrackButton {
    private static final int WIDTH = 78;
    private static final int HEIGHT = 16;

    private ContainerTrackButton() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)
                    || screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
                return;
            }
            ScreenEvents.remove(screen).register(closed -> ProgressModel.refreshSoon());

            BlockPos pos = lookedAtContainer(client);
            if (pos == null || !ClientNet.serverHasMod() || ProjectStore.active().isEmpty()) {
                return;
            }
            AbstractContainerScreenAccessor panel = (AbstractContainerScreenAccessor) containerScreen;
            int x = panel.materialchecklist$leftPos() + panel.materialchecklist$imageWidth() - WIDTH;
            int y = Math.max(2, panel.materialchecklist$topPos() - HEIGHT - 2);
            Button button = Button.builder(label(client, pos), b -> TrackedController.toggle(client, pos))
                    .bounds(x, y, WIDTH, HEIGHT).build();
            Screens.getWidgets(screen).add(button);
            // The server's reply arrives a moment later; relabel every frame rather than guess.
            ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, delta) ->
                    button.setMessage(label(client, pos)));
        });
    }

    private static Component label(Minecraft client, BlockPos pos) {
        return Component.translatable(TrackedController.isTracked(client, pos)
                ? "materialchecklist.track.button_on" : "materialchecklist.track.button_off");
    }

    private static BlockPos lookedAtContainer(Minecraft client) {
        if (client.level == null || !(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        return client.level.getBlockEntity(pos) instanceof Container ? pos : null;
    }
}
