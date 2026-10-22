package io.github.johnnyscriptt.materialchecklist.net;

import io.github.johnnyscriptt.materialchecklist.MaterialChecklistMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: which containers are marked for {@code projectId}? */
public record TrackedQueryPayload(String projectId) implements CustomPacketPayload {

    public static final Type<TrackedQueryPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MaterialChecklistMod.MOD_ID, "tracked_query"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrackedQueryPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(NetLimits.PROJECT_ID), TrackedQueryPayload::projectId,
                    TrackedQueryPayload::new);

    @Override
    public Type<TrackedQueryPayload> type() {
        return TYPE;
    }
}
