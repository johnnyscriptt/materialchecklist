package io.github.johnnyscriptt.materialchecklist.client.ui;

import io.github.johnnyscriptt.materialchecklist.client.config.ConfigIo;
import io.github.johnnyscriptt.materialchecklist.core.config.ChecklistConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A column of buttons, one per setting; clicking one moves it to its next value. Saved when
 * the screen closes.
 */
public final class SettingsScreen extends Screen {
    private static final int BTN_W = 220;
    private static final int BTN_H = 20;
    private static final int GAP = 4;
    private static final int[] ROW_CHOICES = {5, 8, 12, 16, 20};
    private static final int[] RADIUS_CHOICES = {8, 16, 24, 32, 48, 64};
    private static final int TEXT = 0xFFFFFFFF;

    private final Screen parent;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("materialchecklist.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ChecklistConfig cfg = ConfigIo.get();
        int x = (this.width - BTN_W) / 2;
        int y = 32;

        y = toggle(x, y, "hud", c -> c.hudVisible, c -> c.hudVisible = !c.hudVisible);
        y = setting(x, y, c -> Component.translatable("materialchecklist.settings.corner",
                Component.translatable("materialchecklist.corner." + c.hudCorner.name().toLowerCase(Locale.ROOT))),
                c -> c.hudCorner = c.hudCorner.next());
        y = setting(x, y, c -> Component.translatable("materialchecklist.settings.rows", c.hudRows),
                c -> c.hudRows = next(ROW_CHOICES, c.hudRows));
        y = toggle(x, y, "hide_completed", c -> c.hideCompleted, c -> c.hideCompleted = !c.hideCompleted);
        y = toggle(x, y, "stacks", c -> c.stackNotation, c -> c.stackNotation = !c.stackNotation);
        y = toggle(x, y, "ender_chest", c -> c.countEnderChest, c -> c.countEnderChest = !c.countEnderChest);
        y = toggle(x, y, "glow", c -> c.glowMissingSlots, c -> c.glowMissingSlots = !c.glowMissingSlots);
        y = setting(x, y, c -> Component.translatable("materialchecklist.settings.radius", c.radius),
                c -> c.radius = next(RADIUS_CHOICES, c.radius));

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x, Math.max(y + 8, this.height - 28), BTN_W, BTN_H).build());
    }

    private int toggle(int x, int y, String key, Function<ChecklistConfig, Boolean> get,
                       Consumer<ChecklistConfig> flip) {
        return setting(x, y, c -> Component.translatable("materialchecklist.settings." + key,
                get.apply(c) ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF), flip);
    }

    private int setting(int x, int y, Function<ChecklistConfig, Component> label,
                        Consumer<ChecklistConfig> change) {
        ChecklistConfig cfg = ConfigIo.get();
        addRenderableWidget(Button.builder(label.apply(cfg), b -> {
            change.accept(cfg);
            b.setMessage(label.apply(cfg));
        }).bounds(x, y, BTN_W, BTN_H).build());
        return y + BTN_H + GAP;
    }

    private static int next(int[] choices, int current) {
        for (int choice : choices) {
            if (choice > current) {
                return choice;
            }
        }
        return choices[0];
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(this.font, getTitle(), this.width / 2, 12, TEXT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        ConfigIo.save();
        this.minecraft.gui.setScreen(parent);
    }
}
