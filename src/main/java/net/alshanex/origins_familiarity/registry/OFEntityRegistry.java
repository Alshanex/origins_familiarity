package net.alshanex.origins_familiarity.registry;

import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.entity.BoundFamiliarCore;
import net.alshanex.origins_familiarity.entity.BoundFamiliarEntity;
import net.alshanex.origins_familiarity.entity.FlyingBoundFamiliarEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OFEntityRegistry {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, OriginsFamiliarity.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BoundFamiliarEntity>> BOUND_FAMILIAR =
            ENTITIES.register("bound_familiar", () -> EntityType.Builder.<BoundFamiliarEntity>of(BoundFamiliarEntity::new, MobCategory.MISC)
                    .sized(0.625f, 0.8125f)
                    .clientTrackingRange(64)
                    .build(OriginsFamiliarity.id("bound_familiar").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<FlyingBoundFamiliarEntity>> BOUND_FLYING_FAMILIAR =
            ENTITIES.register("bound_flying_familiar", () -> EntityType.Builder.<FlyingBoundFamiliarEntity>of(FlyingBoundFamiliarEntity::new, MobCategory.MISC)
                    .sized(0.625f, 0.8125f)
                    .clientTrackingRange(64)
                    .build(OriginsFamiliarity.id("bound_flying_familiar").toString()));

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(BOUND_FAMILIAR.get(), BoundFamiliarCore.createAttributes().build());
        event.put(BOUND_FLYING_FAMILIAR.get(), BoundFamiliarCore.createAttributes().build());
    }
}
