package io.github.johnnyscriptt.materialchecklist.client.ocr;

import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;
import io.github.johnnyscriptt.materialchecklist.core.ocr.GlyphSet;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * The digit shapes screenshots are compared against: the vanilla font, which is what build
 * videos almost always show, plus the font of any resource pack the player runs (Faithful and
 * friends redraw it), so a screenshot of their own game reads too.
 */
public final class GameFont {
    private static final Identifier ATLAS = Identifier.withDefaultNamespace("textures/font/ascii.png");
    private static GlyphSet numbers;

    private GameFont() {
    }

    /** @return the number glyphs, or null if no font texture can be read (then Tesseract alone reads amounts) */
    public static synchronized GlyphSet numbers(Minecraft client) {
        if (numbers == null) {
            GlyphSet vanilla = load(client.getVanillaPackResources().asResourceManager(), "vanilla");
            GlyphSet active = load(client.getResourceManager(), "active");
            numbers = GlyphSet.union(vanilla, active);
        }
        return numbers.isEmpty() ? null : numbers;
    }

    /** Resource packs can change between reads; drop the cached shapes when they reload. */
    public static synchronized void invalidate() {
        numbers = null;
    }

    private static GlyphSet load(ResourceManager resources, String which) {
        Optional<Resource> resource = resources.getResource(ATLAS);
        if (resource.isPresent()) {
            try (InputStream in = resource.get().open()) {
                return GlyphSet.fromAtlas(ImageIO.read(in), GlyphSet.NUMBER_CHARS);
            } catch (Exception e) {
                MaterialChecklistMod.LOGGER.warn("Could not read the {} font texture for OCR", which, e);
            }
        }
        return GlyphSet.of(Map.of());
    }
}
