package net.alshanex.origins_familiarity.event;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.Scroll;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.entity.BoundFamiliarCore;
import net.alshanex.origins_familiarity.entity.IBoundFamiliar;
import net.alshanex.origins_familiarity.sets.FamiliarSet;
import net.alshanex.origins_familiarity.sets.FamiliarSetItem;
import net.alshanex.origins_familiarity.sets.FamiliarSets;
import net.alshanex.origins_familiarity.util.OFTags;
import net.alshanex.origins_familiarity.util.WeaponStyle;
import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.alshanex.familiarslib.util.HelperMethods;
import net.alshanex.familiarslib.util.familiars.FamiliarCosmetics;
import net.alshanex.familiarslib.util.familiars.FamiliarManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Owner right-clicks on the bound familiar:
 * - Spell scroll: the familiar learns the spell (unless blacklisted). Scroll is used up.
 * - Weapon (melee or staff): goes in its main hand. The previous one is given back.
 * - Hat / helmet / anything wearable on the head: goes on its head. Sneak to choose head for items that are both.
 * - Sneak + empty main hand: takes the weapon, then the hat back.
 * Shrunk cosmetics are left to familiarslib.
 */
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
        if (FamiliarCosmetics.isShrunkCosmetic(stack)) {
            return;
        }

        boolean clientSide = player.level().isClientSide;

        if (stack.getItem() instanceof FamiliarSetItem) {
            accept(event, clientSide);
            if (!clientSide) equipSet(player, familiar, bound, stack);
            return;
        }

        if (stack.getItem() instanceof Scroll) {
            accept(event, clientSide);
            if (!clientSide) learnSpell(player, familiar, bound, stack);
            return;
        }

        if (stack.isEmpty()) {
            if (event.getHand() == InteractionHand.MAIN_HAND && player.isShiftKeyDown()) {
                if (familiar.getMainHandItem().isEmpty() && bound.boundCore().hasSet()) {
                    accept(event, clientSide);
                    if (!clientSide) unequipSet(player, familiar, bound);
                    return;
                }
                EquipmentSlot slot = !familiar.getMainHandItem().isEmpty() ? EquipmentSlot.MAINHAND
                        : !familiar.getItemBySlot(EquipmentSlot.HEAD).isEmpty() ? EquipmentSlot.HEAD
                        : null;
                if (slot != null) {
                    accept(event, clientSide);
                    if (!clientSide) unequip(player, familiar, bound, slot);
                }
            }
            return;
        }

        EquipmentSlot slot = slotFor(familiar, stack, player.isShiftKeyDown());
        if (bound.boundCore().hasSet() && slotFor(familiar, stack, player.isShiftKeyDown()) == EquipmentSlot.HEAD) {
            accept(event, clientSide);
            if (!clientSide) {
                player.displayClientMessage(Component.translatable("message.origins_familiarity.set_blocks_hat")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
            return;
        }
        if (slot != null) {
            accept(event, clientSide);
            if (!clientSide) equip(player, familiar, bound, stack, slot);
        }
    }

    @Nullable
    private static EquipmentSlot slotFor(AbstractSpellCastingPet familiar, ItemStack stack, boolean sneaking) {
        boolean hat = isHat(familiar, stack);
        boolean weapon = WeaponStyle.isWeapon(stack);
        if (hat && weapon) return sneaking ? EquipmentSlot.HEAD : EquipmentSlot.MAINHAND;
        if (hat) return EquipmentSlot.HEAD;
        if (weapon) return EquipmentSlot.MAINHAND;
        return null;
    }

    private static boolean isHat(AbstractSpellCastingPet familiar, ItemStack stack) {
        return stack.is(OFTags.BOUND_HATS)
                || stack.is(FamiliarCosmetics.HATS)
                || familiar.getEquipmentSlotForItem(stack) == EquipmentSlot.HEAD;
    }

    private static void accept(PlayerInteractEvent.EntityInteract event, boolean clientSide) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(clientSide));
    }

    private static void equip(Player player, AbstractSpellCastingPet familiar, IBoundFamiliar bound, ItemStack stack, EquipmentSlot slot) {
        ItemStack previous = familiar.getItemBySlot(slot).copy();
        familiar.setItemSlot(slot, stack.copyWithCount(1));
        familiar.setDropChance(slot, 0.0F);
        stack.consume(1, player);
        if (!previous.isEmpty()) {
            player.getInventory().placeItemBackInInventory(previous);
        }
        familiar.level().playSound(null, familiar, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        bound.boundCore().markGoalDirty();
        FamiliarManager.updateFamiliarData(familiar);
    }

    private static void unequip(Player player, AbstractSpellCastingPet familiar, IBoundFamiliar bound, EquipmentSlot slot) {
        ItemStack removed = familiar.getItemBySlot(slot).copy();
        familiar.setItemSlot(slot, ItemStack.EMPTY);
        player.getInventory().placeItemBackInInventory(removed);
        familiar.level().playSound(null, familiar, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.0F);
        bound.boundCore().markGoalDirty();
        FamiliarManager.updateFamiliarData(familiar);
    }

    /** Puts the set on: the current head item and previous set go back to the player. The weapon stays. */
    private static void equipSet(Player player, AbstractSpellCastingPet familiar, IBoundFamiliar bound, ItemStack stack) {
        FamiliarSet set = FamiliarSets.get(player.level().registryAccess(), FamiliarSetItem.setId(stack));
        if (set == null) {
            player.displayClientMessage(Component.translatable("tooltip.origins_familiarity.set.unknown").withStyle(ChatFormatting.RED), true);
            return;
        }
        ItemStack head = familiar.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.isEmpty()) {
            familiar.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            player.getInventory().placeItemBackInInventory(head);
        }
        ItemStack previousSet = bound.boundCore().removeSet();
        bound.boundCore().equipSet(stack);
        stack.consume(1, player);
        if (!previousSet.isEmpty()) {
            player.getInventory().placeItemBackInInventory(previousSet);
        }
        familiar.level().playSound(null, familiar, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        FamiliarManager.updateFamiliarData(familiar);
    }

    private static void unequipSet(Player player, AbstractSpellCastingPet familiar, IBoundFamiliar bound) {
        ItemStack removed = bound.boundCore().removeSet();
        if (!removed.isEmpty()) {
            player.getInventory().placeItemBackInInventory(removed);
        }
        familiar.level().playSound(null, familiar, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.0F);
        FamiliarManager.updateFamiliarData(familiar);
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