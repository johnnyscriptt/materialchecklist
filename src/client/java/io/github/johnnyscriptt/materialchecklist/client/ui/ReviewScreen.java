package io.github.johnnyscriptt.materialchecklist.client.ui;

import io.github.johnnyscriptt.materialchecklist.client.ItemIndex;
import io.github.johnnyscriptt.materialchecklist.client.store.ProjectStore;
import io.github.johnnyscriptt.materialchecklist.core.count.StackMath;
import io.github.johnnyscriptt.materialchecklist.core.list.Entry;
import io.github.johnnyscriptt.materialchecklist.core.list.Project;
import io.github.johnnyscriptt.materialchecklist.core.parse.ParsedLine;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Shows what the parser made of an import, one line per row, before any of it goes into a
 * list. Rows it was sure about are marked green; rows the player should look at are yellow
 * (a weak name match, or no amount) or red (no item found). Any row can be pointed at a
 * different item, given a new amount, or dropped.
 */
public final class ReviewScreen extends Screen {
    private static final int M = 8;
    private static final int ROW_H = 18;
    private static final int BTN_H = 20;
    private static final int TOP = 40;

    private static final int TEXT = 0xFFFFFFFF;
    private static final int FAINT = 0xFF909090;
    private static final int PANEL_BG = 0x60000000;
    private static final int SELECTED_BG = 0x50FFFFFF;
    private static final int HOVER_BG = 0x25FFFFFF;
    private static final int OK = 0xFF7CF27C;
    private static final int CHECK = 0xFFFFD25A;
    private static final int BAD = 0xFFFF7A6B;

    /** A line as the player is editing it. */
    private static final class Row {
        final String raw;
        String itemId;
        long count;
        boolean sure;

        Row(ParsedLine line) {
            raw = line.raw().replace('\t', ' '); // the font has no tab glyph
            itemId = line.match() == null ? null : line.match().candidate().id();
            count = line.count();
            sure = !line.needsReview();
        }

        int colour() {
            if (itemId == null) {
                return BAD;
            }
            return sure && count != ParsedLine.NO_COUNT ? OK : CHECK;
        }
    }

    private final ImportScreen importScreen;
    private final List<Row> rows = new ArrayList<>();
    private int selected = -1;
    private int scroll;
    private boolean settingCount;

    private EditBox countBox;
    private EditBox nameBox;
    private Button changeButton;
    private Button removeButton;
    private int x;
    private int w;
    private int rowsBottom;

    public ReviewScreen(ImportScreen importScreen, List<ParsedLine> lines) {
        super(Component.translatable("materialchecklist.review.title"));
        this.importScreen = importScreen;
        for (ParsedLine line : lines) {
            rows.add(new Row(line));
        }
    }

    @Override
    protected void init() {
        w = Math.min(460, this.width - 2 * M);
        x = (this.width - w) / 2;
        int bottom2 = this.height - (BTN_H + 4);
        int bottom1 = bottom2 - (BTN_H + 4);
        rowsBottom = bottom1 - 6;

        changeButton = addRenderableWidget(Button.builder(Component.translatable("materialchecklist.review.change"),
                b -> changeSelected()).bounds(x, bottom1, 100, BTN_H).build());
        countBox = new EditBox(this.font, x + 104, bottom1, 80, BTN_H, Component.translatable("materialchecklist.screen.needed"));
        countBox.setMaxLength(32);
        countBox.setHint(Component.translatable("materialchecklist.screen.needed"));
        countBox.setResponder(this::onCountTyped);
        addRenderableWidget(countBox);
        removeButton = addRenderableWidget(Button.builder(Component.translatable("materialchecklist.screen.remove"),
                b -> removeSelected()).bounds(x + 188, bottom1, 70, BTN_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),
                b -> this.minecraft.gui.setScreen(importScreen)).bounds(x + w - 70, bottom1, 70, BTN_H).build());

        Project active = ProjectStore.active().orElse(null);
        int third = (w - 8) / 3;
        nameBox = new EditBox(this.font, x, bottom2, third, BTN_H, Component.translatable("materialchecklist.screen.name"));
        nameBox.setMaxLength(48);
        nameBox.setHint(Component.translatable("materialchecklist.review.new_name"));
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("materialchecklist.review.create"),
                b -> accept(false)).bounds(x + third + 4, bottom2, third, BTN_H).build());
        Button add = addRenderableWidget(Button.builder(active == null
                        ? Component.translatable("materialchecklist.review.add_none")
                        : Component.translatable("materialchecklist.review.add_to", trim(active.name(), third - 40)),
                b -> accept(true)).bounds(x + 2 * (third + 4), bottom2, w - 2 * (third + 4), BTN_H).build());
        add.active = active != null;
        syncSelection();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(this.font, getTitle(), this.width / 2, 10, TEXT);
        long unsure = rows.stream().filter(r -> r.colour() != OK).count();
        g.centeredText(this.font, Component.translatable("materialchecklist.review.summary", rows.size(), unsure),
                this.width / 2, 24, unsure > 0 ? CHECK : OK);

        g.fill(x, TOP, x + w, rowsBottom, PANEL_BG);
        int visible = visibleRows();
        scroll = Math.clamp(scroll, 0, Math.max(0, rows.size() - visible));
        int col2 = x + w / 2;
        for (int i = 0; i < visible && scroll + i < rows.size(); i++) {
            int index = scroll + i;
            Row row = rows.get(index);
            int y = TOP + 1 + i * ROW_H;
            if (index == selected || inside(mouseX, mouseY, x, y, w, ROW_H)) {
                g.fill(x, y, x + w, y + ROW_H, index == selected ? SELECTED_BG : HOVER_BG);
            }
            g.fill(x, y + 1, x + 3, y + ROW_H - 1, row.colour());
            if (row.itemId != null) {
                g.item(ItemIndex.stack(row.itemId), x + 6, y + 1);
            }
            String name = row.itemId == null
                    ? Component.translatable("materialchecklist.review.no_item").getString()
                    : ItemIndex.name(row.itemId).getString();
            String amount = row.count == ParsedLine.NO_COUNT ? "?"
                    : StackMath.format(row.count, row.itemId == null ? 64 : ItemIndex.stackSize(row.itemId));
            g.text(this.font, trim(name, col2 - x - 30 - this.font.width(amount) - 8), x + 26, y + 5,
                    row.itemId == null ? BAD : TEXT, false);
            g.text(this.font, amount, col2 - 6 - this.font.width(amount), y + 5, row.colour(), false);
            g.text(this.font, trim(row.raw, x + w - col2 - 6), col2 + 2, y + 5, FAINT, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int mx = (int) event.x();
        int my = (int) event.y();
        if (mx >= x && mx < x + w && my >= TOP && my < rowsBottom) {
            int index = scroll + (my - TOP - 1) / ROW_H;
            if (index < rows.size()) {
                selected = index;
                syncSelection();
                if (doubleClick) {
                    changeSelected();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        scroll -= (int) Math.signum(deltaY);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(importScreen);
    }

    private int visibleRows() {
        return Math.max(1, (rowsBottom - TOP - 2) / ROW_H);
    }

    private void changeSelected() {
        if (selected < 0) {
            return;
        }
        Row row = rows.get(selected);
        String query = row.itemId == null ? guessQuery(row.raw) : "";
        this.minecraft.gui.setScreen(new ItemPickerScreen(this, query, id -> {
            row.itemId = id;
            row.sure = true;
        }));
    }

    /** The words of the raw line, so the picker opens already filtered near the right item. */
    private static String guessQuery(String raw) {
        String words = raw.replaceAll("[^\\p{L} ]", " ").strip().replaceAll("\\s+", " ");
        int space = words.indexOf(' ');
        return space > 0 ? words.substring(0, space) : words;
    }

    private void removeSelected() {
        if (selected >= 0) {
            rows.remove(selected);
            selected = Math.min(selected, rows.size() - 1);
            syncSelection();
        }
    }

    private void onCountTyped(String text) {
        if (settingCount || selected < 0) {
            return;
        }
        Row row = rows.get(selected);
        int stack = row.itemId == null ? 64 : ItemIndex.stackSize(row.itemId);
        OptionalLong value = StackMath.parse(text, stack);
        if (value.isPresent() && value.getAsLong() > 0) {
            row.count = value.getAsLong();
        }
    }

    private void syncSelection() {
        boolean has = selected >= 0 && selected < rows.size();
        changeButton.active = has;
        removeButton.active = has;
        countBox.active = has;
        countBox.setEditable(has);
        settingCount = true;
        countBox.setValue(has && rows.get(selected).count != ParsedLine.NO_COUNT
                ? Long.toString(rows.get(selected).count) : "");
        settingCount = false;
    }

    private void accept(boolean intoActive) {
        Map<String, Long> totals = new LinkedHashMap<>();
        for (Row row : rows) {
            if (row.itemId != null) {
                totals.merge(row.itemId, row.count == ParsedLine.NO_COUNT ? 1 : row.count, Long::sum);
            }
        }
        List<Entry> entries = new ArrayList<>();
        totals.forEach((id, n) -> entries.add(new Entry(id, n, false)));

        Project target = intoActive ? ProjectStore.active().orElse(null) : null;
        if (target == null) {
            String name = nameBox.getValue().isBlank()
                    ? Component.translatable("materialchecklist.screen.default_name").getString()
                    : nameBox.getValue();
            target = ProjectStore.create(name);
        }
        ProjectStore.save(target.merged(entries));
        ProjectStore.setActive(target.id());
        this.minecraft.gui.setScreen(new ChecklistScreen(importScreen.parentScreen() instanceof ChecklistScreen checklist
                ? checklist.parentScreen() : null));
    }

    private String trim(String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, maxWidth - this.font.width("...")) + "...";
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
