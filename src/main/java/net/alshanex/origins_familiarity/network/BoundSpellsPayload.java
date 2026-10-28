package net.alshanex.origins_familiarity.network;

import io.netty.buffer.ByteBuf;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record BoundSpellsPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<BoundSpellsPayload> TYPE = new Type<>(OriginsFamiliarity.id("bound_spells"));

    public record Entry(ResourceLocation spell, int level, boolean enabled) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Entry::spell,
                ByteBufCodecs.VAR_INT, Entry::level,
                ByteBufCodecs.BOOL, Entry::enabled,
                Entry::new);
    }

    public static final StreamCodec<ByteBuf, BoundSpellsPayload> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(BoundSpellsPayload::new, BoundSpellsPayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
