package io.github.johnnyscriptt.materialchecklist.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Where a container's panel sits, so the Track button can be placed just above it. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int materialchecklist$leftPos();

    @Accessor("topPos")
    int materialchecklist$topPos();

    @Accessor("imageWidth")
    int materialchecklist$imageWidth();
}
