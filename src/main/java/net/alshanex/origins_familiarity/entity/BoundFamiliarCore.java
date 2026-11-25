package net.alshanex.origins_familiarity.entity;

import dev.overgrown.apoli.power.builtin.TogglePower;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardAttackGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.alshanex.familiarslib.util.HelperMethods;
import net.alshanex.origins_familiarity.appearance.FamiliarAppearance;
import net.alshanex.origins_familiarity.appearance.FamiliarAppearances;
import net.alshanex.origins_familiarity.bound.BoundSpellbookBonus;
import net.alshanex.origins_familiarity.origins.OriginPowerMirror;
import net.alshanex.origins_familiarity.sets.FamiliarSet;
import net.alshanex.origins_familiarity.sets.FamiliarSetItem;
import net.alshanex.origins_familiarity.sets.FamiliarSets;
import net.alshanex.origins_familiarity.sets.SetBonuses;
import net.alshanex.origins_familiarity.util.OFTags;
import net.alshanex.origins_familiarity.util.WeaponStyle;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** State and behaviour shared by both forms of the bound familiar. */
public final class BoundFamiliarCore {

    /** Body color layer: normal dyes. */
    public static final int BODY_LAYER = AbstractSpellCastingPet.DYE_LAYER_SLOT;
    /** Eyes color layer: shrunk dyes. */
    public static final int EYES_LAYER = 1;

    public static final String NBT_SPELLS = "BoundSpells";

    @Nullable private WeaponStyle lastStyle;

    public static final String NBT_SET = "BoundSet";
    private static final String SET_BONUS_PREFIX = "set_bonus";

    private ItemStack setItem = ItemStack.EMPTY;
    private final List<SetBonuses.Applied> appliedSetBonuses = new ArrayList<>();

    public static final String NBT_APPEARANCE = "BoundAppearance";

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

    /** Which attack goal the familiar uses. */
    public enum CombatStyle { MELEE, CASTER, RANGER }

    /** Marks the helmet and weapon a set puts on the familiar, so they're never mistaken for the player's own. */
    private static final String SET_PIECE_KEY = "origins_familiarity_set_piece";

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

    /** The worn set's data, or null if none is worn. */
    @Nullable
    public FamiliarSet activeSet() {
        return FamiliarSets.get(pet.level().registryAccess(), ((IBoundFamiliar) pet).equippedSetId());
    }

    public boolean hasSet() {
        return !setItem.isEmpty();
    }

    /**
     * Puts a set on: its helmet and weapon go in the head and main hand slots, and a helmet or weapon of the
     * player's own that was there goes back to the owner.
     */
    public void equipSet(ItemStack stack) {
        setItem = stack.copyWithCount(1);
        ((IBoundFamiliar) pet).setEquippedSetId(FamiliarSetItem.setId(setItem));
        FamiliarSet set = activeSet();
        if (set != null) syncSetEquipment(set);
        markGoalDirty();
    }

    /** Takes the set off (its pieces vanish) and returns its item (empty if none was worn). */
    public ItemStack removeSet() {
        ItemStack removed = setItem;
        setItem = ItemStack.EMPTY;
        ((IBoundFamiliar) pet).setEquippedSetId(null);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.MAINHAND}) {
            if (isSetPiece(pet.getItemBySlot(slot))) pet.setItemSlot(slot, ItemStack.EMPTY);
        }
        SetBonuses.apply(pet, List.of(), SET_BONUS_PREFIX, appliedSetBonuses);
        markGoalDirty();
        return removed;
    }

    /** A copy of a set's helmet or weapon, marked as a set piece. */
    public static ItemStack setPiece(ItemStack stack) {
        ItemStack piece = stack.copyWithCount(1);
        CustomData.update(DataComponents.CUSTOM_DATA, piece, tag -> tag.putBoolean(SET_PIECE_KEY, true));
        return piece;
    }

    public static boolean isSetPiece(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(SET_PIECE_KEY);
    }

    /**
     * Makes the head and main hand hold exactly the set's pieces. Run on equip and once a second, so a set
     * changed by a datapack updates, and familiars that wore a set before sets had pieces give their own
     * weapon and helmet back.
     */
    private void syncSetEquipment(FamiliarSet set) {
        syncSlot(EquipmentSlot.HEAD, set.helmet());
        syncSlot(EquipmentSlot.MAINHAND, set.weapon());
    }

    private void syncSlot(EquipmentSlot slot, Optional<ItemStack> piece) {
        ItemStack wanted = piece.map(BoundFamiliarCore::setPiece).orElse(ItemStack.EMPTY);
        ItemStack current = pet.getItemBySlot(slot);
        if (ItemStack.matches(current, wanted)) return;
        if (!current.isEmpty() && !isSetPiece(current)) {
            returnToOwner(current.copy());
        }
        pet.setItemSlot(slot, wanted);
        pet.setDropChance(slot, 0.0F);
    }

    private void returnToOwner(ItemStack stack) {
        if (pet.getSummoner() instanceof Player owner) {
            owner.getInventory().placeItemBackInInventory(stack);
        } else {
            pet.spawnAtLocation(stack);
        }
    }

    @Nullable
    public static ResourceLocation readSetId(HolderLookup.Provider registries, CompoundTag familiarTag) {
        if (!familiarTag.contains(NBT_SET, Tag.TAG_COMPOUND)) return null;
        ItemStack stack = ItemStack.parseOptional(registries, familiarTag.getCompound(NBT_SET));
        return stack.isEmpty() ? null : FamiliarSetItem.setId(stack);
    }

    /**
     * Bow or crossbow: ranger (shoots, mixing in its spells). Melee weapon: melee.
     * Staff or empty hand: caster when it has at least one spell, otherwise melee.
     */
    public CombatStyle combatStyle(SpellSet set) {
        return switch (WeaponStyle.of(pet.getMainHandItem())) {
            case RANGED -> CombatStyle.RANGER;
            case MELEE -> CombatStyle.MELEE;
            case STAFF, UNARMED -> set.isEmpty() ? CombatStyle.MELEE : CombatStyle.CASTER;
        };
    }

    /** Enabled learned spells plus the worn set's spells, sorted into the four categories by spell tags. */
    public SpellSet spellSet() {
        Map<AbstractSpell, Integer> active = new LinkedHashMap<>();
        for (LearnedSpell learned : spells) {
            if (!learned.enabled()) continue;
            AbstractSpell spell = SpellRegistry.getSpell(learned.id());
            if (spell == null || spell == SpellRegistry.none() || !spell.isEnabled()) continue;
            active.put(spell, learned.level());
        }
        FamiliarSet worn = activeSet();
        if (worn != null) {
            for (FamiliarSet.SpellBonus bonus : worn.spells()) {
                AbstractSpell spell = bonus.resolve();
                if (spell != null) active.merge(spell, bonus.level(), Math::max);
            }
        }

        List<AbstractSpell> attack = new ArrayList<>();
        List<AbstractSpell> defense = new ArrayList<>();
        List<AbstractSpell> movement = new ArrayList<>();
        List<AbstractSpell> support = new ArrayList<>();
        GapCloser gapCloser = null;

        for (Map.Entry<AbstractSpell, Integer> entry : active.entrySet()) {
            AbstractSpell spell = entry.getKey();
            if (gapCloser == null && HelperMethods.isSpellInTag(spell, OFTags.SPELL_GAP_CLOSER)) {
                gapCloser = new GapCloser(spell, entry.getValue());
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
        int level = 0;
        for (LearnedSpell learned : spells) {
            if (learned.id().equals(id)) level = learned.level();
        }
        FamiliarSet worn = activeSet();
        if (worn != null) {
            for (FamiliarSet.SpellBonus bonus : worn.spells()) {
                if (bonus.spell().equals(id)) level = Math.max(level, bonus.level());
            }
        }
        return level > 0 ? Mth.clamp(level, spell.getMinLevel(), spell.getMaxLevel()) : requestedLevel;
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

    /** Whether the current appearance is aquatic: breathes and swims underwater. */
    public boolean isAquatic() {
        FamiliarAppearance appearance = FamiliarAppearances.get(pet.level().registryAccess(), ((IBoundFamiliar) pet).appearanceId());
        return appearance != null && appearance.aquatic();
    }

    // Tick

    public void serverTick() {
        WeaponStyle style = WeaponStyle.of(pet.getMainHandItem());
        if (style != lastStyle) {
            lastStyle = style;
            markGoalDirty();
        }

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

        if (pet.tickCount % 2 == 0) {
            FamiliarAppearance appearance = FamiliarAppearances.get(pet.level().registryAccess(), ((IBoundFamiliar) pet).appearanceId());
            boolean phased = appearance != null && appearance.phasing().isPresent()
                    && summoner instanceof ServerPlayer owner
                    && TogglePower.isActive(owner, appearance.phasing().get().power());
            ((IBoundFamiliar) pet).setPhased(phased);
        }

        if (pet.tickCount % 20 == 0) {
            FamiliarSet worn = activeSet();
            SetBonuses.apply(pet, worn == null ? List.of() : worn.attributes(), SET_BONUS_PREFIX, appliedSetBonuses);
            if (worn != null && hasSet() && summoner != null) syncSetEquipment(worn);
        }

        if (pet.tickCount % 20 == 0 && summoner instanceof ServerPlayer owner && !pet.getIsInHouse()) {
            BoundSpellbookBonus.apply(pet, owner);
            ((IBoundFamiliar) pet).setAppearanceId(FamiliarAppearances.forOwner(owner));
            OriginPowerMirror.sync(pet, owner);
        }
    }

    // Save/Load

    public void save(CompoundTag tag) {
        writeSpells(tag, spells);
        if (!setItem.isEmpty()) {
            tag.put(NBT_SET, setItem.save(pet.registryAccess()));
        }
        ResourceLocation appearance = ((IBoundFamiliar) pet).appearanceId();
        if (appearance != null) {
            tag.putString(NBT_APPEARANCE, appearance.toString());
        }
    }

    public void load(CompoundTag tag) {
        spells.clear();
        spells.addAll(readSpells(tag));
        setItem = tag.contains(NBT_SET, Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(pet.registryAccess(), tag.getCompound(NBT_SET))
                : ItemStack.EMPTY;
        ((IBoundFamiliar) pet).setEquippedSetId(setItem.isEmpty() ? null : FamiliarSetItem.setId(setItem));
        markGoalDirty();
        ((IBoundFamiliar) pet).setAppearanceId(tag.contains(NBT_APPEARANCE)
                ? ResourceLocation.tryParse(tag.getString(NBT_APPEARANCE)) : null);
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
