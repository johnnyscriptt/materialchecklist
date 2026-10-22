package io.github.johnnyscriptt.materialchecklist.client.ocr;

import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;
import io.github.johnnyscriptt.materialchecklist.core.ocr.GlyphSet;
import io.github.johnnyscriptt.materialchecklist.core.ocr.OcrEngine;
import io.github.johnnyscriptt.materialchecklist.core.ocr.Tessdata;
import io.github.johnnyscriptt.materialchecklist.core.parse.OcrCleanup;
import net.minecraft.util.Util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Reads screenshots off the render thread. The engine is opened on first use, not at game
 * start, so players who never import a screenshot never pay for loading Tesseract.
 */
public final class OcrService {
    private static CompletableFuture<OcrEngine> engine;

    private OcrService() {
    }

    /**
     * @param numberGlyphs the game font's digits, for amounts drawn in it; null to trust Tesseract
     * @return the screenshot's text, cleaned up for the list parser
     */
    public static CompletableFuture<String> read(Path image, GlyphSet numberGlyphs) {
        return engine().thenApplyAsync(ocr -> {
            BufferedImage picture;
            try {
                picture = ImageIO.read(image.toFile());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (picture == null) {
                throw new CompletionException(new IOException("not a PNG, JPG, BMP or GIF image"));
            }
            return OcrCleanup.clean(ocr.recognize(picture, numberGlyphs));
        }, Util.backgroundExecutor());
    }

    private static synchronized CompletableFuture<OcrEngine> engine() {
        if (engine == null || engine.isCompletedExceptionally()) {
            engine = CompletableFuture.supplyAsync(() -> {
                try {
                    return OcrEngine.open(Tessdata.install(MaterialChecklistMod.ocrCacheDir()));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, Util.backgroundExecutor());
        }
        return engine;
    }

    /** The innermost cause's message, which is the one worth showing a player. */
    public static String describe(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }
}
