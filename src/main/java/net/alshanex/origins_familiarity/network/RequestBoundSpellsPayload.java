package net.alshanex.origins_familiarity.network;

import io.netty.buffer.ByteBuf;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestBoundSpellsPayload() implements CustomPacketPayload {
    public static final RequestBoundSpellsPayload INSTANCE = new RequestBoundSpellsPayload();
    public static final Type<RequestBoundSpellsPayload> TYPE = new Type<>(OriginsFamiliarity.id("request_bound_spells"));
    public static final StreamCodec<ByteBuf, RequestBoundSpellsPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
