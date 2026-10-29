package net.alshanex.origins_familiarity.appearance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/** A bound familiar appearance for some origins, loaded from data/<namespace>/origins_familiarity/familiar_appearance/<id>.json. */
public record FamiliarAppearance(List<ResourceLocation> origins, Optional<ResourceLocation> model,
                                 List<String> hiddenWhenFlying, List<String> colorLayers, boolean translucent) {
    public static final String BODY = "body";
    public static final String EYES = "eyes";
    public static final List<String> DEFAULT_HIDDEN_WHEN_FLYING = List.of("left_leg", "right_leg");

    public static final Codec<FamiliarAppearance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf().fieldOf("origins").forGetter(FamiliarAppearance::origins),
            ResourceLocation.CODEC.optionalFieldOf("model").forGetter(FamiliarAppearance::model),
            Codec.STRING.listOf().optionalFieldOf("hidden_when_flying", DEFAULT_HIDDEN_WHEN_FLYING).forGetter(FamiliarAppearance::hiddenWhenFlying),
            Codec.STRING.listOf().optionalFieldOf("color_layers", List.of(BODY, EYES)).forGetter(FamiliarAppearance::colorLayers),
            Codec.BOOL.optionalFieldOf("translucent", false).forGetter(FamiliarAppearance::translucent)
    ).apply(instance, FamiliarAppearance::new));

    /** The id its files are named after: "model" if given, otherwise the appearance's own id. */
    public ResourceLocation modelId(ResourceLocation appearanceId) {
        return model.orElse(appearanceId);
    }

    public static ResourceLocation geoFile(ResourceLocation modelId) {
        return file(modelId, "geo/familiar_appearance/", ".geo.json");
    }

    public static ResourceLocation animationFile(ResourceLocation modelId) {
        return file(modelId, "animations/familiar_appearance/", ".animation.json");
    }

    public static ResourceLocation textureFile(ResourceLocation modelId) {
        return file(modelId, "textures/familiar_appearance/", ".png");
    }

    /** e.g. textures/familiar_appearance/avian_body_layer.png, or _body_layer_bw.png for the tinted one */
    public static ResourceLocation layerTexture(ResourceLocation modelId, String layer, boolean tinted) {
        return file(modelId, "textures/familiar_appearance/", "_" + layer + "_layer" + (tinted ? "_bw" : "") + ".png");
    }

    private static ResourceLocation file(ResourceLocation modelId, String folder, String suffix) {
        return ResourceLocation.fromNamespaceAndPath(modelId.getNamespace(), folder + modelId.getPath() + suffix);
    }
}
