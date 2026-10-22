package io.github.johnnyscriptt.materialchecklist.client.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;
import io.github.johnnyscriptt.materialchecklist.core.list.Project;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The player's material lists, one JSON file each under config/materialchecklist/projects/.
 * They live with the game install rather than with a world, so a list made in a creative
 * test world is still there in survival. Every change is written straight away; the files
 * are small and a crash shouldn't cost a typed-in list.
 */
public final class ProjectStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Map<String, Project> projects;
    private static String activeId;

    private record State(String activeProject) {
    }

    private ProjectStore() {
    }

    public static synchronized List<Project> all() {
        ensureLoaded();
        List<Project> out = new ArrayList<>(projects.values());
        out.sort(Comparator.comparing(p -> p.name().toLowerCase(Locale.ROOT)));
        return out;
    }

    public static synchronized Optional<Project> active() {
        ensureLoaded();
        return Optional.ofNullable(activeId == null ? null : projects.get(activeId));
    }

    public static synchronized void setActive(String id) {
        ensureLoaded();
        activeId = id != null && projects.containsKey(id) ? id : null;
        writeState();
    }

    /** Creates an empty list, makes it the active one and returns it. */
    public static synchronized Project create(String name) {
        ensureLoaded();
        Project project = new Project(UUID.randomUUID().toString(), name, List.of());
        save(project);
        setActive(project.id());
        return project;
    }

    public static synchronized void save(Project project) {
        ensureLoaded();
        projects.put(project.id(), project);
        try {
            Files.createDirectories(dir());
            Files.writeString(fileFor(project.id()), GSON.toJson(project));
        } catch (IOException e) {
            MaterialChecklistMod.LOGGER.warn("Could not save list {}", project.name(), e);
        }
    }

    public static synchronized void delete(String id) {
        ensureLoaded();
        projects.remove(id);
        try {
            Files.deleteIfExists(fileFor(id));
        } catch (IOException e) {
            MaterialChecklistMod.LOGGER.warn("Could not delete list file {}", id, e);
        }
        if (id.equals(activeId)) {
            setActive(projects.isEmpty() ? null : all().getFirst().id());
        }
    }

    private static void ensureLoaded() {
        if (projects != null) {
            return;
        }
        projects = new LinkedHashMap<>();
        Path dir = dir();
        if (Files.isDirectory(dir)) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.json")) {
                for (Path file : files) {
                    read(file);
                }
            } catch (IOException e) {
                MaterialChecklistMod.LOGGER.warn("Could not list saved material lists", e);
            }
        }
        Path state = stateFile();
        if (Files.exists(state)) {
            try {
                State loaded = GSON.fromJson(Files.readString(state), State.class);
                activeId = loaded == null ? null : loaded.activeProject();
            } catch (Exception e) {
                MaterialChecklistMod.LOGGER.warn("Could not read {}", state, e);
            }
        }
        if (activeId != null && !projects.containsKey(activeId)) {
            activeId = null;
        }
    }

    private static void read(Path file) {
        try {
            Project project = GSON.fromJson(Files.readString(file), Project.class);
            if (project != null && project.id() != null) {
                projects.put(project.id(), project);
            }
        } catch (Exception e) {
            MaterialChecklistMod.LOGGER.warn("Skipping unreadable list file {}", file, e);
        }
    }

    private static void writeState() {
        try {
            Files.createDirectories(stateFile().getParent());
            Files.writeString(stateFile(), GSON.toJson(new State(activeId)));
        } catch (IOException e) {
            MaterialChecklistMod.LOGGER.warn("Could not save the active list", e);
        }
    }

    private static Path fileFor(String id) {
        // Ids are UUIDs we made; strip anything else so a hand-edited file can't escape the folder.
        return dir().resolve(id.replaceAll("[^A-Za-z0-9-]", "_") + ".json");
    }

    public static Path baseDir() {
        return FabricLoader.getInstance().getConfigDir().resolve(MaterialChecklistMod.MOD_ID);
    }

    private static Path dir() {
        return baseDir().resolve("projects");
    }

    private static Path stateFile() {
        return baseDir().resolve("state.json");
    }
}
