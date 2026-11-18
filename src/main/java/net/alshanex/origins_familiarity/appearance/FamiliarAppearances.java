package net.alshanex.origins_familiarity.appearance;

import dev.overgrown.origins.origin.OriginView;
import net.alshanex.origins_familiarity.OFConfig;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/** The familiar_appearance datapack registry, and which appearance an owner's origin maps to. */
public final class FamiliarAppearances {
    public static final ResourceKey<Registry<FamiliarAppearance>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(OriginsFamiliarity.id("familiar_appearance"));

    public static final ResourceLocation MAIN_LAYER = ResourceLocation.fromNamespaceAndPath("origins", "origin");

    public static void registerRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY_KEY, FamiliarAppearance.CODEC, FamiliarAppearance.CODEC);
    }

    @Nullable
    public static FamiliarAppearance get(@Nullable HolderLookup.Provider registries, @Nullable ResourceLocation id) {
        if (registries == null || id == null) return null;
        return registries.lookup(REGISTRY_KEY)
                .flatMap(lookup -> lookup.get(ResourceKey.create(REGISTRY_KEY, id)))
                .map(Holder::value)
                .orElse(null);
    }

    @Nullable
    public static ResourceLocation forOwner(ServerPlayer owner) {
        if (!OFConfig.originAppearances()) return null;
        ResourceLocation origin = OriginView.chosen(owner).get(MAIN_LAYER);
        if (origin == null) return null;
        return owner.registryAccess().registry(REGISTRY_KEY)
                .flatMap(registry -> registry.entrySet().stream()
                        .filter(entry -> entry.getValue().origins().contains(origin))
                        .map(entry -> entry.getKey().location())
                        .min(Comparator.comparing(ResourceLocation::toString)))
                .orElse(null);
    }
}
