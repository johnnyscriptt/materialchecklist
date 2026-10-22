package io.github.johnnyscriptt.materialchecklist.core.math;

/**
 * Which faces of an axis-aligned box point back at a camera sitting at the origin.
 *
 * <p>The highlight overlay is translucent and drawn with culling off, so drawing all six faces
 * would stack the far side on top of the near side and tint the box unevenly. Picking the
 * camera-facing faces here instead makes the tint uniform and does not depend on the winding
 * order the render pipeline expects.
 */
public final class BoxFaces {
    public static final int MIN_X = 1;
    public static final int MAX_X = 1 << 1;
    public static final int MIN_Y = 1 << 2;
    public static final int MAX_Y = 1 << 3;
    public static final int MIN_Z = 1 << 4;
    public static final int MAX_Z = 1 << 5;
    public static final int ALL = MIN_X | MAX_X | MIN_Y | MAX_Y | MIN_Z | MAX_Z;

    private BoxFaces() {
    }

    /**
     * Bitmask of the faces the eye can see, for a box given in camera-relative coordinates. A
     * face the camera is level with is edge-on and left out. With the camera inside the box
     * every face is returned, so the tint still shows from within.
     */
    public static int facingCamera(double minX, double minY, double minZ,
                                   double maxX, double maxY, double maxZ) {
        boolean inside = minX < 0 && maxX > 0 && minY < 0 && maxY > 0 && minZ < 0 && maxZ > 0;
        if (inside) {
            return ALL;
        }
        int faces = 0;
        if (minX > 0) {
            faces |= MIN_X;
        }
        if (maxX < 0) {
            faces |= MAX_X;
        }
        if (minY > 0) {
            faces |= MIN_Y;
        }
        if (maxY < 0) {
            faces |= MAX_Y;
        }
        if (minZ > 0) {
            faces |= MIN_Z;
        }
        if (maxZ < 0) {
            faces |= MAX_Z;
        }
        return faces;
    }
}
