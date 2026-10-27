package net.alshanex.origins_familiarity.entity;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardAttackGoal;
import net.alshanex.familiarslib.entity.AbstractFlyingMeleeSpellCastingPet;
import net.alshanex.familiarslib.util.familiars.FamiliarGoals;
import net.alshanex.origins_familiarity.bound.BoundStun;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

/**
 * Flying form.
 */
public class FlyingBoundFamiliarEntity extends AbstractFlyingMeleeSpellCastingPet implements IBoundFamiliar {

    private BoundFamiliarCore boundCore;

    private static final RawAnimation IDLE_FLYING = RawAnimation.begin().thenLoop("idle_flying");
    private static final RawAnimation WALK_FLYING = RawAnimation.begin().thenLoop("walk_flying");

    public FlyingBoundFamiliarEntity(EntityType<? extends AbstractFlyingMeleeSpellCastingPet> type, Level level) {
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
        WizardAttackGoal goal;

        if (core.useCasterStyle(set)) {
            goal = new FamiliarGoals.FlyingWizardAttackGoal(this, 1f, 35, 55)
                    .setPreferredHeight(4.0f)
                    .setCombatRanges(5.0f, 7.0f, 11.0f)
                    .setThreatAwarenessRadius(10.0f)
                    .setSpells(set.attack(), set.defense(), set.movement(), set.support())
                    .setIsFlying()
                    .setAllowFleeing(true)
                    .setSpellQuality(min, max);
        } else {
            FamiliarGoals.FlyingSkirmisherAttackGoal<FlyingBoundFamiliarEntity> skirmisher =
                    new FamiliarGoals.FlyingSkirmisherAttackGoal<>(this, 1.25f, 40, 50);
            skirmisher.setPreferredDistance(6.0f)
                    .setDisengageDistance(8.0f)
                    .setEngageCooldown(30)
                    .setMaxCombosPerEngage(2)
                    .setMaxEngageTicks(60)
                    .setMaxDisengageTicks(50)
                    .setThreatAwarenessRadius(12.0f)
                    .setCrowdedThreshold(3);
            if (set.gapCloser() != null) {
                skirmisher.setGapCloserSpell(set.gapCloser().spell(), set.gapCloser().level())
                        .setGapCloserDistance(3.0);
            }
            skirmisher.setMoveset(BoundFamiliarCore.MOVESET);
            skirmisher.setComboChance(.4f);
            skirmisher.setMeleeAttackInverval(15, 20);
            skirmisher.setMeleeMovespeedModifier(1.5f);
            skirmisher.setSpells(set.attack(), set.defense(), set.movement(), set.support());
            skirmisher.setSpellQuality(min, max);
            goal = skirmisher;
        }

        core.setCurrentGoal(goal);
        return goal;
    }

    @Override
    public boolean canWearCosmeticHat() {
        return true;
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
    protected Item getValidTrinket() {
        return Items.BARRIER;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected PlayState idlePredicate(AnimationState event) {
        if (isAnimating()) {
            return PlayState.STOP;
        }
        event.getController().setAnimation(event.isMoving() ? WALK_FLYING : IDLE_FLYING);
        return PlayState.CONTINUE;
    }

    @Override
    public boolean shouldAlwaysAnimateLegs() {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean canBeDyed() {
        return true;
    }

    @Override
    public int getEyeDyeLayerSlot() {
        return BoundFamiliarCore.EYES_LAYER;
    }

    @Override
    public boolean shouldSheathSword() {
        return false;
    }
}
