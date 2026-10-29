package net.alshanex.origins_familiarity.bound;

import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.alshanex.origins_familiarity.OFConfig;

/**
 * Bound familiar never dies, gets stunned instead.
 */
public final class BoundStun {
    public static boolean preventDeath(AbstractSpellCastingPet familiar) {
        int duration = OFConfig.STUN_SECONDS.get() * 20;

        familiar.setHealth(familiar.getMaxHealth());
        familiar.clearFire();
        if (familiar.isCasting()) {
            familiar.cancelCast();
        }
        familiar.setStunned(true, duration);
        return true;
    }
}
