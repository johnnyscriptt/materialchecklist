package io.github.johnnyscriptt.materialchecklist.gametest;

import io.github.johnnyscriptt.materialchecklist.client.FindController;
import io.github.johnnyscriptt.materialchecklist.client.ItemIndex;
import io.github.johnnyscriptt.materialchecklist.client.ProgressModel;
import io.github.johnnyscriptt.materialchecklist.client.TrackedController;
import io.github.johnnyscriptt.materialchecklist.client.config.ConfigIo;
import io.github.johnnyscriptt.materialchecklist.client.ocr.GameFont;
import io.github.johnnyscriptt.materialchecklist.client.ocr.OcrService;
import io.github.johnnyscriptt.materialchecklist.client.store.ProjectStore;
import io.github.johnnyscriptt.materialchecklist.client.ui.ChecklistScreen;
import io.github.johnnyscriptt.materialchecklist.client.ui.ImportScreen;
import io.github.johnnyscriptt.materialchecklist.client.ui.ReviewScreen;
import io.github.johnnyscriptt.materialchecklist.core.config.ChecklistConfig;
import io.github.johnnyscriptt.materialchecklist.core.list.Project;
import io.github.johnnyscriptt.materialchecklist.core.parse.ListParser;
import io.github.johnnyscriptt.materialchecklist.net.TrackTogglePayload;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Real in-game captures for the store pages. Not a pass/fail test: it exists so every store
 * image is an honest screenshot of the mod. Shots are taken at the window's own size, where the
 * GUI is laid out, and enlarged afterwards by the upload-pack script.
 *
 * <p>If the build passes {@code -Dmaterialchecklist.storeFrame} pointing at an existing image
 * (the upload pack's sample Litematica frame), it is read through the real screenshot import
 * for the OCR shot.
 *
 * <p>Output: {@code build/run/clientGameTest/screenshots/store-*.png}.
 */
public final class StoreShotsGameTest implements FabricClientGameTest {
    private static final String COTTAGE = """
            Spruce Planks - 6 stacks
            Spruce Log x 96
            Cobblestone: 2 st
            Stone Bricks 1 sb
            Glass Pane 48
            Oak Stairs 64
            Lantern 12
            Spruce Trapdoor 24
            White Wool 32
            Flower Pot 6
            """;

    private static final List<Item> FILLER = List.of(Items.IRON_INGOT, Items.COAL, Items.WHEAT, Items.REDSTONE,
            Items.BONE, Items.STRING, Items.SAND, Items.TORCH, Items.BREAD, Items.FLINT, Items.GRAVEL, Items.DIRT);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamerule advance_time false");

            BlockPos base = world.getServer().computeOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                BlockPos origin = player.blockPosition().above(30);
                buildRoom(player.level(), origin);
                player.getInventory().add(new ItemStack(Items.SPRUCE_PLANKS, 64));
                player.getInventory().add(new ItemStack(Items.SPRUCE_PLANKS, 64));
                player.getInventory().add(new ItemStack(Items.GLASS_PANE, 20));
                player.getInventory().add(new ItemStack(Items.LANTERN, 12));
                return origin;
            });

            String projectId = context.computeOnClient(client -> {
                ProjectStore.all().forEach(old -> ProjectStore.delete(old.id()));
                ProjectStore.save(new Project("demo-tower", "Wizard Tower", List.of()));
                Project project = ProjectStore.create("Spruce Cottage");
                Project filled = project.merged(ListParser.toEntries(new ListParser(ItemIndex.matcher()).parse(COTTAGE)));
                ProjectStore.save(filled);
                return filled.id();
            });

            // Track the three shulkers on the left: the build's own stash.
            standAt(context, world, base.offset(-4, 0, 1));
            for (int i = 0; i < 3; i++) {
                BlockPos shulker = base.offset(-6, 0, 2 + i);
                int expected = i + 1;
                context.runOnClient(client -> ClientPlayNetworking.send(new TrackTogglePayload(projectId, shulker)));
                context.waitFor(client -> TrackedController.count() == expected, 100);
                context.waitTicks(5);
            }
            context.waitFor(client -> ProgressModel.have("minecraft:spruce_log") > 0, 200);

            // 1: the HUD and Find, from the middle of the room.
            // Five rows keeps the panel clear of the highlighted chests in a small window.
            context.runOnClient(client -> ConfigIo.get().hudRows = 5);
            standAt(context, world, base.offset(-1, 0, -2));
            context.getInput().lookAt(base.offset(-2, 1, 5));
            context.runOnClient(FindController::find);
            context.waitFor(client -> !FindController.state().isEmpty(), 100);
            settle(context, world);
            shot(context, "store-01-hud-and-find");
            context.runOnClient(client -> ConfigIo.get().hudRows = ChecklistConfig.DEFAULT_ROWS);

            // 2-3: pasting a list and checking it.
            String pasted = "Materials (from the video description):\n- Polished Andesite x 2 stacks\n"
                    + "- Spruce Fence 18\n- 3 shulkers of white concrete\n- Cobb1estone 1,280\n- Birch Leaves";
            context.setScreen(() -> new ImportScreen(null, pasted));
            shot(context, "store-02-paste");
            context.setScreen(() -> new ReviewScreen(new ImportScreen(null, pasted),
                    new ListParser(ItemIndex.matcher()).parse(pasted)));
            shot(context, "store-03-review");
            context.setScreen(() -> null);

            // 4: a chest with missing items glowing, and the Track button.
            BlockPos chest = base.offset(-1, 0, 5);
            standAt(context, world, chest.offset(0, 0, -2));
            context.getInput().lookAt(chest);
            context.waitTicks(5);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                player.openMenu(level.getBlockState(chest).getMenuProvider(level, chest));
            });
            context.waitForScreen(ContainerScreen.class);
            shot(context, "store-04-chest-glow");
            context.setScreen(() -> null);

            // 5: the checklist manager.
            context.setScreen(() -> new ChecklistScreen(null));
            shot(context, "store-05-checklist");
            context.setScreen(() -> null);

            // 6: a screenshot of a Litematica list, read by the real import path.
            String framePath = System.getProperty("materialchecklist.storeFrame");
            Path frame = framePath == null ? null : Path.of(framePath);
            if (frame != null && Files.exists(frame)) {
                String text = OcrService.read(frame, context.computeOnClient(GameFont::numbers)).join();
                context.setScreen(() -> new ReviewScreen(new ImportScreen(null, text),
                        new ListParser(ItemIndex.matcher()).parse(text)));
                shot(context, "store-06-from-screenshot");
                context.setScreen(() -> null);
            }
        }
    }

    private static void shot(ClientGameTestContext context, String name) {
        context.waitTicks(8);
        context.runOnClient(client -> client.gui.toastManager().clear());
        context.waitTicks(2);
        context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
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

    /**
     * A spruce storage room: a two-high wall of double chests ahead, and three shulker boxes on
     * the left holding part of the cottage's materials.
     */
    private static void buildRoom(ServerLevel level, BlockPos base) {
        for (int x = -8; x <= 8; x++) {
            for (int z = -6; z <= 8; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
                for (int y = 0; y <= 5; y++) {
                    level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
            for (int y = 0; y <= 5; y++) {
                level.setBlockAndUpdate(base.offset(x, y, 7), Blocks.SPRUCE_PLANKS.defaultBlockState());
            }
        }
        for (int z = -6; z <= 7; z++) {
            for (int y = 0; y <= 5; y++) {
                level.setBlockAndUpdate(base.offset(-8, y, z), Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
            }
        }
        for (int x = -6; x <= 6; x += 3) {
            level.setBlockAndUpdate(base.offset(x, 0, 6), Blocks.LANTERN.defaultBlockState());
        }

        BlockState north = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
        BlockState left = north.setValue(ChestBlock.TYPE, ChestType.LEFT);
        BlockState right = north.setValue(ChestBlock.TYPE, ChestType.RIGHT);
        int filler = 0;
        int chest = 0;
        for (int y = 0; y <= 1; y++) {
            for (int x = -5; x <= 3; x += 2) {
                BlockPos a = base.offset(x, y, 5);
                BlockPos b = base.offset(x + 1, y, 5);
                level.setBlockAndUpdate(a, left);
                level.setBlockAndUpdate(b, right);
                for (int slot = 0; slot < 27; slot += 3) {
                    put(level, a, slot, new ItemStack(FILLER.get(filler++ % FILLER.size()), 8 + (filler * 7) % 57));
                    put(level, b, slot + 1, new ItemStack(FILLER.get(filler++ % FILLER.size()), 8 + (filler * 7) % 57));
                }
                switch (chest) {
                    case 2 -> put(level, a, 13, new ItemStack(Items.COBBLESTONE, 64));
                    case 3 -> {
                        put(level, b, 4, new ItemStack(Items.STONE_BRICKS, 64));
                        put(level, b, 5, new ItemStack(Items.STONE_BRICKS, 64));
                    }
                    case 6 -> put(level, a, 10, new ItemStack(Items.OAK_STAIRS, 32));
                    case 8 -> put(level, b, 16, new ItemStack(Items.WOOL.white(), 20));
                    default -> {
                    }
                }
                chest++;
            }
        }
        // The chest opened for the slot-glow shot: missing items among the clutter.
        BlockPos open = base.offset(-1, 0, 5);
        put(level, open, 2, new ItemStack(Items.COBBLESTONE, 64));
        put(level, open, 11, new ItemStack(Items.SPRUCE_TRAPDOOR, 24));
        put(level, open, 20, new ItemStack(Items.FLOWER_POT, 6));

        BlockState shulker = Blocks.DYED_SHULKER_BOX.cyan().defaultBlockState().setValue(ShulkerBoxBlock.FACING, Direction.UP);
        List<List<ItemStack>> stash = List.of(
                List.of(new ItemStack(Items.SPRUCE_LOG, 64), new ItemStack(Items.SPRUCE_LOG, 32)),
                List.of(new ItemStack(Items.SPRUCE_PLANKS, 64), new ItemStack(Items.SPRUCE_PLANKS, 64),
                        new ItemStack(Items.SPRUCE_PLANKS, 64)),
                List.of(new ItemStack(Items.GLASS_PANE, 28), shulkerOf(new ItemStack(Items.OAK_STAIRS, 32))));
        for (int i = 0; i < 3; i++) {
            BlockPos pos = base.offset(-6, 0, 2 + i);
            level.setBlockAndUpdate(pos, shulker);
            for (int s = 0; s < stash.get(i).size(); s++) {
                put(level, pos, s, stash.get(i).get(s));
            }
        }
    }

    private static ItemStack shulkerOf(ItemStack inside) {
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(inside)));
        return box;
    }

    private static void put(ServerLevel level, BlockPos pos, int slot, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof Container container) {
            container.setItem(slot, stack);
        }
    }
}
