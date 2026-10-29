package net.alshanex.origins_familiarity.sets;

import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.jetbrains.annotations.Nullable;

/** The familiar_set datapack registry. */
public final class FamiliarSets {
    public static final ResourceKey<Registry<FamiliarSet>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(OriginsFamiliarity.id("familiar_set"));

    public static void registerRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY_KEY, FamiliarSet.CODEC, FamiliarSet.CODEC);
    }

    @Nullable
    public static FamiliarSet get(@Nullable HolderLookup.Provider registries, @Nullable ResourceLocation id) {
        if (registries == null || id == null) return null;
        return registries.lookup(REGISTRY_KEY)
                .flatMap(lookup -> lookup.get(ResourceKey.create(REGISTRY_KEY, id)))
                .map(Holder::value)
                .orElse(null);
    }
}
