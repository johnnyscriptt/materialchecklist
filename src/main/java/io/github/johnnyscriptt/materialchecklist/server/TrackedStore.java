package io.github.johnnyscriptt.materialchecklist.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;
import io.github.johnnyscriptt.materialchecklist.core.track.TrackedSpots;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Keeps {@link TrackedSpots} for the running world in {@code <world>/data/materialchecklist-tracked.json}.
 * Loaded when the server starts, written a few seconds after a change and again on shutdown.
 * Runs on the server thread only.
 */
public final class TrackedStore {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String FILE = "materialchecklist-tracked.json";
    /** Batches the writes that a player clicking Track on a row of chests would cause. */
    private static final int SAVE_DELAY_TICKS = 100;

    private static TrackedSpots spots = new TrackedSpots();
    private static Path file;
    private static int dirtyTicks = -1;

    private TrackedStore() {
    }

    public static TrackedSpots spots() {
        return spots;
    }

    public static void markDirty() {
        if (dirtyTicks < 0) {
            dirtyTicks = SAVE_DELAY_TICKS;
        }
    }

    public static void load(MinecraftServer server) {
        file = server.getWorldPath(LevelResource.DATA).resolve(FILE);
        spots = new TrackedSpots();
        dirtyTicks = -1;
        if (!Files.exists(file)) {
            return;
        }
        try {
            spots = TrackedSpots.fromSaved(GSON.fromJson(Files.readString(file), TrackedSpots.Saved.class));
        } catch (Exception e) {
            MaterialChecklistMod.LOGGER.warn("Could not read {}, starting with no tracked containers", file, e);
        }
    }

    public static void tick() {
        if (dirtyTicks > 0 && --dirtyTicks == 0) {
            save();
        }
    }

    public static void save() {
        dirtyTicks = -1;
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            Path partial = file.resolveSibling(FILE + ".tmp");
            Files.writeString(partial, GSON.toJson(spots.toSaved()));
            Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            MaterialChecklistMod.LOGGER.warn("Could not save {}", file, e);
        }
    }

    public static void unload() {
        save();
        file = null;
        spots = new TrackedSpots();
    }
}
