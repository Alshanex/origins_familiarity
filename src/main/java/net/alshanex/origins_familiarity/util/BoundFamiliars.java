package net.alshanex.origins_familiarity.util;

import net.alshanex.origins_familiarity.entity.IBoundFamiliar;
import net.alshanex.familiarslib.data.FamiliarRoster;
import net.alshanex.familiarslib.data.FamiliarSavedData;
import net.alshanex.origins_familiarity.registry.OFEntityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Recognises the bound familiar from an entity or from a roster snapshot (its "id" entry). */
public final class BoundFamiliars {

    public static boolean isBoundTypeId(@Nullable String entityTypeId) {
        if (entityTypeId == null || entityTypeId.isEmpty()) return false;
        return entityTypeId.equals(OFEntityRegistry.BOUND_FAMILIAR.getId().toString())
                || entityTypeId.equals(OFEntityRegistry.BOUND_FLYING_FAMILIAR.getId().toString());
    }

    public static boolean isBoundSnapshot(@Nullable CompoundTag snapshot) {
        return snapshot != null && isBoundTypeId(snapshot.getString("id"));
    }

    public static boolean isBound(@Nullable Entity entity) {
        return entity instanceof IBoundFamiliar;
    }

    public static boolean isBound(FamiliarRoster roster, UUID familiarId) {
        return isBoundSnapshot(roster.getFamiliarData(familiarId));
    }

    public static boolean isBound(FamiliarSavedData store, UUID familiarId) {
        CompoundTag tag = store.getCopy(familiarId);
        if (tag == null) tag = store.getHousedData(familiarId);
        return isBoundSnapshot(tag);
    }
}
