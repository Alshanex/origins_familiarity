package net.alshanex.origins_familiarity.bound;

import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;

/**
 * Bound familiar never dies, gets stunned instead.
 */
public final class BoundStun {
    public static boolean preventDeath(AbstractSpellCastingPet familiar) {
        int duration = 5 * 20;

        familiar.setHealth(familiar.getMaxHealth());
        familiar.clearFire();
        if (familiar.isCasting()) {
            familiar.cancelCast();
        }
        familiar.setStunned(true, duration);
        return true;
    }
}
