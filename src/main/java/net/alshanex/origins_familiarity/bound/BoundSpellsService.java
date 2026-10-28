package net.alshanex.origins_familiarity.bound;

import net.alshanex.origins_familiarity.entity.BoundFamiliarCore;
import net.alshanex.origins_familiarity.entity.IBoundFamiliar;
import net.alshanex.origins_familiarity.entity.LearnedSpell;
import net.alshanex.origins_familiarity.network.BoundSpellsPayload;
import net.alshanex.familiarslib.data.FamiliarRoster;
import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.alshanex.familiarslib.util.familiars.FamiliarManager;
import net.alshanex.familiarslib.util.familiars.FamiliarSync;
import net.alshanex.origins_familiarity.util.BoundFamiliarManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

/**
 * The spell toggle screen.
 */
public final class BoundSpellsService {

    public static void sendSpells(ServerPlayer player) {
        UUID id = BoundFamiliarManager.data(player).boundId();
        if (id == null) {
            player.displayClientMessage(Component.translatable("message.origins_familiarity.no_bound_familiar").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        List<BoundSpellsPayload.Entry> entries = currentSpells(player, id).stream()
                .map(spell -> new BoundSpellsPayload.Entry(spell.id(), spell.level(), spell.enabled()))
                .toList();
        PacketDistributor.sendToPlayer(player, new BoundSpellsPayload(entries));
    }

    public static void toggle(ServerPlayer player, ResourceLocation spellId, boolean enabled) {
        UUID id = BoundFamiliarManager.data(player).boundId();
        if (id == null) return;

        Entity entity = player.serverLevel().getEntity(id);
        if (entity instanceof IBoundFamiliar bound && entity instanceof AbstractSpellCastingPet pet) {
            if (bound.boundCore().setEnabled(spellId, enabled)) {
                FamiliarManager.updateFamiliarData(pet);
            }
        } else {
            FamiliarRoster roster = FamiliarRoster.of(player);
            CompoundTag tag = roster.getFamiliarData(id);
            if (tag != null) {
                List<LearnedSpell> updated = BoundFamiliarCore.setEnabled(BoundFamiliarCore.readSpells(tag), spellId, enabled);
                if (updated != null) {
                    BoundFamiliarCore.writeSpells(tag, updated);
                    roster.addTamedFamiliar(id, tag);
                    FamiliarSync.snapshot(player, id);
                }
            }
        }
        sendSpells(player);
    }

    private static List<LearnedSpell> currentSpells(ServerPlayer player, UUID id) {
        Entity entity = player.serverLevel().getEntity(id);
        if (entity instanceof IBoundFamiliar bound) {
            return bound.boundCore().spells();
        }
        CompoundTag tag = FamiliarRoster.of(player).getFamiliarData(id);
        return tag == null ? List.of() : BoundFamiliarCore.readSpells(tag);
    }
}
