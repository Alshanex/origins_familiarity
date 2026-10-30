package net.alshanex.origins_familiarity.entity;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardAttackGoal;
import net.alshanex.familiarslib.entity.AbstractMeleeSpellCastingPet;
import net.alshanex.familiarslib.util.familiars.FamiliarGoals;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.bound.BoundStun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.fluids.FluidType;

import javax.annotation.Nullable;
import java.util.List;

/** Terrestrial form. */
public class BoundFamiliarEntity extends AbstractMeleeSpellCastingPet implements IBoundFamiliar {

    private static final EntityDataAccessor<String> DATA_SET_ID =
            SynchedEntityData.defineId(BoundFamiliarEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_APPEARANCE_ID =
            SynchedEntityData.defineId(BoundFamiliarEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_PHASED =
            SynchedEntityData.defineId(BoundFamiliarEntity.class, EntityDataSerializers.BOOLEAN);

    private static final ResourceLocation AQUATIC_SWIM_SPEED = OriginsFamiliarity.id("aquatic_swim_speed");

    private BoundFamiliarCore boundCore;

    private boolean aquaticMode;
    @Nullable private MoveControl landMoveControl;
    @Nullable private PathNavigation landNavigation;
    @Nullable private WrappedGoal floatGoal;

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
        WizardAttackGoal goal;

        if (core.useCasterStyle(set)) {
            goal = new WizardAttackGoal(this, 1.25f, 35, 60)
                    .setSpells(set.attack(), set.defense(), set.movement(), set.support())
                    .setAllowFleeing(true)
                    .setSpellQuality(min, max);
        } else {
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
            goal = melee;
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SET_ID, "");
        builder.define(DATA_APPEARANCE_ID, "");
        builder.define(DATA_PHASED, false);
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
            if (tickCount % 20 == 0) updateAquaticMode();
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

    @Override
    public boolean shouldSheathSword() {
        return false;
    }

    @Nullable
    @Override
    public ResourceLocation equippedSetId() {
        String id = entityData.get(DATA_SET_ID);
        return id.isEmpty() ? null : ResourceLocation.tryParse(id);
    }

    @Override
    public void setEquippedSetId(@Nullable ResourceLocation setId) {
        entityData.set(DATA_SET_ID, setId == null ? "" : setId.toString());
    }

    @Nullable
    @Override
    public ResourceLocation appearanceId() {
        String id = entityData.get(DATA_APPEARANCE_ID);
        return id.isEmpty() ? null : ResourceLocation.tryParse(id);
    }

    @Override
    public void setAppearanceId(@Nullable ResourceLocation appearanceId) {
        entityData.set(DATA_APPEARANCE_ID, appearanceId == null ? "" : appearanceId.toString());
    }

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return !boundCore().isAquatic() && super.canDrownInFluidType(type);
    }

    @Override
    public boolean canTeleportIntoWater() {
        return boundCore().isAquatic();
    }

    private void updateAquaticMode() {
        if (aquaticMode) {
            setPathfindingMalus(PathType.WATER, 0.0F);
        }

        boolean aquatic = boundCore().isAquatic();
        if (aquatic == aquaticMode) return;
        aquaticMode = aquatic;
        AttributeInstance waterSpeed = getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);

        if (aquatic) {
            landMoveControl = moveControl;
            landNavigation = navigation;
            moveControl = new AquaticMoveControl(this, landMoveControl);
            navigation = new AmphibiousPathNavigation(this, level());
            setPathfindingMalus(PathType.WATER, 0.0F);
            floatGoal = goalSelector.getAvailableGoals().stream()
                    .filter(goal -> goal.getGoal() instanceof FloatGoal).findFirst().orElse(null);
            if (floatGoal != null) goalSelector.removeGoal(floatGoal.getGoal());
            if (waterSpeed != null && !waterSpeed.hasModifier(AQUATIC_SWIM_SPEED)) {
                waterSpeed.addTransientModifier(new AttributeModifier(AQUATIC_SWIM_SPEED, 1.0, AttributeModifier.Operation.ADD_VALUE));
            }
        } else {
            if (landMoveControl != null) moveControl = landMoveControl;
            if (landNavigation != null) {
                navigation.stop();
                navigation = landNavigation;
            }
            setPathfindingMalus(PathType.WATER, PathType.WATER.getMalus());
            if (floatGoal != null) goalSelector.addGoal(floatGoal.getPriority(), floatGoal.getGoal());
            floatGoal = null;
            if (waterSpeed != null) waterSpeed.removeModifier(AQUATIC_SWIM_SPEED);
        }
        boundCore().markGoalDirty();
    }

    @Override
    public boolean isPhased() {
        return entityData.get(DATA_PHASED);
    }

    @Override
    public void setPhased(boolean phased) {
        entityData.set(DATA_PHASED, phased);
    }

    @Override
    public boolean canBeHitByProjectile() {
        return !isPhased() && super.canBeHitByProjectile();
    }

    @Override
    public boolean isAttackable() {
        return !isPhased() && super.isAttackable();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (isPhased() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        return super.isInvulnerableTo(source);
    }
}
