package io.github.johnnyscriptt.materialchecklist.client.ocr;

import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;
import io.github.johnnyscriptt.materialchecklist.core.ocr.GlyphSet;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * The digit shapes of the font this game is drawing with, read from its font texture so a
 * resource pack's font is what screenshots get compared against.
 */
public final class GameFont {
    private static final Identifier ATLAS = Identifier.withDefaultNamespace("textures/font/ascii.png");
    private static GlyphSet numbers;

    private GameFont() {
    }

    /** @return the number glyphs, or null if the texture can't be read (then Tesseract alone reads amounts) */
    public static synchronized GlyphSet numbers(Minecraft client) {
        if (numbers == null) {
            numbers = load(client);
        }
        return numbers.isEmpty() ? null : numbers;
    }

    private static GlyphSet load(Minecraft client) {
        Optional<Resource> resource = client.getResourceManager().getResource(ATLAS);
        if (resource.isPresent()) {
            try (InputStream in = resource.get().open()) {
                return GlyphSet.fromAtlas(ImageIO.read(in), GlyphSet.NUMBER_CHARS);
            } catch (Exception e) {
                MaterialChecklistMod.LOGGER.warn("Could not read the font texture for OCR", e);
            }
        }
        return GlyphSet.of(Map.of());
    }
}
