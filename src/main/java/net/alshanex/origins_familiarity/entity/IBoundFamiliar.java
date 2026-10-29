package net.alshanex.origins_familiarity.entity;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public interface IBoundFamiliar {
    BoundFamiliarCore boundCore();

    /** Swaps the current attack goal for one matching the held weapon and enabled spells. */
    void rebuildBoundGoal();

    /** Id of the worn set, synced to clients for rendering. Null when no set is worn. */
    @Nullable
    ResourceLocation equippedSetId();

    void setEquippedSetId(@Nullable ResourceLocation setId);

    /** Id of the appearance picked from the owner's origin, synced to clients. Null = base look. */
    @Nullable
    ResourceLocation appearanceId();

    void setAppearanceId(@Nullable ResourceLocation appearanceId);
}
