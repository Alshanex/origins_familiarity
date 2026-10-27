package net.alshanex.origins_familiarity.event;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.Scroll;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.entity.BoundFamiliarCore;
import net.alshanex.origins_familiarity.entity.IBoundFamiliar;
import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.alshanex.familiarslib.util.HelperMethods;
import net.alshanex.familiarslib.util.familiars.FamiliarManager;
import net.alshanex.origins_familiarity.util.OFTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Owner right-clicks on the bound familiar with a spell scroll: it learns the spell. */
@EventBusSubscriber(modid = OriginsFamiliarity.MODID)
public final class BoundInteractionHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractSpellCastingPet familiar) || !(familiar instanceof IBoundFamiliar bound)) {
            return;
        }
        Player player = event.getEntity();
        if (!player.getUUID().equals(familiar.getOwnerUUID())) {
            return;
        }

        ItemStack stack = event.getItemStack();
        boolean clientSide = player.level().isClientSide;

        if (stack.getItem() instanceof Scroll) {
            accept(event, clientSide);
            if (!clientSide) learnSpell(player, familiar, bound, stack);
        }
    }

    private static void accept(PlayerInteractEvent.EntityInteract event, boolean clientSide) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(clientSide));
    }

    private static void learnSpell(Player player, AbstractSpellCastingPet familiar, IBoundFamiliar bound, ItemStack scroll) {
        ISpellContainer container = ISpellContainer.get(scroll);
        SpellData data = container == null ? null : container.getSpellAtIndex(0);
        AbstractSpell spell = data == null ? null : data.getSpell();
        if (spell == null || spell == SpellRegistry.none()) {
            return;
        }

        if (!spell.isEnabled() || HelperMethods.isSpellInTag(spell, OFTags.SPELL_BLACKLIST)) {
            player.displayClientMessage(Component.translatable("message.origins_familiarity.spell_blacklisted",
                    spell.getDisplayName(player)).withStyle(ChatFormatting.RED), true);
            return;
        }

        BoundFamiliarCore.LearnResult result = bound.boundCore().learn(spell, data.getLevel());
        if (result == BoundFamiliarCore.LearnResult.ALREADY_KNOWN) {
            player.displayClientMessage(Component.translatable("message.origins_familiarity.spell_known",
                    spell.getDisplayName(player)).withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        scroll.consume(1, player);
        String key = result == BoundFamiliarCore.LearnResult.LEARNED
                ? "message.origins_familiarity.spell_learned" : "message.origins_familiarity.spell_upgraded";
        player.displayClientMessage(Component.translatable(key, spell.getDisplayName(player), data.getLevel())
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);

        familiar.level().playSound(null, familiar, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
        if (familiar.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANT, familiar.getX(), familiar.getY() + familiar.getBbHeight(), familiar.getZ(),
                    30, 0.4, 0.4, 0.4, 0.6);
        }
        FamiliarManager.updateFamiliarData(familiar);
    }
}
