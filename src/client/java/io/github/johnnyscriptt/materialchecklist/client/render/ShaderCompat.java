package io.github.johnnyscriptt.materialchecklist.client.render;

import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;

import java.lang.reflect.Method;

/**
 * Whether an Iris shader pack is drawing the world. Iris has no shader program for the plain
 * debug-quad pipeline the overlays normally use, so with a pack active they would vanish; the
 * renderer switches to one Iris does handle. Looked up by reflection through Iris's public API,
 * so Iris stays optional.
 */
final class ShaderCompat {
    private static boolean looked;
    private static Object api;
    private static Method inUse;

    private ShaderCompat() {
    }

    static boolean shaderPackInUse() {
        if (!looked) {
            looked = true;
            try {
                Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                api = type.getMethod("getInstance").invoke(null);
                inUse = type.getMethod("isShaderPackInUse");
            } catch (ClassNotFoundException e) {
                // No Iris installed: nothing to work around.
            } catch (ReflectiveOperationException | LinkageError e) {
                MaterialChecklistMod.LOGGER.warn("Iris is present but its API could not be read", e);
            }
        }
        if (inUse == null) {
            return false;
        }
        try {
            return (Boolean) inUse.invoke(api);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
