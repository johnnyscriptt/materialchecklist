package io.github.johnnyscriptt.materialchecklist.net;

import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Client to server: which containers within {@code radius} of me hold any of these items?
 * The server re-clamps the radius; nothing here is trusted.
 */
public record FindRequestPayload(List<Identifier> items, int radius) implements CustomPacketPayload {

    public static final Type<FindRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MaterialChecklistMod.MOD_ID, "find_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FindRequestPayload> CODEC =
            StreamCodec.composite(
                    Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(NetLimits.ITEMS)), FindRequestPayload::items,
                    ByteBufCodecs.VAR_INT, FindRequestPayload::radius,
                    FindRequestPayload::new);

    @Override
    public Type<FindRequestPayload> type() {
        return TYPE;
    }
}
