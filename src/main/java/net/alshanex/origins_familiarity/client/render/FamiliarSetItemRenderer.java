package net.alshanex.origins_familiarity.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.alshanex.origins_familiarity.entity.BoundFamiliarEntity;
import net.alshanex.origins_familiarity.entity.BoundFamiliarRenderer;
import net.alshanex.origins_familiarity.registry.OFEntityRegistry;
import net.alshanex.origins_familiarity.sets.FamiliarSet;
import net.alshanex.origins_familiarity.sets.FamiliarSetItem;
import net.alshanex.origins_familiarity.sets.FamiliarSets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.GeckoLibCache;

/** Renders a familiar set item as a small bound familiar wearing the set. */
public class FamiliarSetItemRenderer extends BlockEntityWithoutLevelRenderer {
    @Nullable private BoundFamiliarEntity preview;

    public FamiliarSetItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (preview == null || preview.level() != mc.level) {
            preview = OFEntityRegistry.BOUND_FAMILIAR.get().create(mc.level);
            if (preview == null) return;
        }
        ResourceLocation setId = FamiliarSetItem.setId(stack);
        preview.setEquippedSetId(setId);

        // Only the set and its helmet, unless the set has neither (then show the whole familiar wearing nothing)
        FamiliarSet set = FamiliarSets.get(mc.level.registryAccess(), setId);
        boolean layersOnly = set != null && (set.helmet().isPresent()
                || GeckoLibCache.getBakedModels().containsKey(FamiliarSet.geoFile(set.modelId(setId))));

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        boolean gui = context == ItemDisplayContext.GUI;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        if (gui) Lighting.setupForEntityInInventory();
        dispatcher.setRenderShadow(false);
        BoundFamiliarRenderer.layersOnly = layersOnly;
        try {
            dispatcher.render(preview, 0.0, 0.0, 0.0, 0.0F, 1.0F, poseStack, bufferSource, packedLight);
        } finally {
            BoundFamiliarRenderer.layersOnly = false;
        }
        dispatcher.setRenderShadow(true);
        if (gui) Lighting.setupFor3DItems();
        poseStack.popPose();
    }
}
