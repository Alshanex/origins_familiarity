package net.alshanex.origins_familiarity.entity;

import io.redspace.ironsspellbooks.entity.mobs.goals.WizardAttackGoal;
import net.alshanex.familiarslib.entity.AbstractFlyingMeleeSpellCastingPet;
import net.alshanex.familiarslib.util.familiars.FamiliarGoals;
import net.alshanex.origins_familiarity.bound.BoundStun;
import net.minecraft.core.BlockPos;
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
        skirmisher.setMoveset(BoundFamiliarCore.MOVESET);
        skirmisher.setComboChance(.4f);
        skirmisher.setMeleeAttackInverval(15, 20);
        skirmisher.setMeleeMovespeedModifier(1.5f);
        skirmisher.setSpells(List.of(), List.of(), List.of(), List.of());
        skirmisher.setSpellQuality(min, max);
        return skirmisher;
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
}
