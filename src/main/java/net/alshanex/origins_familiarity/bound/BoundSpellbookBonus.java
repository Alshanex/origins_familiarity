package net.alshanex.origins_familiarity.bound;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.alshanex.familiarslib.mixin.SchoolTypeAccessor;
import net.alshanex.origins_familiarity.OFConfig;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.*;

/**
 * The Familiar Spellbook's attribute sharing, as a standalone bonus for the bound familiar.
 */
public final class BoundSpellbookBonus {
    private static volatile Map<Holder<Attribute>, ResourceLocation> attributes;

    public static void apply(AbstractSpellCastingPet familiar, LivingEntity owner) {
        double fraction = OFConfig.SPELLBOOK_BONUS_FRACTION.get();

        for (Map.Entry<Holder<Attribute>, ResourceLocation> entry : attributes().entrySet()) {
            AttributeInstance ownerAttribute = owner.getAttribute(entry.getKey());
            AttributeInstance familiarAttribute = familiar.getAttribute(entry.getKey());
            if (ownerAttribute == null || familiarAttribute == null) continue;

            ResourceLocation id = entry.getValue();
            double bonus = Math.round((ownerAttribute.getValue() - ownerAttribute.getBaseValue()) * fraction * 100.0) / 100.0;
            AttributeModifier current = familiarAttribute.getModifier(id);

            if (bonus <= 0) {
                if (current != null) familiarAttribute.removeModifier(id);
                continue;
            }
            if (current != null && Math.abs(current.amount() - bonus) < 1.0E-4) continue;

            familiarAttribute.removeModifier(id);
            familiarAttribute.addTransientModifier(new AttributeModifier(id, bonus, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static Map<Holder<Attribute>, ResourceLocation> attributes() {
        if (attributes == null) {
            synchronized (BoundSpellbookBonus.class) {
                if (attributes == null) {
                    Map<Holder<Attribute>, ResourceLocation> map = new LinkedHashMap<>();
                    put(map, AttributeRegistry.SPELL_POWER);
                    put(map, AttributeRegistry.SPELL_RESIST);
                    for (SchoolType school : SchoolRegistry.REGISTRY) {
                        put(map, ((SchoolTypeAccessor) school).getPowerAttribute());
                        put(map, ((SchoolTypeAccessor) school).getResistanceAttribute());
                    }
                    attributes = Collections.unmodifiableMap(map);
                }
            }
        }
        return attributes;
    }

    private static void put(Map<Holder<Attribute>, ResourceLocation> map, Holder<Attribute> attribute) {
        if (attribute == null) return;
        attribute.unwrapKey().ifPresent(key -> map.put(attribute,
                OriginsFamiliarity.id("bound_bonus/" + key.location().getNamespace() + "/" + key.location().getPath())));
    }
}
