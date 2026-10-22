package io.github.johnnyscriptt.materialchecklist.net;

import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Server to client: every container marked for a project, in every dimension, plus what the
 * request behind this reply did, so the client can say "now tracking" or "too far away".
 */
public record TrackedListPayload(String projectId, List<Spot> spots, int outcome) implements CustomPacketPayload {
    public static final int QUERY = 0;
    public static final int ADDED = 1;
    public static final int REMOVED = 2;
    public static final int FULL = 3;
    public static final int NOT_A_CONTAINER = 4;
    public static final int TOO_FAR = 5;

    public record Spot(String dimension, BlockPos pos) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Spot> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(NetLimits.DIMENSION), Spot::dimension,
                BlockPos.STREAM_CODEC, Spot::pos,
                Spot::new);
    }

    public static final Type<TrackedListPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MaterialChecklistMod.MOD_ID, "tracked_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrackedListPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(NetLimits.PROJECT_ID), TrackedListPayload::projectId,
                    Spot.CODEC.apply(ByteBufCodecs.list(NetLimits.ITEMS)), TrackedListPayload::spots,
                    ByteBufCodecs.VAR_INT, TrackedListPayload::outcome,
                    TrackedListPayload::new);

    @Override
    public Type<TrackedListPayload> type() {
        return TYPE;
    }
}
