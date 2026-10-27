package net.alshanex.origins_familiarity.mixin;

import net.alshanex.origins_familiarity.util.BoundFamiliars;
import net.alshanex.familiarslib.data.FamiliarRoster;
import net.alshanex.familiarslib.util.familiars.FamiliarManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/** The bound familiar can't be released (server-side guard; the button is also hidden). */
@Mixin(value = FamiliarManager.class, remap = false)
public abstract class FamiliarManagerMixin {

    @Inject(method = "handleReleaseFamiliar", at = @At("HEAD"), cancellable = true)
    private static void origins_familiarity$blockBoundRelease(ServerPlayer player, UUID familiarId, CallbackInfo ci) {
        if (BoundFamiliars.isBound(FamiliarRoster.of(player), familiarId)) {
            player.displayClientMessage(Component.translatable("message.origins_familiarity.cannot_release").withStyle(ChatFormatting.RED), true);
            ci.cancel();
        }
    }
}
