package io.github.johnnyscriptt.materialchecklist.gametest;

import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;
import io.github.johnnyscriptt.materialchecklist.client.FindController;
import io.github.johnnyscriptt.materialchecklist.client.ItemIndex;
import io.github.johnnyscriptt.materialchecklist.client.ProgressModel;
import io.github.johnnyscriptt.materialchecklist.client.TrackedController;
import io.github.johnnyscriptt.materialchecklist.client.ocr.GameFont;
import io.github.johnnyscriptt.materialchecklist.client.store.ProjectStore;
import io.github.johnnyscriptt.materialchecklist.client.ui.ChecklistScreen;
import io.github.johnnyscriptt.materialchecklist.client.ui.ImportScreen;
import io.github.johnnyscriptt.materialchecklist.client.ui.ReviewScreen;
import io.github.johnnyscriptt.materialchecklist.core.list.Entry;
import io.github.johnnyscriptt.materialchecklist.core.list.Project;
import io.github.johnnyscriptt.materialchecklist.core.ocr.OcrEngine;
import io.github.johnnyscriptt.materialchecklist.core.ocr.Tessdata;
import io.github.johnnyscriptt.materialchecklist.core.parse.ListParser;
import io.github.johnnyscriptt.materialchecklist.core.parse.OcrCleanup;
import io.github.johnnyscriptt.materialchecklist.core.parse.ParsedLine;
import io.github.johnnyscriptt.materialchecklist.net.TrackTogglePayload;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

/**
 * Drives the whole loop in a real game: a list is made, a chest holding a shulker of logs is
 * marked, counts arrive from inventory and storage, Find lights up a barrel further away, and
 * the screens are opened. Finishes by screenshotting a list drawn in the game's own font and
 * reading it back through OCR, which is the closest thing to a YouTube screenshot of a
 * Litematica list that can be produced automatically.
 *
 * <p>Screenshots: {@code build/run/clientGameTest/screenshots/}.
 */
public final class ChecklistGameTest implements FabricClientGameTest {
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;

    private static final String LIST_TEXT = """
            Glass 64
            Oak Log 32
            Lantern 8
            Stone Bricks 1 st
            Spruce Planks 3 stacks
            Glass Pane 2 sb
            """;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamerule advance_time false");

            BlockPos base = world.getServer().computeOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                BlockPos origin = player.blockPosition().above(24);
                build(player.level(), origin);
                player.getInventory().add(new ItemStack(Items.GLASS, 30));
                player.getInventory().add(shulkerOf(new ItemStack(Items.STONE_BRICKS, 64)));
                return origin;
            });
            BlockPos trackedChest = base.offset(1, 0, 2);
            BlockPos barrel = base.offset(0, 0, 12);

            standAt(context, world, base);
            context.getInput().lookAt(trackedChest);

            // A list, made the way the import screen makes one.
            String projectId = context.computeOnClient(client -> {
                List<ParsedLine> lines = new ListParser(ItemIndex.matcher()).parse(LIST_TEXT);
                Project project = ProjectStore.create("Gametest House");
                Project filled = project.merged(ListParser.toEntries(lines));
                ProjectStore.save(filled);
                return filled.id();
            });
            check(context.computeOnClient(c -> ProjectStore.active().orElseThrow().entries().size()) == 6,
                    "all six lines should parse into entries");

            // Mark the chest; the server answers with the tracked list.
            context.runOnClient(client -> ClientPlayNetworking.send(new TrackTogglePayload(projectId, trackedChest)));
            context.waitFor(client -> TrackedController.count() == 1, 100);

            // 30 glass carried + 40 in the chest; 32 logs inside a shulker inside the chest;
            // 64 stone bricks inside a shulker in the inventory.
            context.waitFor(client -> ProgressModel.have("minecraft:glass") == 70
                    && ProgressModel.have("minecraft:oak_log") == 32
                    && ProgressModel.have("minecraft:stone_bricks") == 64, 200);
            check(context.computeOnClient(c -> ProgressModel.progress().doneCount()) == 3,
                    "glass, logs and stone bricks should be done");

            // Find: the lantern barrel is out of reach of the tracked chest but within the radius.
            context.runOnClient(FindController::find);
            context.waitFor(client -> !FindController.state().isEmpty(), 100);
            check(context.computeOnClient(c -> FindController.foundNearby(c, "minecraft:lantern")),
                    "the barrel's lanterns should be reported nearby");

            context.getInput().lookAt(barrel);
            settle(context, world);
            shot(context, "01-hud-and-find");

            context.getInput().lookAt(trackedChest);
            settle(context, world);
            shot(context, "02-tracked-chest");

            // Inside a container: missing items glow. Containers close themselves on a player
            // who is out of reach, so walk round the wall to the barrel first.
            standAt(context, world, barrel.offset(0, 0, -2));
            context.getInput().lookAt(barrel);
            context.waitTicks(5);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                player.openMenu(level.getBlockState(barrel).getMenuProvider(level, barrel));
            });
            context.waitForScreen(ContainerScreen.class);
            context.waitTicks(10);
            check(context.computeOnClient(client -> Screens.getWidgets(client.gui.screen()).stream()
                    .anyMatch(w -> w.getMessage().getString().startsWith("Track"))),
                    "an opened container should get a Track button");
            context.takeScreenshot(TestScreenshotOptions.of("03-slot-glow").disableCounterPrefix());
            context.setScreen(() -> null);

            context.setScreen(() -> new ChecklistScreen(null));
            context.waitTicks(10);
            context.takeScreenshot(TestScreenshotOptions.of("04-checklist-screen").disableCounterPrefix());

            String pasted = "Polished Andesite 2 stacks\n- Spruce Trapdoor x24\n3 shulkers of white concrete\nCobb1estone 1,280\nBirch Fence";
            context.setScreen(() -> new ImportScreen(null, pasted));
            context.waitTicks(5);
            context.takeScreenshot(TestScreenshotOptions.of("05-import-screen").disableCounterPrefix());
            context.setScreen(() -> new ReviewScreen(new ImportScreen(null, pasted),
                    new ListParser(ItemIndex.matcher()).parse(pasted)));
            context.waitTicks(5);
            context.takeScreenshot(TestScreenshotOptions.of("06-review-screen").disableCounterPrefix());

            // OCR on the game's own font: show a list in the import box and read the screenshot back.
            String fontList = "Oak Planks 512\nStone Bricks 1088\nGlass Pane 36\nSpruce Log 96\nLantern 12\nCobblestone 640";
            context.setScreen(() -> new ImportScreen(null, fontList));
            context.waitTicks(5);
            Path shot = context.takeScreenshot(TestScreenshotOptions.of("07-ocr-source").disableCounterPrefix());
            context.setScreen(() -> null);
            int matched = readBack(context, shot);
            check(matched >= 5, "OCR on the game font matched only " + matched + " of 6 lines");
        }
    }

    /** OCRs a screenshot and counts lines that came back as the right item with the right amount. */
    private static int readBack(ClientGameTestContext context, Path image) {
        try (OcrEngine engine = OcrEngine.open(Tessdata.install(MaterialChecklistMod.ocrCacheDir()))) {
            BufferedImage picture = ImageIO.read(image.toFile());
            String text = OcrCleanup.clean(engine.recognize(picture,
                    context.computeOnClient(GameFont::numbers)));
            List<ParsedLine> lines = context.computeOnClient(c -> new ListParser(ItemIndex.matcher()).parse(text));
            List<Entry> expected = List.of(
                    new Entry("minecraft:oak_planks", 512, false),
                    new Entry("minecraft:stone_bricks", 1088, false),
                    new Entry("minecraft:glass_pane", 36, false),
                    new Entry("minecraft:spruce_log", 96, false),
                    new Entry("minecraft:lantern", 12, false),
                    new Entry("minecraft:cobblestone", 640, false));
            List<Entry> read = ListParser.toEntries(lines);
            MaterialChecklistMod.LOGGER.info("OCR read back: {}", read);
            int hits = 0;
            for (Entry e : expected) {
                if (read.contains(e)) {
                    hits++;
                }
            }
            return hits;
        } catch (Exception e) {
            throw new AssertionError("OCR read-back failed", e);
        }
    }

    private static void build(ServerLevel level, BlockPos base) {
        for (int x = -6; x <= 6; x++) {
            for (int z = -4; z <= 14; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
                for (int y = 0; y <= 4; y++) {
                    level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        BlockState north = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
        BlockPos chest = base.offset(1, 0, 2);
        level.setBlockAndUpdate(chest, north);
        put(level, chest, 0, new ItemStack(Items.GLASS, 40));
        put(level, chest, 4, shulkerOf(new ItemStack(Items.OAK_LOG, 32)));

        // An untracked chest with nothing on the list, so the scene isn't only matches.
        BlockPos decoy = base.offset(-1, 0, 2);
        level.setBlockAndUpdate(decoy, north);
        put(level, decoy, 0, new ItemStack(Items.COBBLESTONE, 64));

        BlockPos barrel = base.offset(0, 0, 12);
        level.setBlockAndUpdate(barrel, Blocks.BARREL.defaultBlockState());
        put(level, barrel, 3, new ItemStack(Items.LANTERN, 8));
        put(level, barrel, 5, new ItemStack(Items.TORCH, 20));
        put(level, barrel, 7, new ItemStack(Items.SPRUCE_PLANKS, 50));
        // A wall between the player and the barrel, to show the Find tint goes through it.
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 3; y++) {
                level.setBlockAndUpdate(base.offset(x, y, 9), Blocks.STONE_BRICKS.defaultBlockState());
            }
        }
    }

    private static ItemStack shulkerOf(ItemStack inside) {
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(inside)));
        return shulker;
    }

    private static void put(ServerLevel level, BlockPos pos, int slot, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof Container container) {
            container.setItem(slot, stack);
        }
    }

    private static void standAt(ClientGameTestContext context, TestSingleplayerContext world, BlockPos pos) {
        world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
                .teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
        context.waitFor(client -> client.player != null && client.player.blockPosition().equals(pos), 100);
    }

    private static void settle(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getConnection().waitForChunksRender();
        context.waitTicks(20);
    }

    private static void shot(ClientGameTestContext context, String name) {
        context.takeScreenshot(TestScreenshotOptions.of(name).withSize(WIDTH, HEIGHT).disableCounterPrefix());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
