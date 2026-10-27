package net.alshanex.origins_familiarity.mixin;

import net.alshanex.origins_familiarity.util.BoundFamiliars;
import net.alshanex.familiarslib.data.ClientFamiliarData;
import net.alshanex.familiarslib.screen.FamiliarSelectionScreen;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/** Hides the Release button while the bound familiar is selected. */
@Mixin(value = FamiliarSelectionScreen.class, remap = false)
public abstract class FamiliarSelectionScreenMixin {
    @Shadow private Button releaseButton;
    @Shadow private UUID selectedFamiliarId;

    @Inject(method = "updateReleaseButtonVisibility", at = @At("TAIL"))
    private void origins_familiarity$afterVisibilityUpdate(CallbackInfo ci) {
        origins_familiarity$hideForBound();
    }

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void origins_familiarity$afterClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        origins_familiarity$hideForBound();
    }

    @Inject(method = "onReleaseButtonPressed", at = @At("HEAD"), cancellable = true)
    private void origins_familiarity$blockRelease(Button button, CallbackInfo ci) {
        if (origins_familiarity$isBoundSelected()) {
            ci.cancel();
        }
    }

    @Unique
    private void origins_familiarity$hideForBound() {
        if (releaseButton != null && origins_familiarity$isBoundSelected()) {
            releaseButton.visible = false;
        }
    }

    @Unique
    private boolean origins_familiarity$isBoundSelected() {
        return selectedFamiliarId != null
                && BoundFamiliars.isBoundSnapshot(ClientFamiliarData.get().getFamiliarData(selectedFamiliarId));
    }
}
