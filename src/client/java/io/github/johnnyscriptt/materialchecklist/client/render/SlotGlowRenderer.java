package io.github.johnnyscriptt.materialchecklist.client.render;

import io.github.johnnyscriptt.materialchecklist.client.ProgressModel;
import io.github.johnnyscriptt.materialchecklist.client.config.ConfigIo;
import io.github.johnnyscriptt.materialchecklist.core.math.Pulse;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * Rings every container slot holding an item the active list still needs, so in a chest full
 * of everything the useful stacks are found at a glance.
 *
 * <p>The glow is a crisp ring on the slot's frame with two fainter rings outside it and a light
 * wash over the slot, breathing on the same pulse as the Find overlay. Slots in the player's
 * own inventory are skipped: those items already count, and lighting the hotbar too would
 * bury the answer.
 */
public final class SlotGlowRenderer {
    private static final int GOLD_RGB = 0xFFC400;

    private static final float RING_ALPHA = 1.0f;
    private static final float MID_ALPHA = 0.7f;
    private static final float OUTER_ALPHA = 0.4f;
    private static final float WASH_ALPHA = 0.28f;

    private static final int SLOT_SIZE = 16;

    private SlotGlowRenderer() {
    }

    /** Called with the pose already translated to the container's top-left corner. */
    public static void draw(GuiGraphicsExtractor graphics, List<Slot> slots) {
        if (!ConfigIo.get().glowMissingSlots) {
            return;
        }
        float pulse = Pulse.at(System.currentTimeMillis(),
                HighlightRenderer.PULSE_PERIOD_MS, HighlightRenderer.PULSE_FLOOR);

        for (Slot slot : slots) {
            if (slot.container instanceof Inventory || !slot.isActive()
                    || !ProgressModel.isMissing(slot.getItem())) {
                continue;
            }
            int x = slot.x;
            int y = slot.y;
            graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, argb(WASH_ALPHA * pulse));
            graphics.outline(x - 3, y - 3, SLOT_SIZE + 6, SLOT_SIZE + 6, argb(OUTER_ALPHA * pulse));
            graphics.outline(x - 2, y - 2, SLOT_SIZE + 4, SLOT_SIZE + 4, argb(MID_ALPHA * pulse));
            graphics.outline(x - 1, y - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, argb(RING_ALPHA * pulse));
        }
    }

    private static int argb(float alpha) {
        int a = Math.clamp(Math.round(alpha * 255), 0, 255);
        return (a << 24) | GOLD_RGB;
    }
}
