package net.alshanex.origins_familiarity.sets;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.registry.OFDataComponents;
import net.alshanex.origins_familiarity.registry.OFItems;
import net.alshanex.origins_familiarity.util.WeaponStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A familiar set: the item only stores the set's id; the set itself is datapack data. */
public class FamiliarSetItem extends Item {

    public FamiliarSetItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(ResourceLocation setId) {
        ItemStack stack = new ItemStack(OFItems.FAMILIAR_SET.get());
        stack.set(OFDataComponents.FAMILIAR_SET.get(), setId);
        return stack;
    }

    @Nullable
    public static ResourceLocation setId(ItemStack stack) {
        return stack.get(OFDataComponents.FAMILIAR_SET.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation id = setId(stack);
        return id == null ? super.getName(stack) : Component.translatable(Util.makeDescriptionId("familiar_set", id));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        FamiliarSet set = FamiliarSets.get(context.registries(), setId(stack));
        if (set == null) {
            lines.add(Component.translatable("tooltip.origins_familiarity.set.unknown").withStyle(ChatFormatting.RED));
            return;
        }

        set.helmet().ifPresent(helmet -> lines.add(Component.translatable("tooltip.origins_familiarity.set.helmet",
                helmet.getHoverName()).withStyle(ChatFormatting.GRAY)));
        set.weapon().ifPresent(weapon -> lines.add(Component.translatable("tooltip.origins_familiarity.set.weapon",
                weapon.getHoverName()).withStyle(ChatFormatting.GRAY)));
        lines.add(Component.translatable("tooltip.origins_familiarity.set.style." + styleKey(set))
                .withStyle(ChatFormatting.DARK_AQUA));

        if (!set.attributes().isEmpty()) {
            lines.add(Component.empty());
            lines.add(Component.translatable("tooltip.origins_familiarity.set.familiar_bonuses").withStyle(ChatFormatting.GRAY));
            set.attributes().forEach(bonus -> lines.add(bonusLine(bonus)));
        }
        if (!set.ownerAttributes().isEmpty()) {
            if (!lines.isEmpty()) lines.add(Component.empty());
            lines.add(Component.translatable("tooltip.origins_familiarity.set.owner_bonuses").withStyle(ChatFormatting.GRAY));
            set.ownerAttributes().forEach(bonus -> lines.add(bonusLine(bonus)));
        }
        if (!set.spells().isEmpty()) {
            if (!lines.isEmpty()) lines.add(Component.empty());
            lines.add(Component.translatable("tooltip.origins_familiarity.set.spells").withStyle(ChatFormatting.GRAY));
            for (FamiliarSet.SpellBonus bonus : set.spells()) {
                AbstractSpell spell = bonus.resolve();
                if (spell == null) continue;
                lines.add(Component.literal(" ").append(Component.translatable("tooltip.origins_familiarity.set.spell",
                        spell.getDisplayName(null), bonus.level()).withStyle(spell.getSchoolType().getDisplayName().getStyle())));
            }
        }
    }

    /** How the set's weapon makes the familiar fight (a staff or no weapon only casts if it knows spells). */
    private static String styleKey(FamiliarSet set) {
        return switch (WeaponStyle.of(set.weapon().orElse(ItemStack.EMPTY))) {
            case RANGED -> "ranger";
            case MELEE -> "melee";
            case STAFF, UNARMED -> "caster";
        };
    }

    private static Component bonusLine(FamiliarSet.AttributeBonus bonus) {
        AttributeModifier modifier = new AttributeModifier(OriginsFamiliarity.id("tooltip"), bonus.amount(), bonus.operation());
        return Component.literal(" ").append(bonus.attribute().value().toComponent(modifier, TooltipFlag.NORMAL));
    }
}
