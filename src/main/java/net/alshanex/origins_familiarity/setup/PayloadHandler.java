package net.alshanex.origins_familiarity.setup;

import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.bound.BoundSpellsService;
import net.alshanex.origins_familiarity.client.ClientAccess;
import net.alshanex.origins_familiarity.network.BoundSpellsPayload;
import net.alshanex.origins_familiarity.network.RequestBoundSpellsPayload;
import net.alshanex.origins_familiarity.network.ToggleBoundSpellPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = OriginsFamiliarity.MODID)
public class PayloadHandler {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar payloadRegistrar = event.registrar(OriginsFamiliarity.MODID).versioned("1.0.0").optional();

        // Client-bound
        payloadRegistrar.playToClient(BoundSpellsPayload.TYPE, BoundSpellsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientAccess.showBoundSpells(payload)));

        // Server
        payloadRegistrar.playToServer(RequestBoundSpellsPayload.TYPE, RequestBoundSpellsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        BoundSpellsService.sendSpells(player);
                    }
                }));
        payloadRegistrar.playToServer(ToggleBoundSpellPayload.TYPE, ToggleBoundSpellPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        BoundSpellsService.toggle(player, payload.spell(), payload.enabled());
                    }
                }));
    }
}
