package net.alshanex.origins_familiarity.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import io.redspace.ironsspellbooks.entity.mobs.HumanoidRenderer;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.render.SpellRenderingHelper;
import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.alshanex.familiarslib.render.FamiliarColorLayer;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.client.render.BoundHatLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Renderer for both forms, sharing one model and texture set. */
public class BoundFamiliarRenderer<T extends AbstractSpellCastingPet> extends HumanoidRenderer<T> {

    private static final String LAYERS = "textures/entity/layers/bond_familiar";

    public BoundFamiliarRenderer(EntityRendererProvider.Context context) {
        super(context, new BoundFamiliarModel<>());
        this.shadowRadius = 0.3f;
        addRenderLayer(new FlyingLegsLayer<>(this));

        addRenderLayer(new FamiliarColorLayer<>(this, BoundFamiliarCore.BODY_LAYER,
                OriginsFamiliarity.id(LAYERS + "_body_layer.png"),
                OriginsFamiliarity.id(LAYERS + "_body_layer_bw.png")));

        addRenderLayer(new FamiliarColorLayer<>(this, BoundFamiliarCore.EYES_LAYER,
                OriginsFamiliarity.id(LAYERS + "_eyes_layer.png"),
                OriginsFamiliarity.id(LAYERS + "_eyes_layer_bw.png")).glowing());

        addRenderLayer(new BoundHatLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return BoundFamiliarModel.TEXTURE;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        SpellRenderingHelper.renderSpellHelper(ClientMagicData.getSyncedSpellData(animatable), animatable, poseStack, bufferSource, partialTick);
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return animatable.isInvisible() ? RenderType.itemEntityTranslucentCull(texture) : RenderType.entityCutout(texture);
    }
}
