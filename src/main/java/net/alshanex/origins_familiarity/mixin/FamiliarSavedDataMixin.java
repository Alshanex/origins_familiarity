package net.alshanex.origins_familiarity.mixin;

import net.alshanex.origins_familiarity.bound.RosterOrder;
import net.alshanex.familiarslib.data.FamiliarSavedData;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/** Whenever something enters the carried roster, the bound familiar is moved back to the front. */
@Mixin(value = FamiliarSavedData.class, remap = false)
public abstract class FamiliarSavedDataMixin {

    @Inject(method = "put", at = @At("TAIL"))
    private void origins_familiarity$afterPut(UUID id, CompoundTag data, CallbackInfo ci) {
        RosterOrder.normalize((FamiliarSavedData) (Object) this);
    }

    @Inject(method = "putIfAbsent", at = @At("RETURN"))
    private void origins_familiarity$afterPutIfAbsent(UUID id, CompoundTag data, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) RosterOrder.normalize((FamiliarSavedData) (Object) this);
    }

    @Inject(method = "moveToCarried", at = @At("RETURN"))
    private void origins_familiarity$afterMoveToCarried(UUID id, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) RosterOrder.normalize((FamiliarSavedData) (Object) this);
    }
}
