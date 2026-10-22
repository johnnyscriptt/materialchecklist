package io.github.johnnyscriptt.materialchecklist.client.ocr;

import io.github.johnnyscriptt.materialchecklist.client.store.ProjectStore;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Finds the screenshot the player most likely just took of a video. Looks in the mod's own
 * inbox folder and where each OS saves screenshots by default: Pictures\Screenshots on Windows
 * (also under OneDrive, where Windows 11 often redirects it), and the Desktop on macOS.
 * Minecraft's own screenshots folder is left out on purpose: its newest file is a picture of
 * the game, not of a list.
 */
public final class ScreenshotFinder {
    private ScreenshotFinder() {
    }

    public static Path inbox() {
        return ProjectStore.baseDir().resolve("inbox");
    }

    public static Optional<Path> newest() {
        Path home = Path.of(System.getProperty("user.home"));
        List<Path> folders = List.of(
                inbox(),
                home.resolve("Pictures").resolve("Screenshots"),
                home.resolve("OneDrive").resolve("Pictures").resolve("Screenshots"),
                home.resolve("Desktop"));
        Path best = null;
        long bestTime = Long.MIN_VALUE;
        for (Path folder : folders) {
            boolean desktop = folder.endsWith("Desktop");
            if (!Files.isDirectory(folder)) {
                continue;
            }
            try (DirectoryStream<Path> files = Files.newDirectoryStream(folder)) {
                for (Path file : files) {
                    String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (!isImage(name) || (desktop && !name.startsWith("screen"))) {
                        continue;
                    }
                    long time = Files.getLastModifiedTime(file).toMillis();
                    if (time > bestTime) {
                        bestTime = time;
                        best = file;
                    }
                }
            } catch (IOException ignored) {
                // An unreadable folder just isn't a source.
            }
        }
        return Optional.ofNullable(best);
    }

    public static boolean isImage(String fileName) {
        String name = fileName.toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".bmp") || name.endsWith(".gif");
    }
}
