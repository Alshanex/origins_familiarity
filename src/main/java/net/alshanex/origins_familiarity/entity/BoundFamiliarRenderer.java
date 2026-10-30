package net.alshanex.origins_familiarity.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.entity.mobs.HumanoidRenderer;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.render.SpellRenderingHelper;
import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.alshanex.familiarslib.render.FamiliarColorLayer;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.appearance.FamiliarAppearance;
import net.alshanex.origins_familiarity.client.render.BoundHatLayer;
import net.alshanex.origins_familiarity.client.render.FamiliarSetLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.util.ClientUtil;
import software.bernie.geckolib.util.Color;

/** Renderer for both forms, sharing one model and texture set. */
public class BoundFamiliarRenderer<T extends AbstractSpellCastingPet> extends HumanoidRenderer<T> {

    /** Set by FamiliarSetItemRenderer while drawing a set item: draw only the set and its helmet, not the familiar. */
    public static boolean layersOnly = false;

    /** Swallows the familiar's own geometry while layersOnly is on. */
    private static final VertexConsumer NO_OP = new VertexConsumer() {
        @Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    };

    public BoundFamiliarRenderer(EntityRendererProvider.Context context) {
        super(context, new BoundFamiliarModel<>());
        this.shadowRadius = 0.3f;
        addRenderLayer(new FlyingLegsLayer<>(this));

        addRenderLayer(bodyColorLayer(BoundFamiliarCore.BODY_LAYER, FamiliarAppearance.BODY));
        addRenderLayer(bodyColorLayer(BoundFamiliarCore.EYES_LAYER, FamiliarAppearance.EYES).glowing());

        addRenderLayer(new FamiliarSetLayer<>(this));
        addRenderLayer(new BoundHatLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return AppearanceResources.texture(entity);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        SpellRenderingHelper.renderSpellHelper(ClientMagicData.getSyncedSpellData(animatable), animatable, poseStack, bufferSource, partialTick);
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        if (AppearanceResources.needsTranslucency(animatable)) {
            return RenderType.entityTranslucent(texture);
        }
        return animatable.isInvisible() ? RenderType.itemEntityTranslucentCull(texture) : RenderType.entityCutout(texture);
    }

    private static final float HELD_ITEM_SCALE = 0.7F;
    @Override
    protected void adjustHandItemRendering(PoseStack poseStack, ItemStack stack, T animatable, float partialTick, boolean offhand) {
        poseStack.scale(HELD_ITEM_SCALE, HELD_ITEM_SCALE, HELD_ITEM_SCALE);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, T animatable, BakedGeoModel model, @Nullable RenderType renderType,
                               MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender,
                               float partialTick, int packedLight, int packedOverlay, int colour) {
        if (layersOnly && !isReRender) buffer = NO_OP;
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {
        Color base = super.getRenderColor(animatable, partialTick, packedLight);
        float alpha = AppearanceResources.alpha(animatable);
        if (alpha >= 1.0F) return base;
        return Color.ofRGBA(base.getRed(), base.getGreen(), base.getBlue(), Math.round(base.getAlpha() * alpha));
    }

    /** A dye layer whose textures follow the familiar's appearance, and that steps aside while only the layers are drawn. */
    private FamiliarColorLayer<T> bodyColorLayer(int slot, String layerName) {
        return new FamiliarColorLayer<>(this, slot, null, null) {
            @Override
            public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType,
                               MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick,
                               int packedLight, int packedOverlay) {
                if (!layersOnly) {
                    super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
                }
            }

            @Nullable
            @Override
            protected ResourceLocation getDefaultTexture(T animatable) {
                return AppearanceResources.layerTexture(animatable, layerName, false);
            }

            @Nullable
            @Override
            protected ResourceLocation getTintTexture(T animatable) {
                return AppearanceResources.layerTexture(animatable, layerName, true);
            }

            @Nullable
            @Override
            protected RenderType getLayerRenderType(T animatable, ResourceLocation texture) {
                if (AppearanceResources.needsTranslucency(animatable)) {
                    if (animatable.isInvisible() && animatable.isInvisibleTo(ClientUtil.getClientPlayer())) return null;
                    return RenderType.entityTranslucent(texture);
                }
                return super.getLayerRenderType(animatable, texture);
            }
        };
    }
}
