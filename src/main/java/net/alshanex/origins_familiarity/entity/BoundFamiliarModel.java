package net.alshanex.origins_familiarity.entity;

import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** One model for both forms (the flying form only hides the legs). */
public class BoundFamiliarModel<T extends AbstractSpellCastingPet> extends DefaultedEntityGeoModel<T> {
    public static final ResourceLocation MODEL = OriginsFamiliarity.id("geo/bond_pet.geo.json");
    public static final ResourceLocation ANIMATIONS = OriginsFamiliarity.id("animations/bond_pet.animation.json");
    public static final ResourceLocation TEXTURE = OriginsFamiliarity.id("textures/entity/bond_familiar.png");

    public BoundFamiliarModel() {
        super(OriginsFamiliarity.id("bond_pet"));
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return AppearanceResources.model(animatable);
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return AppearanceResources.animations(animatable);
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return AppearanceResources.texture(animatable);
    }
}
