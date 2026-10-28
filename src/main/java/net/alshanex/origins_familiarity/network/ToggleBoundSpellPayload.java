package net.alshanex.origins_familiarity.network;

import io.netty.buffer.ByteBuf;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ToggleBoundSpellPayload(ResourceLocation spell, boolean enabled) implements CustomPacketPayload {
    public static final Type<ToggleBoundSpellPayload> TYPE = new Type<>(OriginsFamiliarity.id("toggle_bound_spell"));
    public static final StreamCodec<ByteBuf, ToggleBoundSpellPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ToggleBoundSpellPayload::spell,
            ByteBufCodecs.BOOL, ToggleBoundSpellPayload::enabled,
            ToggleBoundSpellPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
