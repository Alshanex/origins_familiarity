package net.alshanex.origins_familiarity.sets;

import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.List;

/** Keeps an entity's set attribute modifiers in line with a list of bonuses. */
public final class SetBonuses {

    /** One modifier this mod put on an entity. */
    public record Applied(Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {}

    public static void apply(LivingEntity entity, List<FamiliarSet.AttributeBonus> bonuses, String prefix, List<Applied> applied) {
        List<Applied> wanted = new ArrayList<>();
        for (int i = 0; i < bonuses.size(); i++) {
            FamiliarSet.AttributeBonus bonus = bonuses.get(i);
            if (entity.getAttribute(bonus.attribute()) == null) continue; // the entity doesn't have this attribute
            wanted.add(new Applied(bonus.attribute(), OriginsFamiliarity.id(prefix + "/" + i), bonus.amount(), bonus.operation()));
        }

        boolean stillPresent = applied.stream().allMatch(a -> {
            AttributeInstance instance = entity.getAttribute(a.attribute());
            return instance != null && instance.hasModifier(a.id());
        });
        if (wanted.equals(applied) && stillPresent) return;

        for (Applied old : applied) {
            AttributeInstance instance = entity.getAttribute(old.attribute());
            if (instance != null) instance.removeModifier(old.id());
        }
        applied.clear();

        for (Applied bonus : wanted) {
            AttributeInstance instance = entity.getAttribute(bonus.attribute());
            if (instance == null) continue;
            instance.removeModifier(bonus.id());
            instance.addTransientModifier(new AttributeModifier(bonus.id(), bonus.amount(), bonus.operation()));
            applied.add(bonus);
        }
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }
    }
}
