package io.github.johnnyscriptt.materialchecklist.client.ui;

import io.github.johnnyscriptt.materialchecklist.client.FindController;
import io.github.johnnyscriptt.materialchecklist.client.ItemIndex;
import io.github.johnnyscriptt.materialchecklist.client.ProgressModel;
import io.github.johnnyscriptt.materialchecklist.client.TrackedController;
import io.github.johnnyscriptt.materialchecklist.client.config.ConfigIo;
import io.github.johnnyscriptt.materialchecklist.client.net.ClientNet;
import io.github.johnnyscriptt.materialchecklist.client.store.ProjectStore;
import io.github.johnnyscriptt.materialchecklist.core.count.StackMath;
import io.github.johnnyscriptt.materialchecklist.core.list.Entry;
import io.github.johnnyscriptt.materialchecklist.core.list.Progress;
import io.github.johnnyscriptt.materialchecklist.core.list.Project;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.OptionalLong;

/**
 * The checklist manager (M): the player's lists down the left, the active list's items on the
 * right with have/need and a tick box, and the actions along the bottom.
 */
public final class ChecklistScreen extends Screen {
    private static final int M = 8;
    private static final int LIST_W = 110;
    private static final int PROJ_H = 14;
    private static final int ROW_H = 18;
    private static final int TOP = 24;
    private static final int BTN_H = 20;

    private static final int TEXT = 0xFFFFFFFF;
    private static final int FAINT = 0xFFA0A0A0;
    private static final int SUMMARY = 0xFFD0D0D0;
    private static final int ACTIVE_BG = 0x50FFFFFF;
    private static final int HOVER_BG = 0x25FFFFFF;
    private static final int PANEL_BG = 0x60000000;
    private static final int MISSING = 0xFFFF7A6B;
    private static final int PARTIAL = 0xFFFFD25A;
    private static final int DONE = 0xFF7CF27C;
    private static final int WARNING = 0xFFFF5555;

    private final Screen parent;
    private String selectedItem;
    private int scroll;
    private boolean settingNeeded;

    private EditBox nameBox;
    private EditBox neededBox;
    private Button tickButton;
    private Button removeButton;

    private int rx;
    private int rw;
    private int rowsTop;
    private int rowsBottom;

    public ChecklistScreen(Screen parent) {
        super(Component.translatable("materialchecklist.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rx = M + LIST_W + M;
        rw = this.width - rx - M;
        rowsTop = TOP + 40;
        rowsBottom = this.height - 2 * (BTN_H + 4) - 8;

        int bottom1 = this.height - 2 * (BTN_H + 4);
        int bottom2 = this.height - (BTN_H + 4);

        addRenderableWidget(Button.builder(Component.translatable("materialchecklist.screen.new_list"), b -> {
            ProjectStore.create(Component.translatable("materialchecklist.screen.default_name").getString());
            selectedItem = null;
            rebuildWidgets();
        }).bounds(M, bottom1, LIST_W, BTN_H).build());
        Button delete = addRenderableWidget(Button.builder(
                Component.translatable("materialchecklist.screen.delete_list"), b -> confirmDelete())
                .bounds(M, bottom2, LIST_W, BTN_H).build());

        Project project = ProjectStore.active().orElse(null);
        delete.active = project != null;

        int quarter = (rw - 6) / 4;
        addRenderableWidget(Button.builder(Component.translatable("materialchecklist.screen.import"),
                b -> this.minecraft.gui.setScreen(new ImportScreen(this))).bounds(rx, bottom1, quarter, BTN_H).build());
        addRenderableWidget(Button.builder(Component.translatable("materialchecklist.screen.add_item"),
                b -> pickItem()).bounds(rx + quarter + 2, bottom1, quarter, BTN_H).build());
        Button find = addRenderableWidget(Button.builder(Component.translatable("materialchecklist.screen.find"), b -> {
            onClose();
            FindController.find(this.minecraft);
        }).bounds(rx + 2 * (quarter + 2), bottom1, quarter, BTN_H).build());
        find.active = project != null && ClientNet.serverHasMod();
        addRenderableWidget(Button.builder(Component.translatable("materialchecklist.screen.settings"),
                b -> this.minecraft.gui.setScreen(new SettingsScreen(this)))
                .bounds(rx + 3 * (quarter + 2), bottom1, rw - 3 * (quarter + 2), BTN_H).build());

        neededBox = new EditBox(this.font, rx, bottom2, 70, BTN_H, Component.translatable("materialchecklist.screen.needed"));
        neededBox.setMaxLength(32);
        neededBox.setHint(Component.translatable("materialchecklist.screen.needed"));
        neededBox.setResponder(this::onNeededTyped);
        addRenderableWidget(neededBox);
        tickButton = addRenderableWidget(Button.builder(Component.translatable("materialchecklist.screen.tick"),
                b -> toggleSelected()).bounds(rx + 74, bottom2, 80, BTN_H).build());
        removeButton = addRenderableWidget(Button.builder(Component.translatable("materialchecklist.screen.remove"),
                b -> removeSelected()).bounds(rx + 158, bottom2, 60, BTN_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(rx + rw - 60, bottom2, 60, BTN_H).build());

        if (project != null) {
            nameBox = new EditBox(this.font, rx, TOP, rw, 18, Component.translatable("materialchecklist.screen.name"));
            nameBox.setMaxLength(48);
            nameBox.setValue(project.name());
            nameBox.setResponder(this::onRenamed);
            addRenderableWidget(nameBox);
        } else {
            nameBox = null;
        }
        syncSelection();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(this.font, getTitle(), this.width / 2, 8, TEXT);

        // Lists
        List<Project> projects = ProjectStore.all();
        Project active = ProjectStore.active().orElse(null);
        int listBottom = this.height - 2 * (BTN_H + 4) - 4;
        g.fill(M, TOP, M + LIST_W, listBottom, PANEL_BG);
        for (int i = 0; i < projects.size(); i++) {
            int y = TOP + 2 + i * PROJ_H;
            if (y + PROJ_H > listBottom) {
                break;
            }
            Project p = projects.get(i);
            boolean isActive = active != null && active.id().equals(p.id());
            if (isActive || inside(mouseX, mouseY, M, y, LIST_W, PROJ_H)) {
                g.fill(M, y, M + LIST_W, y + PROJ_H, isActive ? ACTIVE_BG : HOVER_BG);
            }
            g.text(this.font, trim(p.name(), LIST_W - 8), M + 4, y + 3, TEXT, false);
        }
        if (projects.isEmpty()) {
            g.textWithWordWrap(this.font, Component.translatable("materialchecklist.screen.no_lists"),
                    M + 4, TOP + 4, LIST_W - 8, FAINT);
        }

        if (active == null) {
            return;
        }

        // Summary line
        Progress progress = ProgressModel.progress();
        if (progress != null) {
            boolean modded = ClientNet.serverHasMod();
            Component summary = Component.translatable(modded ? "materialchecklist.screen.summary"
                            : "materialchecklist.screen.summary_no_server",
                    progress.doneCount(), progress.total(), Math.round(progress.fraction() * 100),
                    TrackedController.count());
            // A dark strip behind it: over a bright sky the grey text was all but invisible.
            g.fill(rx, TOP + 20, rx + rw, TOP + 36, PANEL_BG);
            g.text(this.font, trim(summary.getString(), rw - 8), rx + 4, TOP + 24, modded ? SUMMARY : WARNING, false);
        }

        // Entries
        g.fill(rx, rowsTop, rx + rw, rowsBottom, PANEL_BG);
        List<Entry> entries = active.entries();
        if (entries.isEmpty()) {
            g.textWithWordWrap(this.font, Component.translatable("materialchecklist.screen.empty"),
                    rx + 6, rowsTop + 6, rw - 12, FAINT);
        }
        int visible = visibleRows();
        scroll = Math.clamp(scroll, 0, Math.max(0, entries.size() - visible));
        boolean stacks = ConfigIo.get().stackNotation;
        for (int i = 0; i < visible && scroll + i < entries.size(); i++) {
            Entry entry = entries.get(scroll + i);
            int y = rowsTop + 1 + i * ROW_H;
            boolean selected = entry.itemId().equals(selectedItem);
            if (selected || inside(mouseX, mouseY, rx, y, rw, ROW_H)) {
                g.fill(rx, y, rx + rw, y + ROW_H, selected ? ACTIVE_BG : HOVER_BG);
            }
            long have = ProgressModel.have(entry.itemId());
            boolean done = entry.manuallyChecked() || have >= entry.needed();
            g.item(ItemIndex.stack(entry.itemId()), rx + 2, y + 1);
            int stack = ItemIndex.stackSize(entry.itemId());
            String amount = format(have, stack, stacks) + " / " + format(entry.needed(), stack, stacks);
            int boxX = rx + rw - 14;
            int amountX = boxX - 6 - this.font.width(amount);
            g.text(this.font, trim(ItemIndex.name(entry.itemId()).getString(), amountX - rx - 28),
                    rx + 22, y + 5, TEXT, false);
            g.text(this.font, amount, amountX, y + 5, done ? DONE : have > 0 ? PARTIAL : MISSING, false);
            g.outline(boxX, y + 4, 10, 10, done ? DONE : FAINT);
            if (done) {
                g.text(this.font, "✔", boxX + 2, y + 5, entry.manuallyChecked() ? TEXT : DONE, false);
            }
            if (inside(mouseX, mouseY, rx + 2, y + 1, 16, 16)) {
                g.setTooltipForNextFrame(this.font, tooltip(entry, have), mouseX, mouseY);
            }
        }
    }

    private Component tooltip(Entry entry, long have) {
        return Component.translatable("materialchecklist.screen.breakdown",
                ItemIndex.name(entry.itemId()), ProgressModel.inInventory(entry.itemId()),
                ProgressModel.inStorage(entry.itemId()), have, entry.needed());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int mx = (int) event.x();
        int my = (int) event.y();

        List<Project> projects = ProjectStore.all();
        if (mx >= M && mx < M + LIST_W && my >= TOP + 2) {
            int index = (my - TOP - 2) / PROJ_H;
            if (index < projects.size()) {
                ProjectStore.setActive(projects.get(index).id());
                selectedItem = null;
                scroll = 0;
                rebuildWidgets();
                return true;
            }
        }

        Project active = ProjectStore.active().orElse(null);
        if (active != null && mx >= rx && mx < rx + rw && my >= rowsTop && my < rowsBottom) {
            int index = scroll + (my - rowsTop - 1) / ROW_H;
            if (index < active.entries().size()) {
                Entry entry = active.entries().get(index);
                if (mx >= rx + rw - 16) {
                    ProjectStore.save(active.withEntry(entry.toggled()));
                }
                selectedItem = entry.itemId();
                syncSelection();
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
        ConfigIo.save();
        this.minecraft.gui.setScreen(parent);
    }

    /** The screen to return to on Done; null means back to the game. */
    Screen parentScreen() {
        return parent;
    }

    private int visibleRows() {
        return Math.max(1, (rowsBottom - rowsTop - 2) / ROW_H);
    }

    private void pickItem() {
        this.minecraft.gui.setScreen(new ItemPickerScreen(this, "", id -> {
            Project project = ProjectStore.active().orElseGet(() -> ProjectStore.create(
                    Component.translatable("materialchecklist.screen.default_name").getString()));
            boolean present = project.entries().stream().anyMatch(e -> e.itemId().equals(id));
            if (!present) {
                ProjectStore.save(project.withEntry(new Entry(id, ItemIndex.stackSize(id), false)));
            }
            selectedItem = id;
        }));
    }

    private void confirmDelete() {
        Project project = ProjectStore.active().orElse(null);
        if (project == null) {
            return;
        }
        this.minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                ProjectStore.delete(project.id());
                selectedItem = null;
            }
            this.minecraft.gui.setScreen(this);
        }, Component.translatable("materialchecklist.screen.delete_title"),
                Component.translatable("materialchecklist.screen.delete_message", project.name())));
    }

    private void onRenamed(String name) {
        ProjectStore.active().ifPresent(p -> ProjectStore.save(p.withName(name)));
    }

    private void onNeededTyped(String text) {
        if (settingNeeded || selectedItem == null) {
            return;
        }
        Project project = ProjectStore.active().orElse(null);
        Entry entry = selected(project);
        if (entry == null) {
            return;
        }
        OptionalLong value = StackMath.parse(text, ItemIndex.stackSize(entry.itemId()));
        if (value.isPresent() && value.getAsLong() > 0 && value.getAsLong() != entry.needed()) {
            ProjectStore.save(project.withEntry(entry.withNeeded(value.getAsLong())));
        }
    }

    private void toggleSelected() {
        Project project = ProjectStore.active().orElse(null);
        Entry entry = selected(project);
        if (entry != null) {
            ProjectStore.save(project.withEntry(entry.toggled()));
            syncSelection();
        }
    }

    private void removeSelected() {
        Project project = ProjectStore.active().orElse(null);
        Entry entry = selected(project);
        if (entry != null) {
            ProjectStore.save(project.without(entry.itemId()));
            selectedItem = null;
            syncSelection();
        }
    }

    private Entry selected(Project project) {
        if (project == null || selectedItem == null) {
            return null;
        }
        return project.entries().stream().filter(e -> e.itemId().equals(selectedItem)).findFirst().orElse(null);
    }

    /** Points the bottom controls at the selected entry, or greys them out. */
    private void syncSelection() {
        Entry entry = selected(ProjectStore.active().orElse(null));
        boolean has = entry != null;
        neededBox.active = has;
        neededBox.setEditable(has);
        tickButton.active = has;
        removeButton.active = has;
        settingNeeded = true;
        neededBox.setValue(has ? Long.toString(entry.needed()) : "");
        settingNeeded = false;
        tickButton.setMessage(Component.translatable(has && entry.manuallyChecked()
                ? "materialchecklist.screen.untick" : "materialchecklist.screen.tick"));
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

    private static String format(long n, int stack, boolean stacks) {
        return stacks ? StackMath.format(n, stack) : Long.toString(n);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
