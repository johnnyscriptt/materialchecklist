package io.github.johnnyscriptt.materialchecklist.client.ui;

import io.github.johnnyscriptt.materialchecklist.client.ItemIndex;
import io.github.johnnyscriptt.materialchecklist.core.search.ItemFilter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Creative-menu style item picker: type to filter, click an item, and it is handed back to the
 * screen that opened this one.
 */
public final class ItemPickerScreen extends Screen {
    private static final int SLOT = 18;
    private static final int COLS = 9;
    private static final int GRID_W = COLS * SLOT;
    private static final int ROW_H = 20;
    private static final int TITLE_Y = 10;
    private static final int SLOT_BG = 0x30000000;
    private static final int SLOT_HOVER = 0x80FFFFFF;
    private static final int TEXT = 0xFFFFFFFF;

    private final Screen parent;
    private final Consumer<String> onPick;

    private EditBox searchBox;
    private List<ItemIndex.Indexed> filtered = List.of();
    private String appliedQuery;
    private String query;
    private int scrollRow;
    private int gridX;
    private int gridY;
    private int visibleRows;

    /** @param initialQuery prefills the filter, e.g. with the text OCR couldn't match */
    public ItemPickerScreen(Screen parent, String initialQuery, Consumer<String> onPick) {
        super(Component.translatable("materialchecklist.picker.title"));
        this.parent = parent;
        this.onPick = onPick;
        this.query = initialQuery == null ? "" : initialQuery;
    }

    @Override
    protected void init() {
        int x = (this.width - GRID_W) / 2;
        int y = TITLE_Y + 14;

        searchBox = new EditBox(this.font, x, y, GRID_W, ROW_H,
                Component.translatable("materialchecklist.picker.search_hint"));
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.translatable("materialchecklist.picker.search_hint"));
        searchBox.setValue(query);
        searchBox.setResponder(value -> query = value);
        addRenderableWidget(searchBox);
        setInitialFocus(searchBox);
        y += ROW_H + 6;

        gridX = x;
        gridY = y;
        visibleRows = Math.max(1, (this.height - gridY - 32) / SLOT);

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x, this.height - 26, GRID_W, 20).build());
        appliedQuery = null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        applyQuery();
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        extractor.centeredText(this.font, getTitle(), this.width / 2, TITLE_Y, TEXT);

        int hovered = slotIndexAt(mouseX, mouseY);
        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = (scrollRow + row) * COLS + col;
                if (index >= filtered.size()) {
                    break;
                }
                int slotX = gridX + col * SLOT;
                int slotY = gridY + row * SLOT;
                extractor.fill(slotX, slotY, slotX + SLOT, slotY + SLOT, index == hovered ? SLOT_HOVER : SLOT_BG);
                extractor.item(filtered.get(index).stack(), slotX + 1, slotY + 1);
            }
        }
        if (filtered.isEmpty()) {
            extractor.centeredText(this.font, Component.translatable("materialchecklist.picker.no_matches",
                    searchBox.getValue()), this.width / 2, gridY + 4, TEXT);
        }
        if (hovered >= 0) {
            extractor.setTooltipForNextFrame(this.font, filtered.get(hovered).stack(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int index = slotIndexAt((int) event.x(), (int) event.y());
        if (index >= 0) {
            String id = filtered.get(index).id().toString();
            // Hand the pick over first, so the parent lays itself out around the new state.
            onPick.accept(id);
            this.minecraft.gui.setScreen(parent);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int totalRows = (filtered.size() + COLS - 1) / COLS;
        int maxScroll = Math.max(0, totalRows - visibleRows);
        scrollRow = Math.clamp(scrollRow - (int) Math.signum(deltaY), 0, maxScroll);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    private void applyQuery() {
        String normalized = ItemFilter.normalize(searchBox.getValue());
        if (normalized.equals(appliedQuery)) {
            return;
        }
        appliedQuery = normalized;
        List<ItemIndex.Indexed> all = ItemIndex.all();
        filtered = normalized.isEmpty() ? all
                : all.stream().filter(item -> ItemFilter.matches(item.entry(), normalized)).toList();
        scrollRow = 0;
    }

    private int slotIndexAt(int mouseX, int mouseY) {
        if (mouseX < gridX || mouseX >= gridX + GRID_W || mouseY < gridY || mouseY >= gridY + visibleRows * SLOT) {
            return -1;
        }
        int index = (scrollRow + (mouseY - gridY) / SLOT) * COLS + (mouseX - gridX) / SLOT;
        return index < filtered.size() ? index : -1;
    }
}
