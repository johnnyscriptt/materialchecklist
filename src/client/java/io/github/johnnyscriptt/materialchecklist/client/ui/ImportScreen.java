package io.github.johnnyscriptt.materialchecklist.client.ui;

import com.mojang.blaze3d.Blaze3D;
import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;
import io.github.johnnyscriptt.materialchecklist.client.ItemIndex;
import io.github.johnnyscriptt.materialchecklist.client.ocr.GameFont;
import io.github.johnnyscriptt.materialchecklist.client.ocr.OcrService;
import io.github.johnnyscriptt.materialchecklist.client.ocr.ScreenshotFinder;
import io.github.johnnyscriptt.materialchecklist.core.parse.ListParser;
import io.github.johnnyscriptt.materialchecklist.core.parse.LitematicaImport;
import io.github.johnnyscriptt.materialchecklist.core.parse.ParsedLine;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Where a list comes in: typed or pasted into the box, dropped onto the window as a
 * screenshot, .txt or .csv, or read from the newest screenshot on disk. Every route ends in the
 * {@link ReviewScreen}, so nothing reaches a list unchecked.
 */
public final class ImportScreen extends Screen {
    private static final int M = 8;
    private static final int BTN_H = 20;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int FAINT = 0xFFA0A0A0;
    private static final int WARNING = 0xFFFF7A6B;

    /** Text files beyond this are not material lists. */
    private static final long MAX_TEXT_BYTES = 1_000_000;

    private final Screen parent;
    private MultiLineEditBox textBox;
    private String text = "";
    private Component status = Component.empty();
    private boolean statusIsError;
    private boolean busy;

    public ImportScreen(Screen parent) {
        this(parent, "");
    }

    public ImportScreen(Screen parent, String initialText) {
        super(Component.translatable("materialchecklist.import.title"));
        this.parent = parent;
        this.text = initialText;
    }

    @Override
    protected void init() {
        int w = Math.min(420, this.width - 2 * M);
        int x = (this.width - w) / 2;
        int boxTop = 40;
        int boxH = Math.max(60, this.height - boxTop - 3 * (BTN_H + 4) - 22);

        textBox = MultiLineEditBox.builder()
                .setX(x).setY(boxTop)
                .setPlaceholder(Component.translatable("materialchecklist.import.placeholder"))
                .build(this.font, w, boxH, Component.translatable("materialchecklist.import.title"));
        textBox.setCharacterLimit(200_000);
        textBox.setValue(text);
        textBox.setValueListener(value -> text = value);
        addRenderableWidget(textBox);
        setInitialFocus(textBox);

        int y = boxTop + boxH + 18;
        int half = (w - 4) / 2;
        addRenderableWidget(Button.builder(Component.translatable("materialchecklist.import.read_text"),
                b -> review(text)).bounds(x, y, w, BTN_H).build()).active = !busy;
        y += BTN_H + 4;
        addRenderableWidget(Button.builder(Component.translatable("materialchecklist.import.latest_screenshot"),
                b -> readLatestScreenshot()).bounds(x, y, half, BTN_H).build()).active = !busy;
        addRenderableWidget(Button.builder(Component.translatable("materialchecklist.import.open_folder"),
                b -> openInbox()).bounds(x + half + 4, y, w - half - 4, BTN_H).build());
        y += BTN_H + 4;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(x, y, w, BTN_H).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(this.font, getTitle(), this.width / 2, 10, TEXT);
        g.centeredText(this.font, Component.translatable("materialchecklist.import.hint"), this.width / 2, 24, FAINT);
        int boxBottom = textBox.getY() + textBox.getHeight();
        g.centeredText(this.font, status, this.width / 2, boxBottom + 5, statusIsError ? WARNING : FAINT);
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (!paths.isEmpty()) {
            readFile(paths.getFirst());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    /** Back here from the review screen, with the text the player can still edit. */
    Screen parentScreen() {
        return parent;
    }

    private void readLatestScreenshot() {
        Optional<Path> newest = ScreenshotFinder.newest();
        if (newest.isEmpty()) {
            setStatus(Component.translatable("materialchecklist.import.no_screenshot"), true);
            return;
        }
        readFile(newest.get());
    }

    private void readFile(Path file) {
        String name = file.getFileName().toString();
        if (ScreenshotFinder.isImage(name)) {
            readImage(file);
            return;
        }
        try {
            if (Files.size(file) > MAX_TEXT_BYTES) {
                setStatus(Component.translatable("materialchecklist.import.too_big", name), true);
                return;
            }
            String content = Files.readString(file, StandardCharsets.UTF_8);
            text = LitematicaImport.looksLikeCsv(content) ? LitematicaImport.toRows(content) : content;
            review(text);
        } catch (IOException e) {
            setStatus(Component.translatable("materialchecklist.import.unreadable", name), true);
        }
    }

    private void readImage(Path file) {
        busy = true;
        setStatus(Component.translatable("materialchecklist.import.reading", file.getFileName().toString()), false);
        rebuildWidgets();
        OcrService.read(file, GameFont.numbers(this.minecraft)).whenComplete((result, error) -> this.minecraft.execute(() -> {
            busy = false;
            if (error != null) {
                MaterialChecklistMod.LOGGER.warn("Screenshot import failed for {}", file, error);
                setStatus(Component.translatable("materialchecklist.import.ocr_failed", OcrService.describe(error)), true);
                rebuildWidgets();
                return;
            }
            text = result;
            if (this.minecraft.gui.screen() == this) {
                rebuildWidgets();
                review(result);
            }
        }));
    }

    private void review(String content) {
        List<ParsedLine> lines = new ListParser(ItemIndex.matcher()).parse(content);
        if (lines.isEmpty()) {
            setStatus(Component.translatable("materialchecklist.import.nothing_found"), true);
            return;
        }
        this.minecraft.gui.setScreen(new ReviewScreen(this, lines));
    }

    private void openInbox() {
        Path inbox = ScreenshotFinder.inbox();
        try {
            Files.createDirectories(inbox);
            Blaze3D.openPath(inbox);
        } catch (IOException e) {
            setStatus(Component.translatable("materialchecklist.import.unreadable", inbox.toString()), true);
        }
    }

    private void setStatus(Component message, boolean error) {
        status = message;
        statusIsError = error;
    }
}
