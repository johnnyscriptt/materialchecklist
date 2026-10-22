package io.github.johnnyscriptt.materialchecklist.net;

import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;
import io.github.johnnyscriptt.materialchecklist.core.search.SearchRules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Server to client: the containers to highlight, and which of the asked-about items turned up
 * in at least one of them (the HUD marks those rows as nearby). An empty answer is valid and
 * means nothing was found, rather than leaving old highlights up.
 */
public record FindResultPayload(List<BlockPos> positions, List<Identifier> found) implements CustomPacketPayload {

    public static final Type<FindResultPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MaterialChecklistMod.MOD_ID, "find_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FindResultPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(SearchRules.MAX_RESULTS)), FindResultPayload::positions,
                    Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(NetLimits.ITEMS)), FindResultPayload::found,
                    FindResultPayload::new);

    @Override
    public Type<FindResultPayload> type() {
        return TYPE;
    }
}
