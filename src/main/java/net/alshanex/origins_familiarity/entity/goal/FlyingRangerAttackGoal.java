package net.alshanex.origins_familiarity.entity.goal;

import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.alshanex.familiarslib.util.familiars.FamiliarGoals;

/**
 * Flying ranger: familiarslib's flying caster movement (orbiting, keeping away from threats), shooting its
 * bow/crossbow on each attack and sometimes casting a spell instead.
 */
public class FlyingRangerAttackGoal extends FamiliarGoals.FlyingWizardAttackGoal {
    private final RangerShooter shooter;

    public FlyingRangerAttackGoal(AbstractSpellCastingPet mob, double speedModifier, int attackIntervalMin, int attackIntervalMax) {
        super(mob, speedModifier, attackIntervalMin, attackIntervalMax);
        this.shooter = new RangerShooter(mob);
    }

    @Override
    public void tick() {
        super.tick();
        shooter.tick(target, hasLineOfSight);
    }

    @Override
    protected void doSpellAction() {
        if (shooter.isDrawing()) return;
        boolean hasSpells = !attackSpells.isEmpty() || !defenseSpells.isEmpty()
                || !movementSpells.isEmpty() || !supportSpells.isEmpty();
        if (hasSpells && mob.getRandom().nextFloat() < RangerAttackGoal.SPELL_CHANCE) {
            super.doSpellAction();
        } else if (hasLineOfSight) {
            shooter.start();
        } else {
            spellAttackDelay = 5;
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
