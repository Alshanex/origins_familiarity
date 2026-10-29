package net.alshanex.origins_familiarity.sets;

import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.entity.IBoundFamiliar;
import net.alshanex.origins_familiarity.network.OwnerSetPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Owner side of sets: attributes and spell wheel entries while the summoned bound familiar wears a set. */
@EventBusSubscriber(modid = OriginsFamiliarity.MODID)
public final class SetOwnerBonuses {
    private static final String PREFIX = "set_owner_bonus";
    private static final Map<UUID, List<SetBonuses.Applied>> APPLIED = new ConcurrentHashMap<>();

    public static void tick(ServerPlayer player, @Nullable UUID boundId) {
        ResourceLocation setId = null;
        if (boundId != null && player.serverLevel().getEntity(boundId) instanceof IBoundFamiliar bound) {
            setId = bound.equippedSetId();
        }
        FamiliarSet set = FamiliarSets.get(player.registryAccess(), setId);
        if (set == null) setId = null;

        // Re-applies by itself after a respawn, since the new player entity has none of the modifiers
        List<SetBonuses.Applied> applied = APPLIED.computeIfAbsent(player.getUUID(), id -> new ArrayList<>());
        SetBonuses.apply(player, set == null ? List.of() : set.ownerAttributes(), PREFIX, applied);

        if (!Objects.equals(OwnerSetState.get(player), setId)) {
            OwnerSetState.set(player.getUUID(), setId);
            PacketDistributor.sendToPlayer(player, new OwnerSetPayload(Optional.ofNullable(setId)));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        APPLIED.remove(id);
        OwnerSetState.set(id, null);
    }
}
