package io.github.johnnyscriptt.materialchecklist.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** One-line feedback above the hotbar, where vanilla shows "Now playing" and bed messages. */
public final class Hud {
    private Hud() {
    }

    public static void say(Minecraft client, Component message) {
        client.gui.hud.setOverlayMessage(message, false);
    }
}
