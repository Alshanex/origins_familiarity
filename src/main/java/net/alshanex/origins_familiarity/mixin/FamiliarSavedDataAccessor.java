package net.alshanex.origins_familiarity.mixin;

import net.alshanex.familiarslib.data.FamiliarSavedData;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.LinkedHashMap;
import java.util.UUID;

@Mixin(value = FamiliarSavedData.class, remap = false)
public interface FamiliarSavedDataAccessor {
    @Accessor("familiars")
    LinkedHashMap<UUID, CompoundTag> origins_familiarity$getFamiliars();
}
