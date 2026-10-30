package net.alshanex.origins_familiarity.util;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.alshanex.familiarslib.util.ModTags;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class OFTags {
    // Items
    public static final TagKey<Item> BOUND_MELEE_WEAPONS = item("bound_melee_weapons");
    public static final TagKey<Item> BOUND_STAVES = item("bound_staves");

    // Spells
    public static final TagKey<AbstractSpell> SPELL_BLACKLIST = ModTags.create(OriginsFamiliarity.id("bound/blacklist"));
    public static final TagKey<AbstractSpell> SPELL_DEFENSE = ModTags.create(OriginsFamiliarity.id("bound/defense"));
    public static final TagKey<AbstractSpell> SPELL_MOVEMENT = ModTags.create(OriginsFamiliarity.id("bound/movement"));
    public static final TagKey<AbstractSpell> SPELL_SUPPORT = ModTags.create(OriginsFamiliarity.id("bound/support"));
    public static final TagKey<AbstractSpell> SPELL_GAP_CLOSER = ModTags.create(OriginsFamiliarity.id("bound/gap_closer"));

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, OriginsFamiliarity.id(path));
    }
}
