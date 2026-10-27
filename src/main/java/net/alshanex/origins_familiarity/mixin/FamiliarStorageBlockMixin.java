package net.alshanex.origins_familiarity.mixin;

import net.alshanex.origins_familiarity.util.BoundFamiliars;
import net.alshanex.familiarslib.block.AbstractFamiliarStorageBlock;
import net.alshanex.familiarslib.block.entity.AbstractFamiliarStorageBlockEntity;
import net.alshanex.familiarslib.data.FamiliarSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Familiar Houses don't count the bound familiar.
 */
@Mixin(value = AbstractFamiliarStorageBlock.class, remap = false)
public abstract class FamiliarStorageBlockMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void origins_familiarity$ignoreBound(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) return;
        if (!(level.getBlockEntity(pos) instanceof AbstractFamiliarStorageBlockEntity house) || !house.isOwner(serverPlayer)) return;
        if (house.getStoredFamiliarCount() > 0 || house.getOutsideFamiliarCount() > 0) return;

        boolean carriesOthers = FamiliarSavedData.get(serverPlayer).view().values().stream()
                .anyMatch(snapshot -> !BoundFamiliars.isBoundSnapshot(snapshot));
        if (!carriesOthers) {
            serverPlayer.displayClientMessage(Component.translatable("message.familiarslib.no_familiars").withStyle(ChatFormatting.YELLOW), true);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
