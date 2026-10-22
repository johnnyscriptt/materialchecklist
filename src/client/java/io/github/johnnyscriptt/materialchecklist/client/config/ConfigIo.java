package io.github.johnnyscriptt.materialchecklist.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;
import io.github.johnnyscriptt.materialchecklist.core.config.ChecklistConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads and saves the single {@link ChecklistConfig} instance at config/materialchecklist.json. */
public final class ConfigIo {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ChecklistConfig config;

    private ConfigIo() {
    }

    public static synchronized ChecklistConfig get() {
        if (config == null) {
            config = load();
        }
        return config;
    }

    public static synchronized void save() {
        if (config == null) {
            return;
        }
        config.clamp();
        try {
            Files.writeString(path(), GSON.toJson(config));
        } catch (IOException e) {
            MaterialChecklistMod.LOGGER.warn("Could not save materialchecklist.json", e);
        }
    }

    private static ChecklistConfig load() {
        Path file = path();
        if (Files.exists(file)) {
            try {
                ChecklistConfig loaded = GSON.fromJson(Files.readString(file), ChecklistConfig.class);
                if (loaded != null) {
                    loaded.clamp();
                    return loaded;
                }
            } catch (Exception e) {
                MaterialChecklistMod.LOGGER.warn("Could not read materialchecklist.json, using defaults", e);
            }
        }
        return new ChecklistConfig();
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("materialchecklist.json");
    }
}
