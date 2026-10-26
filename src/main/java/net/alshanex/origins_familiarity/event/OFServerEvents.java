package net.alshanex.origins_familiarity.event;

import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.util.BoundFamiliarManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = OriginsFamiliarity.MODID)
public class OFServerEvents {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BoundFamiliarManager.tick(player);
        }
    }
}
