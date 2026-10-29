package net.alshanex.origins_familiarity.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.alshanex.origins_familiarity.client.screen.BoundSpellsScreen;
import net.alshanex.origins_familiarity.network.BoundSpellsPayload;
import net.alshanex.origins_familiarity.network.OwnerSetPayload;
import net.alshanex.origins_familiarity.sets.OwnerSetState;
import net.minecraft.client.Minecraft;

public final class ClientAccess {

    public static void showBoundSpells(BoundSpellsPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof BoundSpellsScreen screen) {
            screen.update(payload.entries());
        } else if (mc.screen == null) {
            mc.setScreen(new BoundSpellsScreen(payload.entries()));
        }
    }

    public static void onOwnerSet(OwnerSetPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        OwnerSetState.set(mc.player.getUUID(), payload.setId().orElse(null));
        ClientMagicData.updateSpellSelectionManager();
    }
}
