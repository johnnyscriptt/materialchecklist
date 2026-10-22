package io.github.johnnyscriptt.materialchecklist.net;

import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: mark (or unmark) the container at {@code pos} for the list
 * {@code projectId}. The server checks that the player is within reach and that a container
 * is really there.
 */
public record TrackTogglePayload(String projectId, BlockPos pos) implements CustomPacketPayload {

    public static final Type<TrackTogglePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MaterialChecklistMod.MOD_ID, "track_toggle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrackTogglePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(NetLimits.PROJECT_ID), TrackTogglePayload::projectId,
                    BlockPos.STREAM_CODEC, TrackTogglePayload::pos,
                    TrackTogglePayload::new);

    @Override
    public Type<TrackTogglePayload> type() {
        return TYPE;
    }
}
