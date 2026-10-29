package net.alshanex.origins_familiarity.event;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.sets.FamiliarSet;
import net.alshanex.origins_familiarity.sets.FamiliarSets;
import net.alshanex.origins_familiarity.sets.OwnerSetState;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Adds the active set's spells to the owner's spell wheel. */
@EventBusSubscriber(modid = OriginsFamiliarity.MODID)
public final class SetSpellWheelHandler {
    public static final String SLOT_ID = "origins_familiarity_set";

    @SubscribeEvent
    public static void onSpellSelection(SpellSelectionManager.SpellSelectionEvent event) {
        Player player = event.getEntity();
        FamiliarSet set = FamiliarSets.get(player.level().registryAccess(), OwnerSetState.get(player));
        if (set == null) return;

        int index = 0;
        for (FamiliarSet.SpellBonus bonus : set.spells()) {
            AbstractSpell spell = bonus.resolve();
            if (spell == null) continue;
            event.addSelectionOption(new SpellData(spell, bonus.level()), SLOT_ID, index++);
        }
    }
}
