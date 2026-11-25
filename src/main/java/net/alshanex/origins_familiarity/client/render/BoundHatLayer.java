package net.alshanex.origins_familiarity.client.render;

import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.alshanex.familiarslib.render.FamiliarHatLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;

/** Draws the familiar's head item (its own helmet, or a worn set's helmet) on the "bound_hat" bone. */
public class BoundHatLayer<T extends AbstractSpellCastingPet> extends FamiliarHatLayer<T> {
    public static final String BONE = "bound_hat";

    public BoundHatLayer(GeoRenderer<T> renderer) {
        super(renderer, BONE);
    }

    @Nullable
    @Override
    protected ItemStack getArmorItemForBone(GeoBone bone, T animatable) {
        if (!BONE.equals(bone.getName())) return null;
        ItemStack head = animatable.getItemBySlot(EquipmentSlot.HEAD);
        return head.isEmpty() ? null : head;
    }
}
