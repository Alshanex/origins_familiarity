package net.alshanex.origins_familiarity.entity.goal;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardAttackGoal;

/**
 * Terrestrial ranger: keeps its distance like a caster and, on each attack, either shoots its bow/crossbow or
 * (if it knows spells) sometimes casts one instead.
 */
public class RangerAttackGoal extends WizardAttackGoal {
    /** Chance that an attack is a spell instead of a shot, when the familiar has spells. */
    public static final float SPELL_CHANCE = 0.3F;
    /** Shooting range in blocks: the goal strafes inside it and walks in when further away. */
    public static final float RANGE = 15.0F;

    private final RangerShooter shooter;

    public RangerAttackGoal(IMagicEntity mob, double speedModifier, int attackIntervalMin, int attackIntervalMax) {
        super(mob, speedModifier, attackIntervalMin, attackIntervalMax);
        this.shooter = new RangerShooter(this.mob);
        this.spellcastingRange = RANGE;
        this.spellcastingRangeSqr = RANGE * RANGE;
    }

    @Override
    public void tick() {
        super.tick();
        shooter.tick(target, hasLineOfSight);
    }

    /** Called by WizardAttackGoal each time its attack timer fires. */
    @Override
    protected void doSpellAction() {
        if (shooter.isDrawing()) return;
        boolean hasSpells = !attackSpells.isEmpty() || !defenseSpells.isEmpty()
                || !movementSpells.isEmpty() || !supportSpells.isEmpty();
        if (hasSpells && mob.getRandom().nextFloat() < SPELL_CHANCE) {
            super.doSpellAction();
        } else if (hasLineOfSight) {
            shooter.start();
        } else {
            spellAttackDelay = 5;   // no shot possible yet, try again shortly
        }
    }

    @Override
    public boolean isActing() {
        return super.isActing() || shooter.isDrawing();
    }

    @Override
    public void stop() {
        super.stop();
        shooter.cancel();
    }
}
