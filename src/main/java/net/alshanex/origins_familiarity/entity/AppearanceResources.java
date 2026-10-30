package net.alshanex.origins_familiarity.entity;

import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.appearance.FamiliarAppearance;
import net.alshanex.origins_familiarity.appearance.FamiliarAppearances;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.GeckoLibCache;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AppearanceResources {
    private static final String BASE_LAYERS = "textures/entity/layers/bond_familiar";
    private static final Map<ResourceLocation, Boolean> TEXTURE_EXISTS = new ConcurrentHashMap<>();

    private record Resolved(ResourceLocation modelId, FamiliarAppearance appearance) {}

    @Nullable
    private static Resolved resolve(Entity entity) {
        if (!(entity instanceof IBoundFamiliar bound)) return null;
        ResourceLocation id = bound.appearanceId();
        FamiliarAppearance appearance = FamiliarAppearances.get(entity.level().registryAccess(), id);
        return appearance == null ? null : new Resolved(appearance.modelId(id), appearance);
    }

    public static ResourceLocation model(Entity entity) {
        Resolved resolved = resolve(entity);
        if (resolved != null) {
            ResourceLocation geo = FamiliarAppearance.geoFile(resolved.modelId());
            if (GeckoLibCache.getBakedModels().containsKey(geo)) return geo;
        }
        return BoundFamiliarModel.MODEL;
    }

    public static ResourceLocation animations(Entity entity) {
        Resolved resolved = resolve(entity);
        if (resolved != null) {
            ResourceLocation animations = FamiliarAppearance.animationFile(resolved.modelId());
            if (GeckoLibCache.getBakedAnimations().containsKey(animations)) return animations;
        }
        return BoundFamiliarModel.ANIMATIONS;
    }

    public static ResourceLocation texture(Entity entity) {
        Resolved resolved = resolve(entity);
        if (resolved != null) {
            ResourceLocation texture = FamiliarAppearance.textureFile(resolved.modelId());
            if (exists(texture)) return texture;
        }
        return BoundFamiliarModel.TEXTURE;
    }

    /** A dye layer texture, or null when this appearance has no such layer (the layer is then skipped). */
    @Nullable
    public static ResourceLocation layerTexture(Entity entity, String layer, boolean tinted) {
        Resolved resolved = resolve(entity);
        // No appearance, or an appearance that reuses the base model: use the base dye layers
        if (resolved == null || !GeckoLibCache.getBakedModels().containsKey(FamiliarAppearance.geoFile(resolved.modelId()))) {
            return OriginsFamiliarity.id(BASE_LAYERS + "_" + layer + "_layer" + (tinted ? "_bw" : "") + ".png");
        }
        if (!resolved.appearance().colorLayers().contains(layer)) return null;
        ResourceLocation texture = FamiliarAppearance.layerTexture(resolved.modelId(), layer, tinted);
        return exists(texture) ? texture : null;
    }

    public static List<String> hiddenWhenFlying(Entity entity) {
        Resolved resolved = resolve(entity);
        return resolved == null ? FamiliarAppearance.DEFAULT_HIDDEN_WHEN_FLYING : resolved.appearance().hiddenWhenFlying();
    }

    /** Cached, since this runs every frame. */
    private static boolean exists(ResourceLocation texture) {
        return TEXTURE_EXISTS.computeIfAbsent(texture,
                id -> Minecraft.getInstance().getResourceManager().getResource(id).isPresent());
    }

    /** Whether this familiar's appearance has semi-transparent pixels (needs a translucent render type). */
    public static boolean translucent(Entity entity) {
        Resolved resolved = resolve(entity);
        return resolved != null && resolved.appearance().translucent();
    }

    /** Body transparency: the phasing alpha while phased, otherwise the appearance's alpha (1 = solid). */
    public static float alpha(Entity entity) {
        Resolved resolved = resolve(entity);
        if (resolved == null) return 1.0F;
        if (entity instanceof IBoundFamiliar bound && bound.isPhased() && resolved.appearance().phasing().isPresent()) {
            return resolved.appearance().phasing().get().alpha();
        }
        return resolved.appearance().alpha();
    }

    /** Needs a translucent render type: semi-transparent texture pixels, or a whole-body alpha below 1. */
    public static boolean needsTranslucency(Entity entity) {
        return translucent(entity) || alpha(entity) < 1.0F;
    }

    public static FamiliarAppearance.ShadowMode shadow(Entity entity) {
        Resolved resolved = resolve(entity);
        return resolved == null ? FamiliarAppearance.ShadowMode.NORMAL : resolved.appearance().shadow();
    }
}
