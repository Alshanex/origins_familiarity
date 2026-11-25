package net.alshanex.origins_familiarity.entity.goal;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.jetbrains.annotations.Nullable;

/**
 * Draws and fires the bow or crossbow the familiar holds, shared by both ranger goals.
 * The weapon is "used" while drawing so its pull animation shows; ammo is the weapon's default ammo
 * (arrows for bows and crossbows), unlimited and not pickable.
 */
public final class RangerShooter {
    /** Give up a held shot after this many ticks without line of sight. */
    private static final int MAX_HOLD_TICKS = 40;

    private final PathfinderMob mob;
    private int drawTicks = -1;   // -1 = not drawing
    private int drawTime;

    public RangerShooter(PathfinderMob mob) {
        this.mob = mob;
    }

    public boolean isDrawing() {
        return drawTicks >= 0;
    }

    /** Starts drawing the held weapon. Does nothing if it isn't a bow or crossbow. */
    public void start() {
        ItemStack weapon = mob.getMainHandItem();
        if (!(weapon.getItem() instanceof ProjectileWeaponItem)) return;
        drawTime = weapon.getItem() instanceof CrossbowItem ? CrossbowItem.getChargeDuration(weapon, mob) : 20;
        drawTicks = 0;
        mob.startUsingItem(InteractionHand.MAIN_HAND);
    }

    /** Every goal tick: keeps aiming, fires once fully drawn and the target is in sight. */
    public void tick(@Nullable LivingEntity target, boolean hasLineOfSight) {
        if (!isDrawing()) return;
        if (target == null || !target.isAlive() || !(mob.getMainHandItem().getItem() instanceof ProjectileWeaponItem)) {
            cancel();
            return;
        }
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        drawTicks++;
        if (drawTicks < drawTime) return;
        if (hasLineOfSight) {
            fire(target);
            cancel();
        } else if (drawTicks > drawTime + MAX_HOLD_TICKS) {
            cancel();
        }
    }

    public void cancel() {
        drawTicks = -1;
        // stopUsingItem, not releaseUsingItem: release would let the bow/crossbow run its player-only logic
        if (mob.isUsingItem()) mob.stopUsingItem();
    }

    /** Same as a skeleton's shot. */
    private void fire(LivingEntity target) {
        ItemStack weapon = mob.getMainHandItem();
        if (!(weapon.getItem() instanceof ProjectileWeaponItem weaponItem)) return;
        boolean crossbow = weapon.getItem() instanceof CrossbowItem;

        ItemStack ammo = weaponItem.getDefaultCreativeAmmo(null, weapon);
        float power = crossbow ? 1.0F : BowItem.getPowerForTime(drawTicks);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(mob, ammo, power, weapon);
        arrow = weaponItem.customArrow(arrow, ammo, weapon);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;   // unlimited ammo must not be farmable

        double dx = target.getX() - mob.getX();
        double dy = target.getY(0.3333333333333333) - arrow.getY();
        double dz = target.getZ() - mob.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + horizontal * 0.2, dz, 1.6F, 14 - mob.level().getDifficulty().getId() * 4);
        mob.level().addFreshEntity(arrow);

        mob.playSound(crossbow ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.ARROW_SHOOT,
                1.0F, 1.0F / (mob.getRandom().nextFloat() * 0.4F + 0.8F));
    }
}
