package net.alshanex.origins_familiarity.entity;

public interface IBoundFamiliar {
    BoundFamiliarCore boundCore();

    /** Swaps the current attack goal for one matching the held weapon and enabled spells. */
    void rebuildBoundGoal();
}
