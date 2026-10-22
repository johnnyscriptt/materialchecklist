package io.github.johnnyscriptt.materialchecklist.net;

import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Client to server: how many of these items are in the containers marked for
 * {@code projectId}, and in the ender chest when {@code enderChest} is set? The client counts
 * its own inventory itself, live, so the server leaves that out.
 */
public record CountsRequestPayload(String projectId, List<Identifier> items, boolean enderChest) implements CustomPacketPayload {

    public static final Type<CountsRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MaterialChecklistMod.MOD_ID, "counts_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CountsRequestPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(NetLimits.PROJECT_ID), CountsRequestPayload::projectId,
                    Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(NetLimits.ITEMS)), CountsRequestPayload::items,
                    ByteBufCodecs.BOOL, CountsRequestPayload::enderChest,
                    CountsRequestPayload::new);

    @Override
    public Type<CountsRequestPayload> type() {
        return TYPE;
    }
}
