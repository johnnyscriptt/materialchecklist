package io.github.johnnyscriptt.materialchecklist.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Supplier;

/**
 * Renders the mod's hero item with Minecraft's own item renderer, large, on flat magenta, so
 * {@code brand_icon.py render ... --key auto} can cut it out for the Modrinth and in-jar icon.
 * The art is the game's, not AI's (Modrinth content rule 6.2a).
 *
 * <p>Each variant is one screenshot: a main item, plus an optional small corner item drawn
 * over its bottom-right (like a chest in the corner of a clipboard). Render a few variants
 * and let the user pick.
 *
 * <p>Output: {@code build/run/clientGameTest/screenshots/icon-render-<name>.png}.
 */
public final class IconRenderGameTest implements FabricClientGameTest {
    /** Stacks are built lazily: item components only bind once a world is loaded. */
    private record Variant(String name, Supplier<ItemStack> main, Supplier<ItemStack> corner) {
    }

    /** A checklist (Book and Quill) with the chest it fills, as on the CurseForge icon; or the chest alone. */
    private static final List<Variant> VARIANTS = List.of(
            new Variant("book-and-chest", () -> new ItemStack(Items.WRITABLE_BOOK), () -> new ItemStack(Items.CHEST)),
            new Variant("chest", () -> new ItemStack(Items.CHEST), () -> ItemStack.EMPTY));

    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(1024, 1024);
        // Item components are only bound once a world is loaded ("Components not bound yet"
        // at the title screen), so the render happens inside a throwaway singleplayer world.
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            for (Variant variant : VARIANTS) {
                context.setScreen(() -> new IconScreen(variant.main().get(), variant.corner().get()));
                context.waitTicks(10);
                context.takeScreenshot(TestScreenshotOptions.of("icon-render-" + variant.name())
                        .disableCounterPrefix());
            }
            context.setScreen(() -> null);
        }
    }

    private static final class IconScreen extends Screen {
        private final ItemStack main;
        private final ItemStack corner;

        IconScreen(ItemStack main, ItemStack corner) {
            super(Component.empty());
            this.main = main;
            this.corner = corner;
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            // No blur or panorama: the background must be one flat colour to key out.
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            g.fill(0, 0, width, height, 0xFFFF00FF);
            float side = Math.min(width, height);
            if (corner.isEmpty()) {
                draw(g, main, width / 2f, height / 2f, side * 0.8f);
            } else {
                draw(g, main, width / 2f - side * 0.06f, height / 2f - side * 0.06f, side * 0.72f);
                draw(g, corner, width / 2f + side * 0.24f, height / 2f + side * 0.24f, side * 0.34f);
            }
        }

        private static void draw(GuiGraphicsExtractor g, ItemStack stack, float centreX, float centreY, float size) {
            float scale = size / 16f;
            g.pose().pushMatrix();
            g.pose().translate(centreX - size / 2f, centreY - size / 2f);
            g.pose().scale(scale, scale);
            g.item(stack, 0, 0);
            g.pose().popMatrix();
        }
    }
}
