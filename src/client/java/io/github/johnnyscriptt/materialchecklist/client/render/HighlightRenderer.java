package io.github.johnnyscriptt.materialchecklist.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.johnnyscriptt.materialchecklist.client.Boxes;
import io.github.johnnyscriptt.materialchecklist.client.FindController;
import io.github.johnnyscriptt.materialchecklist.client.TrackedController;
import io.github.johnnyscriptt.materialchecklist.client.config.ConfigIo;
import io.github.johnnyscriptt.materialchecklist.core.math.BlockPosL;
import io.github.johnnyscriptt.materialchecklist.core.math.BoxFaces;
import io.github.johnnyscriptt.materialchecklist.core.math.Pulse;
import io.github.johnnyscriptt.materialchecklist.core.search.HighlightState;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Tints containers in the world, two ways:
 *
 * <ul>
 *   <li><b>Find results</b> pulse gold and show through any number of blocks, for as long as the
 *       highlight lasts.</li>
 *   <li><b>Marked containers</b> get a steady soft blue while the checklist HUD is on. They are
 *       not drawn through walls: a base full of marked shulkers glowing through every floor
 *       would be noise.</li>
 * </ul>
 *
 * <p><b>Shape.</b> The overlay hugs the block's own outline shape rather than the full cube, and
 * a double chest is the union of both halves. Only camera-facing faces are drawn (see
 * {@link BoxFaces}), so the far side doesn't blend in behind the near side.
 *
 * <p><b>How the see-through works.</b> Every filled render type in 26.3 is depth tested, so the
 * x-ray geometry is moved to win the test instead: it is scaled uniformly about the camera to
 * {@link #XRAY_DISTANCE} blocks away. Scaling about the eye keeps every corner on its view ray,
 * so the overlay covers exactly the same pixels; only its depth changes.
 */
public final class HighlightRenderer {
    private static final int GOLD = 0xFFC400;
    private static final int BLUE = 0x3FA9FF;

    private static final float FIND_PEAK_ALPHA = 0.42f;
    private static final float TRACKED_ALPHA = 0.22f;
    /** Marked containers further than this aren't tinted; they'd only clutter the distance. */
    private static final double TRACKED_RANGE = 48.0;

    private static final double XRAY_DISTANCE = 0.5;

    public static final long PULSE_PERIOD_MS = 900L;
    public static final float PULSE_FLOOR = 0.6f;

    /** Lift the overlay just off the model so it never z-fights at arm's length. */
    private static final double INFLATE = 0.004;

    private HighlightRenderer() {
    }

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(HighlightRenderer::render);
    }

    private static void render(LevelRenderContext ctx) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null) {
            return;
        }
        long now = System.currentTimeMillis();
        String dimension = Boxes.dimensionKey(client);
        Vec3 cam = ctx.levelState().cameraRenderState.pos;
        PoseStack poses = ctx.poseStack();

        HighlightState tracked = TrackedController.state(client);
        if (ConfigIo.get().hudVisible && tracked.visibleIn(dimension, now)) {
            int alpha = Math.round(255 * TRACKED_ALPHA);
            submit(ctx, poses, level, cam, tracked, BLUE, alpha, false);
        }

        HighlightState found = FindController.state();
        if (found.visibleIn(dimension, now)) {
            float strength = found.fadeAlpha(now) * Pulse.at(now, PULSE_PERIOD_MS, PULSE_FLOOR);
            int alpha = Math.clamp(Math.round(255 * FIND_PEAK_ALPHA * strength), 0, 255);
            if (alpha > 0) {
                submit(ctx, poses, level, cam, found, GOLD, alpha, true);
            }
        }
    }

    private static void submit(LevelRenderContext ctx, PoseStack poses, ClientLevel level, Vec3 cam,
                               HighlightState state, int rgb, int alpha, boolean xray) {
        long[] mins = state.mins();
        long[] maxs = state.maxs();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        // No translate: vertices are emitted already relative to the camera, which also keeps
        // them small enough to stay precise in the float matrix far from spawn.
        ctx.submitNodeCollector().submitCustomGeometry(poses, RenderTypes.debugQuads(), (pose, vc) -> {
            for (int i = 0; i < mins.length; i++) {
                AABB box = overlayBounds(level, mins[i], maxs[i]);
                double x1 = box.minX - INFLATE - cam.x;
                double y1 = box.minY - INFLATE - cam.y;
                double z1 = box.minZ - INFLATE - cam.z;
                double x2 = box.maxX + INFLATE - cam.x;
                double y2 = box.maxY + INFLATE - cam.y;
                double z2 = box.maxZ + INFLATE - cam.z;
                double cx = (x1 + x2) * 0.5;
                double cy = (y1 + y2) * 0.5;
                double cz = (z1 + z2) * 0.5;
                if (!xray && cx * cx + cy * cy + cz * cz > TRACKED_RANGE * TRACKED_RANGE) {
                    continue;
                }
                int faces = BoxFaces.facingCamera(x1, y1, z1, x2, y2, z2);
                double scale = xray ? xrayScale(cx, cy, cz) : 1.0;
                emitFaces(pose, vc, faces,
                        (float) (x1 * scale), (float) (y1 * scale), (float) (z1 * scale),
                        (float) (x2 * scale), (float) (y2 * scale), (float) (z2 * scale),
                        r, g, b, alpha);
            }
        });
    }

    /**
     * World-space bounds of the container's model across the whole box. Falls back to the full
     * block range when the block has no outline: an unloaded chunk, or a container broken since.
     */
    private static AABB overlayBounds(ClientLevel level, long min, long max) {
        BlockPos lo = new BlockPos(BlockPosL.unpackX(min), BlockPosL.unpackY(min), BlockPosL.unpackZ(min));
        BlockPos hi = new BlockPos(BlockPosL.unpackX(max), BlockPosL.unpackY(max), BlockPosL.unpackZ(max));
        AABB full = new AABB(lo.getX(), lo.getY(), lo.getZ(), hi.getX() + 1, hi.getY() + 1, hi.getZ() + 1);

        AABB bounds = shapeBounds(level, lo);
        if (!hi.equals(lo)) {
            AABB other = shapeBounds(level, hi);
            bounds = bounds == null ? other : other == null ? bounds : bounds.minmax(other);
        }
        return bounds == null ? full : bounds;
    }

    private static AABB shapeBounds(ClientLevel level, BlockPos pos) {
        VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
        return shape.isEmpty() ? null : shape.bounds().move(pos);
    }

    /** Uniform scale about the camera that brings a camera-relative box in to {@link #XRAY_DISTANCE}. */
    private static double xrayScale(double cx, double cy, double cz) {
        double distance = Math.sqrt(cx * cx + cy * cy + cz * cz);
        return distance <= XRAY_DISTANCE ? 1.0 : XRAY_DISTANCE / distance;
    }

    private static void emitFaces(PoseStack.Pose pose, VertexConsumer vc, int faces,
                                  float x1, float y1, float z1, float x2, float y2, float z2,
                                  int r, int g, int b, int a) {
        if ((faces & BoxFaces.MIN_X) != 0) {
            quad(pose, vc, r, g, b, a, x1, y1, z1, x1, y2, z1, x1, y2, z2, x1, y1, z2);
        }
        if ((faces & BoxFaces.MAX_X) != 0) {
            quad(pose, vc, r, g, b, a, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1);
        }
        if ((faces & BoxFaces.MIN_Y) != 0) {
            quad(pose, vc, r, g, b, a, x1, y1, z1, x1, y1, z2, x2, y1, z2, x2, y1, z1);
        }
        if ((faces & BoxFaces.MAX_Y) != 0) {
            quad(pose, vc, r, g, b, a, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2);
        }
        if ((faces & BoxFaces.MIN_Z) != 0) {
            quad(pose, vc, r, g, b, a, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1);
        }
        if ((faces & BoxFaces.MAX_Z) != 0) {
            quad(pose, vc, r, g, b, a, x1, y1, z2, x1, y2, z2, x2, y2, z2, x2, y1, z2);
        }
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vc, int r, int g, int b, int a,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vc.addVertex(pose, ax, ay, az).setColor(r, g, b, a);
        vc.addVertex(pose, bx, by, bz).setColor(r, g, b, a);
        vc.addVertex(pose, cx, cy, cz).setColor(r, g, b, a);
        vc.addVertex(pose, dx, dy, dz).setColor(r, g, b, a);
    }
}
