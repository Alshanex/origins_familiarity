package net.alshanex.origins_familiarity.entity;

import io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData;
import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

/** State and behaviour shared by both forms of the bound familiar. */
public final class BoundFamiliarCore {

    /** Body color layer: normal dyes. */
    public static final int BODY_LAYER = AbstractSpellCastingPet.DYE_LAYER_SLOT;
    /** Eyes color layer: shrunk dyes. */
    public static final int EYES_LAYER = 1;

    /**
     * Melee moveset.
     */
    public static final List<AttackAnimationData> MOVESET = List.of(
            new AttackAnimationData(20, "melee_attack_1", 8),
            new AttackAnimationData(20, "melee_attack_2", 10)
    );

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0)
                .add(Attributes.ATTACK_SPEED, 4.0)
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
}
