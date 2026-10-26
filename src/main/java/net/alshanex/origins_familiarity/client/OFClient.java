package net.alshanex.origins_familiarity.client;

import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.entity.BoundFamiliarRenderer;
import net.alshanex.origins_familiarity.registry.OFEntityRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class OFClient {

    @EventBusSubscriber(modid = OriginsFamiliarity.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(OFEntityRegistry.BOUND_FAMILIAR.get(), BoundFamiliarRenderer::new);
            event.registerEntityRenderer(OFEntityRegistry.BOUND_FLYING_FAMILIAR.get(), BoundFamiliarRenderer::new);
        }
    }
}
