package io.github.johnnyscriptt.materialchecklist.client;

import io.github.johnnyscriptt.materialchecklist.client.config.ConfigIo;
import io.github.johnnyscriptt.materialchecklist.client.net.ClientNet;
import io.github.johnnyscriptt.materialchecklist.client.store.ProjectStore;
import io.github.johnnyscriptt.materialchecklist.core.list.Progress;
import io.github.johnnyscriptt.materialchecklist.core.list.Project;
import io.github.johnnyscriptt.materialchecklist.net.CountsPayload;
import io.github.johnnyscriptt.materialchecklist.world.StackCounter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * How much of the active list the player has: their own inventory (counted here, live, shulker
 * boxes included) plus their marked containers and ender chest (counted by the server, which
 * is the only side that can see inside them).
 */
public final class ProgressModel {
    /** The inventory changes constantly; a quarter second is fresh enough to read as live. */
    private static final int INVENTORY_EVERY_TICKS = 5;
    private static final long SERVER_EVERY_MS = 2_000L;

    private static Map<String, Long> inventory = Map.of();
    private static Map<String, Long> stored = Map.of();
    private static String storedFor;
    private static int storedContainers;
    private static long lastRequestMs;
    private static boolean refreshSoon;
    private static int tickCounter;

    private static Progress progress;
    private static Project progressFor;
    private static Set<String> missing = Set.of();

    private ProgressModel() {
    }

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        if (++tickCounter % INVENTORY_EVERY_TICKS == 0) {
            inventory = countInventory(player);
            progressFor = null; // recompute on next read
        }

        Project project = ProjectStore.active().orElse(null);
        if (project == null || !ClientNet.serverHasMod()) {
            return;
        }
        if (!project.id().equals(storedFor)) {
            stored = Map.of();
            storedFor = project.id();
            storedContainers = 0;
            refreshSoon = true;
            TrackedController.query(project);
        }
        long now = System.currentTimeMillis();
        boolean wanted = ConfigIo.get().hudVisible || client.gui.screen() != null;
        if (wanted && (refreshSoon || now - lastRequestMs >= SERVER_EVERY_MS)) {
            refreshSoon = false;
            lastRequestMs = now;
            ClientNet.requestCounts(project, ConfigIo.get().countEnderChest);
        }
    }

    public static void onCounts(CountsPayload payload) {
        if (!payload.projectId().equals(storedFor)) {
            return; // an answer for the list the player just switched away from
        }
        Map<String, Long> counts = new HashMap<>();
        for (Map.Entry<Identifier, Long> entry : payload.counts().entrySet()) {
            counts.put(entry.getKey().toString(), entry.getValue());
        }
        stored = counts;
        storedContainers = payload.containers();
        progressFor = null;
    }

    /** Ask the server again on the next tick: a container was just closed or (un)marked. */
    public static void refreshSoon() {
        refreshSoon = true;
    }

    /** Progress of the active list, or null when there is none. */
    public static Progress progress() {
        Project project = ProjectStore.active().orElse(null);
        if (project == null) {
            return null;
        }
        if (progress == null || progressFor != project) {
            progress = Progress.of(project.entries(), id -> have(id));
            progressFor = project;
            missing = progress.rows().stream().filter(r -> !r.done()).map(r -> r.entry().itemId())
                    .collect(Collectors.toUnmodifiableSet());
        }
        return progress;
    }

    public static long have(String itemId) {
        return inventory.getOrDefault(itemId, 0L) + stored.getOrDefault(itemId, 0L);
    }

    public static long inInventory(String itemId) {
        return inventory.getOrDefault(itemId, 0L);
    }

    public static long inStorage(String itemId) {
        return stored.getOrDefault(itemId, 0L);
    }

    public static int storedContainers() {
        return storedContainers;
    }

    /** True when {@code stack} is an item the active list still needs more of. */
    public static boolean isMissing(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        progress();
        return missing.contains(StackCounter.id(stack.getItem()));
    }

    public static void clear() {
        inventory = Map.of();
        stored = Map.of();
        storedFor = null;
        storedContainers = 0;
        progress = null;
        progressFor = null;
        missing = Set.of();
    }

    private static Map<String, Long> countInventory(LocalPlayer player) {
        Map<String, Long> counts = new HashMap<>();
        Inventory inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            StackCounter.add(inv.getItem(slot), counts);
        }
        // A stack held on the cursor is still the player's; without this the count flickers
        // every time an item is moved.
        StackCounter.add(player.containerMenu.getCarried(), counts);
        return counts;
    }
}
