package net.alshanex.origins_familiarity.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.alshanex.origins_familiarity.entity.AppearanceResources;
import net.alshanex.origins_familiarity.entity.IBoundFamiliar;
import net.alshanex.origins_familiarity.sets.FamiliarSet;
import net.alshanex.origins_familiarity.sets.FamiliarSets;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.ClientUtil;

import java.util.Optional;
import java.util.Set;

/**
 * Draws the worn set's model.
 */
public class FamiliarSetLayer<T extends AbstractSpellCastingPet> extends GeoRenderLayer<T> {

    /** Bones of the base bond familiar that sets attach to. If an appearance removes one, the set's pieces on it are hidden. */
    private static final Set<String> BASE_BONES = Set.of(
            "body", "left_arm", "right_arm", "left_leg", "right_leg", "bipedHandRight", "bound_hat");

    public FamiliarSetLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType,
                       MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        if (!(animatable instanceof IBoundFamiliar bound)) return;
        ResourceLocation setId = bound.equippedSetId();
        if (setId == null) return;
        FamiliarSet set = FamiliarSets.get(animatable.level().registryAccess(), setId);
        if (set == null) return;

        ResourceLocation modelId = set.modelId(setId);
        BakedGeoModel setModel = GeckoLibCache.getBakedModels().get(FamiliarSet.geoFile(modelId));
        if (setModel == null) return; // no model file for this set: nothing to draw

        for (GeoBone bone : setModel.topLevelBones()) {
            follow(bone, bakedModel);
        }

        RenderType type = layerRenderType(animatable, FamiliarSet.textureFile(modelId));
        if (type == null) return;
        // Renderer color keeps the set's alpha in step with the body (invisible familiars seen by spectators)
        int colour = getRenderer().getRenderColor(animatable, partialTick, packedLight).argbInt();
        getRenderer().reRender(setModel, poseStack, bufferSource, animatable, type,
                bufferSource.getBuffer(type), partialTick, packedLight, packedOverlay, colour);
    }

    /** Copies the pose of the same-named familiar bone onto a set bone, then does the same for its children. */
    private static void follow(GeoBone setBone, BakedGeoModel familiarModel) {
        Optional<GeoBone> match = familiarModel.getBone(setBone.getName());
        if (match.isPresent()) {
            GeoBone source = match.get();
            setBone.setRotX(source.getRotX());
            setBone.setRotY(source.getRotY());
            setBone.setRotZ(source.getRotZ());
            setBone.setPosX(source.getPosX());
            setBone.setPosY(source.getPosY());
            setBone.setPosZ(source.getPosZ());
            setBone.setScaleX(source.getScaleX());
            setBone.setScaleY(source.getScaleY());
            setBone.setScaleZ(source.getScaleZ());
            setBone.setHidden(source.isHidden());
            setBone.setChildrenHidden(source.isHidingChildren());
        } else if (BASE_BONES.contains(setBone.getName())) {
            // This appearance has no such body part (e.g. a legless slime): hide the set's pieces for it
            setBone.setHidden(true);
            setBone.setChildrenHidden(true);
            return;
        }
        for (GeoBone child : setBone.getChildBones()) {
            follow(child, familiarModel);
        }
    }

    /** Same visibility rules as the body: hidden when invisible, translucent for spectators who can see it. */
    @Nullable
    private static RenderType layerRenderType(Entity animatable, ResourceLocation texture) {
        if (AppearanceResources.needsTranslucency(animatable) && !animatable.isInvisible()) {
            return RenderType.entityTranslucent(texture);
        }
        if (!animatable.isInvisible()) {
            return RenderType.entityCutoutNoCull(texture);
        }
        if (!animatable.isInvisibleTo(ClientUtil.getClientPlayer())) {
            return RenderType.itemEntityTranslucentCull(texture);
        }
        return null;
    }
}
