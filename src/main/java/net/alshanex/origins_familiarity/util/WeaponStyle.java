package net.alshanex.origins_familiarity.util;

import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import net.minecraft.world.item.*;

/**
 * How the bound familiar fights with what it holds.
 */
public enum WeaponStyle {
    MELEE,
    STAFF,
    RANGED,
    UNARMED;

    public static WeaponStyle of(ItemStack stack) {
        if (stack.isEmpty()) return UNARMED;
        if (isRanged(stack)) return RANGED;
        return isStaff(stack) ? STAFF : MELEE;
    }

    /** Can be handed to the bound familiar as its weapon. */
    public static boolean isWeapon(ItemStack stack) {
        return isRanged(stack) || isStaff(stack) || isMeleeWeapon(stack);
    }

    public static boolean isRanged(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ProjectileWeaponItem;
    }

    public static boolean isStaff(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(OFTags.BOUND_STAVES)) return true;
        if (stack.is(OFTags.BOUND_MELEE_WEAPONS) || stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem) {
            return false;
        }
        return stack.getItem() instanceof StaffItem;
    }

    public static boolean isMeleeWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(OFTags.BOUND_MELEE_WEAPONS)) return true;
        Item item = stack.getItem();
        return item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem || item instanceof MaceItem;
    }
}
