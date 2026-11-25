package net.alshanex.origins_familiarity.sets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * A familiar set, loaded from data/<namespace>/origins_familiarity/familiar_set/<id>.json: a helmet and a weapon
 * worn together. While worn, they replace the familiar's own helmet and weapon and can't be swapped one by one.
 * The weapon decides how it fights (staff = caster, bow/crossbow = ranger, anything else = melee), and both items'
 * own attributes apply, on top of the set's "attributes".
 */
public record FamiliarSet(Optional<ItemStack> helmet, Optional<ItemStack> weapon,
                          List<AttributeBonus> attributes, List<AttributeBonus> ownerAttributes,
                          List<SpellBonus> spells) {

    /** "minecraft:iron_helmet" or {"id": "minecraft:leather_helmet", "components": {...}} */
    private static final Codec<ItemStack> ITEM_CODEC =
            Codec.withAlternative(ItemStack.SIMPLE_ITEM_CODEC, ItemStack.SINGLE_ITEM_CODEC);

    public static final Codec<FamiliarSet> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITEM_CODEC.optionalFieldOf("helmet").forGetter(FamiliarSet::helmet),
            ITEM_CODEC.optionalFieldOf("weapon").forGetter(FamiliarSet::weapon),
            AttributeBonus.CODEC.listOf().optionalFieldOf("attributes", List.of()).forGetter(FamiliarSet::attributes),
            AttributeBonus.CODEC.listOf().optionalFieldOf("owner_attributes", List.of()).forGetter(FamiliarSet::ownerAttributes),
            SpellBonus.CODEC.listOf().optionalFieldOf("spells", List.of()).forGetter(FamiliarSet::spells)
    ).apply(instance, FamiliarSet::new));

    public record AttributeBonus(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
        public static final Codec<AttributeBonus> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Attribute.CODEC.fieldOf("attribute").forGetter(AttributeBonus::attribute),
                Codec.DOUBLE.fieldOf("amount").forGetter(AttributeBonus::amount),
                AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE).forGetter(AttributeBonus::operation)
        ).apply(instance, AttributeBonus::new));
    }

    public record SpellBonus(ResourceLocation spell, int level) {
        public static final Codec<SpellBonus> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("spell").forGetter(SpellBonus::spell),
                Codec.intRange(1, 100).optionalFieldOf("level", 1).forGetter(SpellBonus::level)
        ).apply(instance, SpellBonus::new));

        /** The spell, or null if it doesn't exist or is disabled. */
        @Nullable
        public AbstractSpell resolve() {
            AbstractSpell spell = SpellRegistry.getSpell(this.spell);
            return spell == null || spell == SpellRegistry.none() || !spell.isEnabled() ? null : spell;
        }
    }
}
