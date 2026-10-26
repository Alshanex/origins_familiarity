package net.alshanex.origins_familiarity.bound;

import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.alshanex.origins_familiarity.registry.OFEntityRegistry;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public enum BoundForm implements StringRepresentable {
    NONE("none"),
    TERRESTRIAL("terrestrial"),
    FLYING("flying");

    private final String name;

    BoundForm(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    @Nullable
    public EntityType<? extends AbstractSpellCastingPet> entityType() {
        return switch (this) {
            case TERRESTRIAL -> OFEntityRegistry.BOUND_FAMILIAR.get();
            case FLYING -> OFEntityRegistry.BOUND_FLYING_FAMILIAR.get();
            case NONE -> null;
        };
    }

    public static BoundForm byName(String name) {
        for (BoundForm form : values()) {
            if (form.name.equals(name)) return form;
        }
        return NONE;
    }

    public static BoundForm byId(int id) {
        BoundForm[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}
