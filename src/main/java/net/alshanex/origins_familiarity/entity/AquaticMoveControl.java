package net.alshanex.origins_familiarity.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

/** Swims while in water. */
public class AquaticMoveControl extends MoveControl {
    private final MoveControl land;

    public AquaticMoveControl(Mob mob, MoveControl land) {
        super(mob);
        this.land = land;
    }

    @Override
    public void tick() {
        if (!mob.isInWater()) {
            if (operation == Operation.MOVE_TO) {
                land.setWantedPosition(wantedX, wantedY, wantedZ, speedModifier);
            } else if (operation == Operation.STRAFE) {
                land.strafe(strafeForwards, strafeRight);
            }
            operation = Operation.WAIT;
            land.tick();
            return;
        }

        if (operation == Operation.MOVE_TO && !mob.getNavigation().isDone()) {
            double dx = wantedX - mob.getX();
            double dy = wantedY - mob.getY();
            double dz = wantedZ - mob.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < 1.0E-5) {
                mob.setSpeed(0.0F);
                return;
            }
            float yRot = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            mob.setYRot(rotlerp(mob.getYRot(), yRot, 90.0F));
            mob.yBodyRot = mob.getYRot();
            float speed = (float) (speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            mob.setSpeed(Mth.lerp(0.125F, mob.getSpeed(), speed));
            mob.setDeltaMovement(mob.getDeltaMovement().add(0.0, mob.getSpeed() * (dy / distance) * 0.1, 0.0));
        } else {
            operation = Operation.WAIT;
            mob.setSpeed(0.0F);
        }
    }
}
