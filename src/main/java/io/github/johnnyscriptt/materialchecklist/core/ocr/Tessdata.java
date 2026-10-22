package io.github.johnnyscriptt.materialchecklist.core.ocr;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * The English Tesseract model ships inside the jar, but Tesseract reads models from a folder on
 * disk, so it is copied out once to a cache folder the caller chooses.
 */
public final class Tessdata {
    public static final String LANGUAGE = "eng";
    private static final String RESOURCE = "/assets/materialchecklist/tessdata/" + LANGUAGE + ".traineddata";

    private Tessdata() {
    }

    /**
     * Makes sure {@code dir} holds the bundled model and returns {@code dir}. A copy of the wrong
     * size (an interrupted earlier copy, or an older model) is replaced.
     */
    public static Path install(Path dir) throws IOException {
        Files.createDirectories(dir);
        Path target = dir.resolve(LANGUAGE + ".traineddata");
        try (InputStream in = Tessdata.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IOException("bundled OCR model missing: " + RESOURCE);
            }
            byte[] model = in.readAllBytes();
            if (!Files.exists(target) || Files.size(target) != model.length) {
                Path partial = dir.resolve(LANGUAGE + ".traineddata.part");
                Files.write(partial, model);
                Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return dir;
    }
}
