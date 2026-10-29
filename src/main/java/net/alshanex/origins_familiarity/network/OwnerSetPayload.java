package net.alshanex.origins_familiarity.network;

import io.netty.buffer.ByteBuf;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Server -> client: the set whose owner bonuses (and spell wheel entries) the player has now, if any. */
public record OwnerSetPayload(Optional<ResourceLocation> setId) implements CustomPacketPayload {
    public static final Type<OwnerSetPayload> TYPE = new Type<>(OriginsFamiliarity.id("owner_set"));
    public static final StreamCodec<ByteBuf, OwnerSetPayload> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional).map(OwnerSetPayload::new, OwnerSetPayload::setId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
