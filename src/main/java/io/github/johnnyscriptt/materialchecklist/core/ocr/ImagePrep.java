package io.github.johnnyscriptt.materialchecklist.core.ocr;

import java.awt.image.BufferedImage;

/**
 * Turns a screenshot into the single-channel, dark-on-light, large-text image Tesseract reads
 * best.
 *
 * <p>Game menus are light text on a dark panel, often coloured (Litematica draws missing items
 * in red and available ones in green). Luminance alone leaves dark red close to the panel, so a
 * dark image is read by its brightest channel instead, then inverted: any saturated colour
 * stands out against black. A light image is read by its darkest channel for the same reason.
 */
public final class ImagePrep {
    /** Tesseract is most accurate when glyphs are around 30 px tall; video frames are smaller. */
    static final int TARGET_WIDTH = 2400;
    static final int MAX_SCALE = 4;

    private ImagePrep() {
    }

    /** @param scale how many prepared pixels one source pixel became */
    public record Gray(byte[] pixels, int width, int height, int scale) {
    }

    /** How to enlarge and whether to cut to pure black and white; tuned against real fixtures. */
    record Settings(int scale, boolean nearest, boolean binarize) {
        static final Settings DEFAULT = new Settings(0, false, false);
    }

    public static Gray prepare(BufferedImage image) {
        return prepare(image, Settings.DEFAULT);
    }

    static Gray prepare(BufferedImage image, Settings settings) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] rgb = image.getRGB(0, 0, w, h, null, 0, w);

        boolean dark = meanLuminance(rgb) < 110;
        int[] value = new int[rgb.length];
        for (int i = 0; i < rgb.length; i++) {
            int r = (rgb[i] >> 16) & 0xFF;
            int g = (rgb[i] >> 8) & 0xFF;
            int b = rgb[i] & 0xFF;
            value[i] = dark ? 255 - Math.max(r, Math.max(g, b)) : Math.min(r, Math.min(g, b));
        }
        stretch(value);

        int scale = settings.scale() > 0 ? settings.scale() : scaleFor(w);
        Gray gray = settings.nearest() ? nearest(value, w, h, scale) : upscale(value, w, h, scale);
        return settings.binarize() ? binarize(gray) : gray;
    }

    private static Gray nearest(int[] value, int w, int h, int scale) {
        int ow = w * scale;
        byte[] out = new byte[ow * h * scale];
        for (int y = 0; y < h * scale; y++) {
            int row = (y / scale) * w;
            for (int x = 0; x < ow; x++) {
                out[y * ow + x] = (byte) value[row + x / scale];
            }
        }
        return new Gray(out, ow, h * scale, scale);
    }

    /** Otsu's threshold: the cut that best separates ink from background. */
    private static Gray binarize(Gray gray) {
        byte[] px = gray.pixels();
        int[] histogram = new int[256];
        for (byte b : px) {
            histogram[b & 0xFF]++;
        }
        long total = px.length;
        double sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += (double) i * histogram[i];
        }
        double sumBack = 0;
        long weightBack = 0;
        double best = -1;
        int threshold = 128;
        for (int t = 0; t < 256; t++) {
            weightBack += histogram[t];
            if (weightBack == 0) {
                continue;
            }
            long weightFore = total - weightBack;
            if (weightFore == 0) {
                break;
            }
            sumBack += (double) t * histogram[t];
            double meanBack = sumBack / weightBack;
            double meanFore = (sum - sumBack) / weightFore;
            double between = (double) weightBack * weightFore * (meanBack - meanFore) * (meanBack - meanFore);
            if (between > best) {
                best = between;
                threshold = t;
            }
        }
        byte[] out = new byte[px.length];
        for (int i = 0; i < px.length; i++) {
            out[i] = (byte) ((px[i] & 0xFF) > threshold ? 255 : 0);
        }
        return new Gray(out, gray.width(), gray.height(), gray.scale());
    }

    static int scaleFor(int width) {
        return Math.clamp((TARGET_WIDTH + width - 1) / Math.max(1, width), 1, MAX_SCALE);
    }

    private static double meanLuminance(int[] rgb) {
        long sum = 0;
        for (int p : rgb) {
            sum += (299L * ((p >> 16) & 0xFF) + 587L * ((p >> 8) & 0xFF) + 114L * (p & 0xFF)) / 1000;
        }
        return rgb.length == 0 ? 255 : (double) sum / rgb.length;
    }

    /** Spreads the 2nd..98th percentile across the full range, so faint text gets real contrast. */
    private static void stretch(int[] value) {
        int[] histogram = new int[256];
        for (int v : value) {
            histogram[v]++;
        }
        int lo = percentile(histogram, value.length, 0.02);
        int hi = percentile(histogram, value.length, 0.98);
        if (hi <= lo) {
            return;
        }
        for (int i = 0; i < value.length; i++) {
            value[i] = Math.clamp((value[i] - lo) * 255 / (hi - lo), 0, 255);
        }
    }

    private static int percentile(int[] histogram, int total, double fraction) {
        long wanted = Math.round(total * fraction);
        long seen = 0;
        for (int v = 0; v < 256; v++) {
            seen += histogram[v];
            if (seen > wanted) {
                return v;
            }
        }
        return 255;
    }

    /** Bilinear upscale; nearest-neighbour would keep video compression blocks as hard edges. */
    private static Gray upscale(int[] value, int w, int h, int scale) {
        int ow = w * scale;
        int oh = h * scale;
        byte[] out = new byte[ow * oh];
        for (int y = 0; y < oh; y++) {
            double sy = Math.max(0, (y + 0.5) / scale - 0.5);
            int y0 = Math.min((int) sy, h - 1);
            int y1 = Math.min(y0 + 1, h - 1);
            double fy = sy - y0;
            for (int x = 0; x < ow; x++) {
                double sx = Math.max(0, (x + 0.5) / scale - 0.5);
                int x0 = Math.min((int) sx, w - 1);
                int x1 = Math.min(x0 + 1, w - 1);
                double fx = sx - x0;
                double top = value[y0 * w + x0] * (1 - fx) + value[y0 * w + x1] * fx;
                double bottom = value[y1 * w + x0] * (1 - fx) + value[y1 * w + x1] * fx;
                out[y * ow + x] = (byte) Math.round(top * (1 - fy) + bottom * fy);
            }
        }
        return new Gray(out, ow, oh, scale);
    }
}
