package io.github.johnnyscriptt.materialchecklist.client.mixin;

import io.github.johnnyscriptt.materialchecklist.client.render.SlotGlowRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the missing-item slot glow right after the slots themselves: above the item icons, but
 * below the hover highlight, the carried stack and tooltips. At this point the pose is already
 * translated to the container's corner, so slot coordinates can be used as they are.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Inject(method = "extractSlots", at = @At("TAIL"))
    private void materialchecklist$glowMissingSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                    CallbackInfo ci) {
        // The creative tabs are a catalogue of every item, not a container, so glow would be noise.
        if ((Object) this instanceof CreativeModeInventoryScreen) {
            return;
        }
        SlotGlowRenderer.draw(graphics, menu.slots);
    }
}
