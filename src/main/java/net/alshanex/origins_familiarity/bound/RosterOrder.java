package net.alshanex.origins_familiarity.bound;

import net.alshanex.origins_familiarity.mixin.FamiliarSavedDataAccessor;
import net.alshanex.origins_familiarity.util.BoundFamiliars;
import net.alshanex.familiarslib.data.FamiliarSavedData;
import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps the bound familiar first in the carried roster.
 */
public final class RosterOrder {

    /** Moves the bound familiar to the front. True if the order changed. */
    public static boolean normalize(FamiliarSavedData data) {
        LinkedHashMap<UUID, CompoundTag> familiars = ((FamiliarSavedDataAccessor) data).origins_familiarity$getFamiliars();

        UUID bound = null;
        int index = 0;
        for (Map.Entry<UUID, CompoundTag> entry : familiars.entrySet()) {
            if (BoundFamiliars.isBoundSnapshot(entry.getValue())) {
                bound = entry.getKey();
                break;
            }
            index++;
        }
        if (bound == null || index == 0) {
            return false;
        }

        CompoundTag boundTag = familiars.remove(bound);
        LinkedHashMap<UUID, CompoundTag> rest = new LinkedHashMap<>(familiars);
        familiars.clear();
        familiars.put(bound, boundTag);
        familiars.putAll(rest);
        data.setDirty();
        return true;
    }
}
