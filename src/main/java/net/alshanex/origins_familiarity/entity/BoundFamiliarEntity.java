package net.alshanex.origins_familiarity.entity;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardAttackGoal;
import net.alshanex.familiarslib.entity.AbstractMeleeSpellCastingPet;
import net.alshanex.familiarslib.util.familiars.FamiliarGoals;
import net.alshanex.origins_familiarity.bound.BoundStun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;

/** Terrestrial form. */
public class BoundFamiliarEntity extends AbstractMeleeSpellCastingPet implements IBoundFamiliar {

    private BoundFamiliarCore boundCore;

    public BoundFamiliarEntity(EntityType<? extends AbstractMeleeSpellCastingPet> type, Level level) {
        super(type, level);
        BoundFamiliarCore.initEquipment(this);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        initializeAttackGoal(2);
    }

    @Override
    protected WizardAttackGoal createAttackGoal(float min, float max) {
        BoundFamiliarCore core = boundCore();
        BoundFamiliarCore.SpellSet set = core.spellSet();

        FamiliarGoals.MeleeSpellCastWarlockAttackGoal<BoundFamiliarEntity> melee =
                new FamiliarGoals.MeleeSpellCastWarlockAttackGoal<>(this, 1.25f, 40, 60);
        if (set.gapCloser() != null) {
            melee.setGapCloserSpell(set.gapCloser().spell(), set.gapCloser().level());
            melee.setGapCloserDistance(4.0);
        }
        melee.setMoveset(BoundFamiliarCore.MOVESET);
        melee.setComboChance(.4f);
        melee.setMeleeAttackInverval(10, 20);
        melee.setMeleeMovespeedModifier(1.5f);
        melee.setMeleeBias(0.8f, 0.9f);
        melee.setSpells(set.attack(), set.defense(), set.movement(), set.support());
        melee.setAllowFleeing(false);
        melee.setSpellQuality(min, max);

        core.setCurrentGoal(melee);
        return melee;
    }

    @Override
    public void initiateCastSpell(AbstractSpell spell, int spellLevel) {
        int level = level().isClientSide ? spellLevel : boundCore().castLevel(spell, spellLevel);
        super.initiateCastSpell(spell, level);
    }

    @Override
    public void rebuildBoundGoal() {
        WizardAttackGoal old = boundCore().currentGoal();
        if (old != null) {
            this.goalSelector.removeGoal(old);
        }
        initializeAttackGoal(2);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        boundCore().save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        boundCore().load(tag);
    }

    @Override
    public BoundFamiliarCore boundCore() {
        if (boundCore == null) boundCore = new BoundFamiliarCore(this);
        return boundCore;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            boundCore().serverTick();
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && BoundStun.preventDeath(this)) {
            return;
        }
        super.die(source);
    }

    @Override
    protected boolean hasSitAnimation() {
        return true;
    }

    @Override
    protected Item getValidTrinket() {
        return Items.BARRIER;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canBeDyed() {
        return true;
    }

    @Override
    public int getEyeDyeLayerSlot() {
        return BoundFamiliarCore.EYES_LAYER;
    }
}
