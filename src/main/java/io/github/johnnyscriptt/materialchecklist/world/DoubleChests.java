package io.github.johnnyscriptt.materialchecklist.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Pairing for double chests. The server uses it to include both halves in a result, and the
 * client to merge that pair back into one box — both read the same block state, which is
 * synced, so the two sides always agree on what is joined to what.
 */
public final class DoubleChests {

    private DoubleChests() {
    }

    /** @return the other half of the double chest at {@code pos}, or null if it stands alone. */
    public static BlockPos partnerOf(BlockGetter blocks, BlockPos pos) {
        BlockState state = blocks.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)
                || !state.hasProperty(ChestBlock.TYPE)
                || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return null;
        }
        return ChestBlock.getConnectedBlockPos(pos, state);
    }
}
