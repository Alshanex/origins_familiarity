package net.alshanex.origins_familiarity.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
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

/**
 * Draws the worn set's model.
 */
public class FamiliarSetLayer<T extends AbstractSpellCastingPet> extends GeoRenderLayer<T> {

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
        familiarModel.getBone(setBone.getName()).ifPresent(source -> {
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
        });
        for (GeoBone child : setBone.getChildBones()) {
            follow(child, familiarModel);
        }
    }

    /** Same visibility rules as the body: hidden when invisible, translucent for spectators who can see it. */
    @Nullable
    private static RenderType layerRenderType(Entity animatable, ResourceLocation texture) {
        if (!animatable.isInvisible()) {
            return RenderType.entityCutoutNoCull(texture);
        }
        if (!animatable.isInvisibleTo(ClientUtil.getClientPlayer())) {
            return RenderType.itemEntityTranslucentCull(texture);
        }
        return null;
    }
}
