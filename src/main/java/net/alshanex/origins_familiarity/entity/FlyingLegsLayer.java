package net.alshanex.origins_familiarity.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;

/**
 * Hides the leg bones for the flying form.
 */
public class FlyingLegsLayer<T extends AbstractSpellCastingPet> extends GeoRenderLayer<T> {
    public static final List<String> LEG_BONES = List.of("left_leg", "right_leg");

    public FlyingLegsLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType,
                          MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick,
                          int packedLight, int packedOverlay) {
        boolean hide = animatable instanceof FlyingBoundFamiliarEntity;
        for (String name : LEG_BONES) {
            bakedModel.getBone(name).ifPresent(bone -> {
                bone.setHidden(hide);
                bone.setChildrenHidden(hide);
            });
        }
    }
}
