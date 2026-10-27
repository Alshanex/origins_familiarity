package net.alshanex.origins_familiarity.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.alshanex.origins_familiarity.util.BoundFamiliars;
import net.alshanex.familiarslib.data.ClientFamiliarData;
import net.alshanex.familiarslib.screen.FamiliarStorageScreen;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** The house screen's "Player Familiars" column never lists the bound familiar. */
@Mixin(value = FamiliarStorageScreen.class, remap = false)
public abstract class FamiliarStorageScreenMixin {

    @WrapOperation(method = "loadFamiliarData",
            at = @At(value = "INVOKE", target = "Lnet/alshanex/familiarslib/data/ClientFamiliarData;getAllFamiliars()Ljava/util/Map;"))
    private Map<UUID, CompoundTag> origins_familiarity$hideBound(ClientFamiliarData data, Operation<Map<UUID, CompoundTag>> original) {
        Map<UUID, CompoundTag> familiars = new LinkedHashMap<>(original.call(data));
        familiars.values().removeIf(BoundFamiliars::isBoundSnapshot);
        return familiars;
    }
}
