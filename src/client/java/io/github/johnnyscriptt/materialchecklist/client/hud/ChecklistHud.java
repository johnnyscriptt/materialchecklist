package io.github.johnnyscriptt.materialchecklist.client.hud;

import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;
import io.github.johnnyscriptt.materialchecklist.client.FindController;
import io.github.johnnyscriptt.materialchecklist.client.ItemIndex;
import io.github.johnnyscriptt.materialchecklist.client.ProgressModel;
import io.github.johnnyscriptt.materialchecklist.client.config.ConfigIo;
import io.github.johnnyscriptt.materialchecklist.client.store.ProjectStore;
import io.github.johnnyscriptt.materialchecklist.core.config.ChecklistConfig;
import io.github.johnnyscriptt.materialchecklist.core.count.StackMath;
import io.github.johnnyscriptt.materialchecklist.core.list.Progress;
import io.github.johnnyscriptt.materialchecklist.core.list.Project;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The always-on checklist: list name and overall progress, then one row per item with its
 * icon, name and have/need, missing items first. Sits in a corner of the screen the player
 * picks, under the chat so chat stays readable.
 */
public final class ChecklistHud {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(MaterialChecklistMod.MOD_ID, "checklist");

    private static final int MARGIN = 4;
    private static final int PAD = 4;
    private static final int ROW_H = 17;
    private static final int ICON = 16;
    private static final int MAX_NAME_W = 110;
    private static final int BAR_H = 3;

    private static final int PANEL = 0x90000000;
    private static final int TITLE = 0xFFFFFFFF;
    private static final int NAME = 0xFFE0E0E0;
    private static final int DONE_NAME = 0xFF8FA88F;
    private static final int MISSING = 0xFFFF7A6B;
    private static final int PARTIAL = 0xFFFFD25A;
    private static final int DONE = 0xFF7CF27C;
    private static final int NEARBY = 0xFFFFC400;
    private static final int BAR_BG = 0xFF3A3A3A;
    private static final int FAINT = 0xFFA0A0A0;

    private ChecklistHud() {
    }

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, ChecklistHud::extract);
    }

    private static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        ChecklistConfig cfg = ConfigIo.get();
        // F1 hides the whole HUD layer, this element included, so it needs no check of its own.
        if (!cfg.hudVisible || client.player == null) {
            return;
        }
        Project project = ProjectStore.active().orElse(null);
        Progress progress = ProgressModel.progress();
        if (project == null || progress == null || progress.total() == 0) {
            return;
        }

        Font font = client.font;
        List<Progress.Row> rows = new ArrayList<>();
        for (Progress.Row row : progress.rows()) {
            if (!(cfg.hideCompleted && row.done())) {
                rows.add(row);
            }
        }
        int hidden = Math.max(0, rows.size() - cfg.hudRows);
        List<Progress.Row> shown = rows.subList(0, Math.min(rows.size(), cfg.hudRows));

        String title = trim(font, project.name(), MAX_NAME_W);
        String tally = progress.doneCount() + "/" + progress.total();
        int width = Math.max(font.width(title) + 8 + font.width(tally), 120);
        List<String> names = new ArrayList<>();
        List<String> amounts = new ArrayList<>();
        for (Progress.Row row : shown) {
            String name = trim(font, ItemIndex.name(row.entry().itemId()).getString(), MAX_NAME_W);
            String amount = amount(row, cfg.stackNotation);
            names.add(name);
            amounts.add(amount);
            width = Math.max(width, ICON + 4 + font.width(name) + 8 + font.width(amount) + 10);
        }
        Component footer = progress.complete()
                ? Component.translatable("materialchecklist.hud.complete")
                : hidden > 0 ? Component.translatable("materialchecklist.hud.more", hidden) : null;
        int height = PAD + font.lineHeight + 2 + BAR_H + 3 + shown.size() * ROW_H
                + (footer == null ? 0 : font.lineHeight + 2) + PAD - 2;
        int panelW = width + PAD * 2;

        int x = cfg.hudCorner.right() ? g.guiWidth() - panelW - MARGIN : MARGIN;
        int y = cfg.hudCorner.bottom() ? g.guiHeight() - height - MARGIN - bottomInset(cfg) : MARGIN;
        g.fill(x, y, x + panelW, y + height, PANEL);

        int cx = x + PAD;
        int cy = y + PAD;
        g.text(font, title, cx, cy, TITLE, true);
        g.text(font, tally, cx + width - font.width(tally), cy, progress.complete() ? DONE : FAINT, true);
        cy += font.lineHeight + 2;
        g.fill(cx, cy, cx + width, cy + BAR_H, BAR_BG);
        g.fill(cx, cy, cx + (int) Math.round(width * progress.fraction()), cy + BAR_H, DONE);
        cy += BAR_H + 3;

        for (int i = 0; i < shown.size(); i++) {
            Progress.Row row = shown.get(i);
            String itemId = row.entry().itemId();
            g.item(ItemIndex.stack(itemId), cx, cy);
            int textY = cy + (ICON - font.lineHeight) / 2 + 1;
            g.text(font, names.get(i), cx + ICON + 4, textY, row.done() ? DONE_NAME : NAME, true);
            int amountX = cx + width - 10 - font.width(amounts.get(i));
            g.text(font, amounts.get(i), amountX, textY, colour(row), true);
            if (row.done()) {
                g.text(font, "✔", cx + width - 8, textY, DONE, true);
            } else if (FindController.foundNearby(client, itemId)) {
                g.text(font, "●", cx + width - 8, textY, NEARBY, true);
            }
            cy += ROW_H;
        }
        if (footer != null) {
            g.text(font, footer, cx, cy + 1, progress.complete() ? DONE : FAINT, true);
        }
    }

    /** Bottom corners sit above the hotbar's height so they never cover the hearts. */
    private static int bottomInset(ChecklistConfig cfg) {
        return cfg.hudCorner.right() ? 0 : 40;
    }

    private static String amount(Progress.Row row, boolean stacks) {
        int stack = ItemIndex.stackSize(row.entry().itemId());
        long need = row.entry().needed();
        if (row.done() && row.have() < need) {
            return format(need, stack, stacks); // ticked off by hand
        }
        return format(row.have(), stack, stacks) + " / " + format(need, stack, stacks);
    }

    private static String format(long n, int stack, boolean stacks) {
        return stacks ? StackMath.format(n, stack) : Long.toString(n);
    }

    private static int colour(Progress.Row row) {
        if (row.done()) {
            return DONE;
        }
        return row.have() > 0 ? PARTIAL : MISSING;
    }

    private static String trim(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        String cut = font.plainSubstrByWidth(text, maxWidth - font.width(ellipsis));
        return cut + ellipsis;
    }
}
