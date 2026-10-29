package net.alshanex.origins_familiarity.client.render;

import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.alshanex.familiarslib.render.FamiliarHatLayer;
import net.alshanex.origins_familiarity.entity.IBoundFamiliar;
import net.alshanex.origins_familiarity.sets.FamiliarSet;
import net.alshanex.origins_familiarity.sets.FamiliarSets;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;

public class BoundHatLayer<T extends AbstractSpellCastingPet> extends FamiliarHatLayer<T> {
    public static final String BONE = "bound_hat";

    public BoundHatLayer(GeoRenderer<T> renderer) {
        super(renderer, BONE);
    }

    @Nullable
    @Override
    protected ItemStack getArmorItemForBone(GeoBone bone, T animatable) {
        if (!BONE.equals(bone.getName())) return null;

        if (animatable instanceof IBoundFamiliar bound && bound.equippedSetId() != null) {
            FamiliarSet set = FamiliarSets.get(animatable.level().registryAccess(), bound.equippedSetId());
            return set == null ? null : set.helmet().orElse(null);
        }

        ItemStack head = animatable.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.isEmpty()) return head;
        return super.getArmorItemForBone(bone, animatable);
    }
}
