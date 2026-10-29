package net.alshanex.origins_familiarity.util;

import dev.overgrown.origins.origin.OriginView;
import net.alshanex.familiarslib.data.FamiliarRoster;
import net.alshanex.familiarslib.data.FamiliarSavedData;
import net.alshanex.familiarslib.entity.AbstractSpellCastingPet;
import net.alshanex.familiarslib.util.familiars.FamiliarManager;
import net.alshanex.familiarslib.util.familiars.FamiliarSync;
import net.alshanex.origins_familiarity.OFConfig;
import net.alshanex.origins_familiarity.OriginsFamiliarity;
import net.alshanex.origins_familiarity.bound.BoundForm;
import net.alshanex.origins_familiarity.bound.BoundPlayerData;
import net.alshanex.origins_familiarity.bound.RosterOrder;
import net.alshanex.origins_familiarity.origins.OriginsCompat;
import net.alshanex.origins_familiarity.registry.OFDataAttachments;
import net.alshanex.origins_familiarity.sets.SetOwnerBonuses;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

import java.util.UUID;

/**
 * Lifecycle of the bound familiar: the player picks its form on the Origins "Bound Familiar" layer -> the familiar
 * is created, then kept existing (and converted if the pick changes, e.g. with an Orb of Origin).
 * It is added without the cap check (it can never be refused) but it still counts toward every cap.
 */
public final class BoundFamiliarManager {
    public static final ResourceLocation FAMILIAR_LAYER = OriginsFamiliarity.id("bound_familiar");
    private static final ResourceLocation TERRESTRIAL_ORIGIN = OriginsFamiliarity.id("terrestrial_familiar");
    private static final ResourceLocation FLYING_ORIGIN = OriginsFamiliarity.id("flying_familiar");

    public static BoundPlayerData data(ServerPlayer player) {
        return player.getData(OFDataAttachments.BOUND_DATA.get());
    }

    /** The form picked on the Bound Familiar origin layer, or NONE if nothing is picked there yet. */
    public static BoundForm formFromOrigins(ServerPlayer player) {
        ResourceLocation origin = OriginView.chosen(player).get(FAMILIAR_LAYER);
        if (TERRESTRIAL_ORIGIN.equals(origin)) return BoundForm.TERRESTRIAL;
        if (FLYING_ORIGIN.equals(origin)) return BoundForm.FLYING;
        return BoundForm.NONE;
    }

    /** Server, every player tick. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0) return;

        SetOwnerBonuses.tick(player, data(player).boundId());

        if (!OriginsCompat.hasFinishedChoosing(player)) return;

        BoundForm form = formFromOrigins(player);
        if (form == BoundForm.NONE) return;

        BoundPlayerData data = data(player);
        data.setForm(form);
        ensureBound(player, data);
    }

    /** Makes sure the bound familiar exists, matches the picked form and is carried. */
    public static void ensureBound(ServerPlayer player, BoundPlayerData data) {
        EntityType<? extends AbstractSpellCastingPet> type = data.form().entityType();
        if (type == null) return;
        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();

        FamiliarSavedData store = FamiliarSavedData.get(player);
        UUID id = data.boundId();

        if (id != null && store.contains(id)) {
            CompoundTag tag = store.getCopy(id);
            if (tag != null && !typeId.equals(tag.getString("id"))) {
                convert(player, id, typeId);
            }
            if (RosterOrder.normalize(store)) {
                FamiliarSync.state(player);
            }
            return;
        }

        if (id != null && store.isHoused(id)) {
            store.moveToCarried(id);
            FamiliarRoster.of(player).validate();
            FamiliarSync.snapshot(player, id);
            return;
        }

        if (id != null) {
            OriginsFamiliarity.LOGGER.warn("Bound familiar {} of {} was missing from the roster, creating a new one",
                    id, player.getName().getString());
        }
        create(player, data, type);
    }

    private static void create(ServerPlayer player, BoundPlayerData data, EntityType<? extends AbstractSpellCastingPet> type) {
        AbstractSpellCastingPet familiar = type.create(player.serverLevel());
        if (familiar == null) return;

        familiar.setOwnerUUID(player.getUUID());
        familiar.setPersistenceRequired();
        familiar.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);

        UUID id = familiar.getUUID();
        CompoundTag nbt = FamiliarManager.createFamiliarNBT(familiar);
        familiar.discard(); // never added to the world, only used to build the snapshot

        FamiliarRoster roster = FamiliarRoster.of(player);
        roster.addTamedFamiliar(id, nbt); // no cap check: it can't be refused, but it counts toward the cap
        data.setBoundId(id);
        if (roster.getSelectedFamiliarId() == null) {
            roster.setSelectedFamiliarId(id);
        }
        RosterOrder.normalize(FamiliarSavedData.get(player));
        FamiliarSync.snapshot(player, id);

        player.displayClientMessage(Component.translatable("message.origins_familiarity.bound_created")
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);

        if (OFConfig.SUMMON_ON_CREATE.get()) {
            FamiliarManager.summonSpecificFamiliarAtPosition(player, id, 0);
        }
    }

    /** The pick changed (e.g. Orb of Origin): rewrite the snapshot's entity type, keeping spells, equipment and colors. */
    private static void convert(ServerPlayer player, UUID id, String typeId) {
        boolean wasOut = player.serverLevel().getEntity(id) instanceof AbstractSpellCastingPet;
        if (wasOut) {
            FamiliarManager.desummonSpecificFamiliar(player, id);
        }
        FamiliarRoster roster = FamiliarRoster.of(player);
        CompoundTag tag = roster.getFamiliarData(id);
        if (tag == null) return;
        tag.putString("id", typeId);
        roster.addTamedFamiliar(id, tag);
        FamiliarSync.snapshot(player, id);
        if (wasOut) {
            FamiliarManager.summonSpecificFamiliarAtPosition(player, id, 0);
        }
    }
}
