package net.alshanex.origins_familiarity.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.client.render.FamiliarSetItemRenderer;
import net.alshanex.origins_familiarity.entity.BoundFamiliarRenderer;
import net.alshanex.origins_familiarity.network.RequestBoundSpellsPayload;
import net.alshanex.origins_familiarity.registry.OFEntityRegistry;
import net.alshanex.origins_familiarity.registry.OFItems;
import net.alshanex.origins_familiarity.sets.OwnerSetState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class OFClient {

    public static final KeyMapping OPEN_BOUND_SPELLS = new KeyMapping(
            "key.origins_familiarity.bound_spells",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.origins_familiarity");

    @EventBusSubscriber(modid = OriginsFamiliarity.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_BOUND_SPELLS);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(OFEntityRegistry.BOUND_FAMILIAR.get(), BoundFamiliarRenderer::new);
            event.registerEntityRenderer(OFEntityRegistry.BOUND_FLYING_FAMILIAR.get(), BoundFamiliarRenderer::new);
        }

        @SubscribeEvent
        public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
            event.registerItem(new IClientItemExtensions() {
                private FamiliarSetItemRenderer renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (renderer == null) renderer = new FamiliarSetItemRenderer();
                    return renderer;
                }
            }, OFItems.FAMILIAR_SET.get());
        }
    }

    @EventBusSubscriber(modid = OriginsFamiliarity.MODID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            while (OPEN_BOUND_SPELLS.consumeClick()) {
                PacketDistributor.sendToServer(RequestBoundSpellsPayload.INSTANCE);
            }
        }

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            OwnerSetState.clearAll();
        }
    }
}
