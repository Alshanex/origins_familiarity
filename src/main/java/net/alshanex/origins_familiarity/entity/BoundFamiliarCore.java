package net.alshanex.origins_familiarity.entity;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardAttackGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.alshanex.familiarslib.util.HelperMethods;
import net.alshanex.origins_familiarity.bound.BoundSpellbookBonus;
import net.alshanex.origins_familiarity.util.OFTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** State and behaviour shared by both forms of the bound familiar. */
public final class BoundFamiliarCore {

    /** Body color layer: normal dyes. */
    public static final int BODY_LAYER = AbstractSpellCastingPet.DYE_LAYER_SLOT;
    /** Eyes color layer: shrunk dyes. */
    public static final int EYES_LAYER = 1;

    public static final String NBT_SPELLS = "BoundSpells";

    /**
     * Melee moveset.
     */
    public static final List<AttackAnimationData> MOVESET = List.of(
            new AttackAnimationData(20, "melee_attack_1", 8),
            new AttackAnimationData(20, "melee_attack_2", 10)
    );

    /** Rebuild anyway after this many ticks even if the old goal is still acting. */
    private static final int MAX_DIRTY_TICKS = 100;

    public enum LearnResult { LEARNED, UPGRADED, ALREADY_KNOWN }

    public record GapCloser(AbstractSpell spell, int level) {}

    public record SpellSet(List<AbstractSpell> attack, List<AbstractSpell> defense, List<AbstractSpell> movement,
                           List<AbstractSpell> support, @Nullable GapCloser gapCloser) {
        public boolean isEmpty() {
            return attack.isEmpty() && defense.isEmpty() && movement.isEmpty() && support.isEmpty();
        }
    }

    private final AbstractSpellCastingPet pet;
    private final List<LearnedSpell> spells = new ArrayList<>();
    @Nullable private WizardAttackGoal currentGoal;
    private boolean goalDirty;
    private int dirtyTicks;

    public BoundFamiliarCore(AbstractSpellCastingPet pet) {
        this.pet = pet;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0)
                .add(Attributes.ATTACK_SPEED, 4.0)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 2.5)
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.5);
    }

    /** Equipment never drops and the familiar never picks things up by itself. */
    public static void initEquipment(AbstractSpellCastingPet pet) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            pet.setDropChance(slot, 0.0F);
        }
        pet.setCanPickUpLoot(false);
    }

    // Goals

    public void setCurrentGoal(WizardAttackGoal goal) {
        this.currentGoal = goal;
    }

    @Nullable
    public WizardAttackGoal currentGoal() {
        return currentGoal;
    }

    public void markGoalDirty() {
        this.goalDirty = true;
    }

    /** Enabled learned spells, sorted into the four categories by spell tags. */
    public SpellSet spellSet() {
        List<AbstractSpell> attack = new ArrayList<>();
        List<AbstractSpell> defense = new ArrayList<>();
        List<AbstractSpell> movement = new ArrayList<>();
        List<AbstractSpell> support = new ArrayList<>();
        GapCloser gapCloser = null;

        for (LearnedSpell learned : spells) {
            if (!learned.enabled()) continue;
            AbstractSpell spell = SpellRegistry.getSpell(learned.id());
            if (spell == null || spell == SpellRegistry.none() || !spell.isEnabled()) continue;

            if (gapCloser == null && HelperMethods.isSpellInTag(spell, OFTags.SPELL_GAP_CLOSER)) {
                gapCloser = new GapCloser(spell, learned.level());
            }
            if (HelperMethods.isSpellInTag(spell, OFTags.SPELL_DEFENSE)) {
                defense.add(spell);
            } else if (HelperMethods.isSpellInTag(spell, OFTags.SPELL_MOVEMENT)) {
                movement.add(spell);
            } else if (HelperMethods.isSpellInTag(spell, OFTags.SPELL_SUPPORT)) {
                support.add(spell);
            } else {
                attack.add(spell);
            }
        }
        return new SpellSet(attack, defense, movement, support, gapCloser);
    }

    // Spells

    public List<LearnedSpell> spells() {
        return List.copyOf(spells);
    }

    /**
     * The level a learned spell is cast at: always the learned level (clamped to the spell's range).
     */
    public int castLevel(AbstractSpell spell, int requestedLevel) {
        ResourceLocation id = spell.getSpellResource();
        for (LearnedSpell learned : spells) {
            if (learned.id().equals(id)) {
                return Mth.clamp(learned.level(), spell.getMinLevel(), spell.getMaxLevel());
            }
        }
        return requestedLevel;
    }

    public LearnResult learn(AbstractSpell spell, int level) {
        ResourceLocation id = spell.getSpellResource();
        for (int i = 0; i < spells.size(); i++) {
            LearnedSpell known = spells.get(i);
            if (known.id().equals(id)) {
                if (level > known.level()) {
                    spells.set(i, new LearnedSpell(id, level, known.enabled()));
                    markGoalDirty();
                    return LearnResult.UPGRADED;
                }
                return LearnResult.ALREADY_KNOWN;
            }
        }
        spells.add(new LearnedSpell(id, Math.max(1, level), true));
        markGoalDirty();
        return LearnResult.LEARNED;
    }

    public boolean setEnabled(ResourceLocation id, boolean enabled) {
        List<LearnedSpell> updated = setEnabled(spells, id, enabled);
        if (updated == null) return false;
        spells.clear();
        spells.addAll(updated);
        markGoalDirty();
        return true;
    }

    /** Copy of the list with one spell toggled, or null if that spell isn't known. */
    @Nullable
    public static List<LearnedSpell> setEnabled(List<LearnedSpell> list, ResourceLocation id, boolean enabled) {
        List<LearnedSpell> result = new ArrayList<>(list);
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).id().equals(id)) {
                result.set(i, result.get(i).withEnabled(enabled));
                return result;
            }
        }
        return null;
    }

    // Tick

    public void serverTick() {
        if (goalDirty) {
            dirtyTicks++;
            if (currentGoal == null || !currentGoal.isActing() || dirtyTicks > MAX_DIRTY_TICKS) {
                goalDirty = false;
                dirtyTicks = 0;
                ((IBoundFamiliar) pet).rebuildBoundGoal();
            }
        }

        LivingEntity summoner = pet.getSummoner();

        // Never lost to the void: it can't die, so bring it back to its owner
        if (summoner != null && pet.getY() < pet.level().getMinBuildHeight() - 8) {
            pet.teleportTo(summoner.getX(), summoner.getY() + 1, summoner.getZ());
            pet.setDeltaMovement(Vec3.ZERO);
            pet.resetFallDistance();
        }

        if (pet.tickCount % 20 == 0 && summoner instanceof ServerPlayer owner && !pet.getIsInHouse()) {
            BoundSpellbookBonus.apply(pet, owner);
        }
    }

    // Save/Load

    public void save(CompoundTag tag) {
        writeSpells(tag, spells);
    }

    public void load(CompoundTag tag) {
        spells.clear();
        spells.addAll(readSpells(tag));
        markGoalDirty();
    }

    public static List<LearnedSpell> readSpells(CompoundTag familiarTag) {
        List<LearnedSpell> result = new ArrayList<>();
        if (familiarTag.contains(NBT_SPELLS, Tag.TAG_LIST)) {
            ListTag list = familiarTag.getList(NBT_SPELLS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                LearnedSpell spell = LearnedSpell.fromTag(list.getCompound(i));
                if (spell != null) result.add(spell);
            }
        }
        return result;
    }

    public static void writeSpells(CompoundTag familiarTag, List<LearnedSpell> list) {
        ListTag tags = new ListTag();
        for (LearnedSpell spell : list) {
            tags.add(spell.toTag());
        }
        familiarTag.put(NBT_SPELLS, tags);
    }
}
