package net.alshanex.origins_familiarity.mixin;

import net.alshanex.origins_familiarity.util.BoundFamiliars;
import net.alshanex.familiarslib.block.entity.AbstractFamiliarStorageBlockEntity;
import net.alshanex.familiarslib.data.FamiliarRoster;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/** The bound familiar stays with its owner: it can't be put in a Familiar House. */
@Mixin(value = AbstractFamiliarStorageBlockEntity.class, remap = false)
public abstract class FamiliarStorageMixin {

    @Inject(method = "storeFamiliar", at = @At("HEAD"), cancellable = true)
    private void origins_familiarity$keepBoundWithOwner(UUID familiarId, ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (BoundFamiliars.isBound(FamiliarRoster.of(player), familiarId)) {
            player.displayClientMessage(Component.translatable("message.origins_familiarity.cannot_store").withStyle(ChatFormatting.RED), true);
            cir.setReturnValue(false);
        }
    }
}
