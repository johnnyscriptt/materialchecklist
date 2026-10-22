package io.github.johnnyscriptt.materialchecklist.net;

import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Server to client: how many of each asked-about item sit in the project's marked containers
 * (and ender chest). Items with none are left out. {@code containers} is how many marked
 * containers still exist.
 */
public record CountsPayload(String projectId, Map<Identifier, Long> counts, int containers) implements CustomPacketPayload {

    public static final Type<CountsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MaterialChecklistMod.MOD_ID, "counts"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CountsPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(NetLimits.PROJECT_ID), CountsPayload::projectId,
                    ByteBufCodecs.<RegistryFriendlyByteBuf, Identifier, Long, Map<Identifier, Long>>map(
                            HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_LONG, NetLimits.ITEMS), CountsPayload::counts,
                    ByteBufCodecs.VAR_INT, CountsPayload::containers,
                    CountsPayload::new);

    @Override
    public Type<CountsPayload> type() {
        return TYPE;
    }
}
